package com.sky.service.impl;

import com.sky.config.AiFunctionConfig;
import com.sky.entity.AiChatMessage;
import com.sky.entity.AiChatSession;
import com.sky.exception.BaseException;
import com.sky.mapper.AiChatMessageMapper;
import com.sky.mapper.AiChatSessionMapper;
import com.sky.service.AiService;
import com.sky.vo.AiChatMessageVO;
import com.sky.vo.AiChatSessionVO;
import com.sky.vo.AiChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    //多轮对话最多携带的历史消息条数
    private static final int MAX_HISTORY = 20;

    //会话标题最大长度
    private static final int MAX_TITLE_LENGTH = 50;

    private static final String ROLE_USER = "user";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_ASSISTANT = "assistant";

    private final ChatClient chatClient;
    private final Function<AiFunctionConfig.OrderRequest, String> queryOrderFunction;
    private final Function<AiFunctionConfig.DishRequest, String> searchDishFunction;
    private final Function<AiFunctionConfig.UserOrdersRequest, String> queryUserOrdersFunction;
    private final Function<AiFunctionConfig.CategoryTypeRequest, String> queryCategoryListFunction;
    private final Function<AiFunctionConfig.ShopStatusRequest, String> queryShopStatusFunction;
    private final Function<AiFunctionConfig.HotDishesRequest, String> queryHotDishesFunction;

    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;
    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder,
                         @Qualifier("queryOrderFunction") Function<AiFunctionConfig.OrderRequest, String> queryOrderFunction,
                         @Qualifier("searchDishFunction") Function<AiFunctionConfig.DishRequest, String> searchDishFunction,
                         @Qualifier("queryUserOrdersFunction") Function<AiFunctionConfig.UserOrdersRequest, String> queryUserOrdersFunction,
                         @Qualifier("queryCategoryListFunction") Function<AiFunctionConfig.CategoryTypeRequest, String> queryCategoryListFunction,
                         @Qualifier("queryShopStatusFunction") Function<AiFunctionConfig.ShopStatusRequest, String> queryShopStatusFunction,
                         @Qualifier("queryHotDishesFunction") Function<AiFunctionConfig.HotDishesRequest, String> queryHotDishesFunction) {
        this.chatClient = chatClientBuilder.build();
        this.queryOrderFunction = queryOrderFunction;
        this.searchDishFunction = searchDishFunction;
        this.queryUserOrdersFunction = queryUserOrdersFunction;
        this.queryCategoryListFunction = queryCategoryListFunction;
        this.queryShopStatusFunction = queryShopStatusFunction;
        this.queryHotDishesFunction = queryHotDishesFunction;
    }

    @Override
    public AiChatVO chatWithUser(String message, Long userId, String sessionId) {
        log.info("用户AI请求: message={}, userId={}, sessionId={}", message, userId, sessionId);
        AiChatSession session = resolveSession(sessionId, userId, ROLE_USER, message);

        String systemPrompt = """
                你是"苍穹外卖"的智能店小二，名字叫"小二"。
                性格亲切幽默，喜欢用emoji表情，称呼用户为"客官"。
                回复简洁，每次不超过200字。

                【核心规则 —— 必须严格遵守】
                1. 任何关于菜品、菜单、价格、订单、营业状态的问题，必须调用对应的工具函数获取真实数据后再回答
                2. 严禁编造任何菜品名称、价格或订单信息。工具返回什么你就说什么，工具没返回的菜品一概不存在
                3. 推荐菜品时，必须调用 searchDishFunction 或 queryHotDishesFunction 获取真实在售菜品
                4. 用户询问"我的订单"、订单状态时，必须调用 queryUserOrdersFunction（无需传userId参数，系统已自动绑定当前用户）
                5. 用户提供订单号查询时，调用 queryOrderFunction
                6. 询问营业状态时，调用 queryShopStatusFunction
                7. 工具返回"没有找到"或"暂无"时，如实告诉用户，不许编造
                8. 工具调用判断标准——
                   · 用户询问具体数据（菜品、价格、分类、订单、营业状态、热销排行）：必须调用对应工具查询后作答，严禁反问"需要查什么"
                   · 寒暄、问候、闲聊（如"你好"、"你是谁"、"谢谢"）：直接自然回复即可，禁止调用任何工具
                9. 只调用与用户问题直接相关的一个工具，不要一次调用多个工具堆砌无关信息
                """;

        String response = callModel(session, message, systemPrompt, buildUserTools(userId));
        return new AiChatVO(session.getSessionId(), response);
    }

    @Override
    public AiChatVO chatWithAdmin(String message, String sessionId) {
        log.info("管理端AI请求: {}, sessionId={}", message, sessionId);
        AiChatSession session = resolveSession(sessionId, null, ROLE_ADMIN, message);

        String systemPrompt = """
                你是"苍穹外卖"的后台经营助手，名字叫"管理员助手"。
                语气专业严谨，回复简洁明了。

                【核心规则 —— 必须严格遵守】
                1. 任何关于菜品、菜单、价格、订单、营业状态、热销数据的问题，必须调用对应的工具函数获取真实数据后再回答
                2. 严禁编造任何菜品名称、价格、订单信息或统计数据。工具返回什么你就说什么，工具没返回的数据一概不存在
                3. 查询菜品时，必须调用 searchDishFunction 获取真实菜品信息
                4. 查询分类列表时，调用 queryCategoryListFunction（type=1为菜品分类，type=2为套餐分类）
                5. 查询订单详情时，用户提供订单号则调用 queryOrderFunction
                6. 询问营业状态时，调用 queryShopStatusFunction
                7. 查询热销排行时，调用 queryHotDishesFunction
                8. 工具返回"没有找到"或"暂无"时，如实告诉用户，不许编造
                9. 工具调用判断标准——
                   · 用户询问具体数据（菜品、价格、分类、订单、营业状态、热销排行）：必须调用对应工具查询后作答，严禁反问"需要查什么"
                   · 寒暄、问候、闲聊（如"你好"、"你是谁"、"谢谢"）：直接自然回复即可，禁止调用任何工具
                10. 只调用与用户问题直接相关的一个工具，不要一次调用多个工具堆砌无关信息；被用户指出行为不当时，先诚恳道歉并纠正
                """;

        String response = callModel(session, message, systemPrompt, buildAdminTools());
        return new AiChatVO(session.getSessionId(), response);
    }

    @Override
    public List<AiChatSessionVO> listUserSessions(Long userId) {
        // 未登录用户返回其匿名会话(user_id为null)
        List<AiChatSession> sessions = (userId != null)
                ? aiChatSessionMapper.listByUserId(userId)
                : aiChatSessionMapper.listAnonymous();
        return toSessionVOs(sessions);
    }

    @Override
    public List<AiChatSessionVO> listAdminSessions() {
        return toSessionVOs(aiChatSessionMapper.listAdmin());
    }

    @Override
    public List<AiChatMessageVO> listMessages(String sessionId, Long userId) {
        AiChatSession session = aiChatSessionMapper.getBySessionId(sessionId);
        if (session == null) {
            throw new BaseException("会话不存在");
        }
        // 归属校验：登录用户只能查看自己的会话，管理端(userId为null)不限制
        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new BaseException("无权查看该会话");
        }
        List<AiChatMessage> messages = aiChatMessageMapper.listBySessionId(sessionId);
        return messages.stream()
                .map(m -> AiChatMessageVO.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .createTime(m.getCreateTime())
                        .build())
                .toList();
    }

    /**
     * 解析会话：sessionId有效且归属正确则复用，否则新建会话（标题取首条消息）
     */
    private AiChatSession resolveSession(String sessionId, Long userId, String role, String firstMessage) {
        AiChatSession session = null;
        if (sessionId != null && !sessionId.isBlank()) {
            session = aiChatSessionMapper.getBySessionId(sessionId);
            if (session != null && userId != null && session.getUserId() != null
                    && !session.getUserId().equals(userId)) {
                throw new BaseException("无权访问该会话");
            }
        }
        if (session == null) {
            session = AiChatSession.builder()
                    .sessionId(UUID.randomUUID().toString().replace("-", ""))
                    .userId(userId)
                    .role(role)
                    .title(abbreviate(firstMessage))
                    .createTime(LocalDateTime.now())
                    .build();
            aiChatSessionMapper.insert(session);
            log.info("新建AI会话: sessionId={}, role={}, userId={}", session.getSessionId(), role, userId);
        }
        return session;
    }

    /**
     * 核心对话流程：保存用户消息 → 加载最近历史构建多轮上下文 → 调用模型 → 保存AI回复
     */
    private String callModel(AiChatSession session, String message, String systemPrompt, List<ToolCallback> tools) {
        // 1. 持久化用户消息
        aiChatMessageMapper.insert(AiChatMessage.builder()
                .sessionId(session.getSessionId())
                .role(ROLE_USER)
                .content(message)
                .createTime(LocalDateTime.now())
                .build());

        // 2. 加载最近MAX_HISTORY条历史（含刚保存的本条），SQL已按时间正序返回，直接转换
        List<AiChatMessage> history = aiChatMessageMapper.listLatest(session.getSessionId(), MAX_HISTORY);
        List<Message> promptMessages = history.stream()
                .<Message>map(m -> ROLE_USER.equals(m.getRole())
                        ? new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList();

        // 3. 调用模型：system + 历史消息（当前消息已在历史末尾，无需再.user()）+ 工具
        String response = chatClient.prompt()
                .system(systemPrompt)
                .messages(promptMessages)
                .toolCallbacks(tools.toArray(new ToolCallback[0]))
                .call()
                .content();

        log.info("AI Response length: {} chars", response != null ? response.length() : 0);

        // 4. 持久化AI回复
        aiChatMessageMapper.insert(AiChatMessage.builder()
                .sessionId(session.getSessionId())
                .role(ROLE_ASSISTANT)
                .content(response != null ? response : "")
                .createTime(LocalDateTime.now())
                .build());

        return response;
    }

    /**
     * 用户端工具集：queryUserOrdersFunction 闭包绑定当前登录用户
     */
    private List<ToolCallback> buildUserTools(Long userId) {
        List<ToolCallback> tools = new ArrayList<>();
        tools.add(FunctionToolCallback.builder("queryOrderFunction", queryOrderFunction)
                .description("根据订单号查询订单状态、金额和详情")
                .inputType(AiFunctionConfig.OrderRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("searchDishFunction", searchDishFunction)
                .description("根据菜品名称或关键字搜索菜品信息及价格，可用于推荐菜品")
                .inputType(AiFunctionConfig.DishRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryCategoryListFunction", queryCategoryListFunction)
                .description("查询菜品分类列表，type=1为菜品分类，type=2为套餐分类")
                .inputType(AiFunctionConfig.CategoryTypeRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryShopStatusFunction", queryShopStatusFunction)
                .description("查询店铺当前的营业状态，返回店铺是否正在营业")
                .inputType(AiFunctionConfig.ShopStatusRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryHotDishesFunction", queryHotDishesFunction)
                .description("查询当前热销菜品排行榜，用于向用户推荐热门菜品")
                .inputType(AiFunctionConfig.HotDishesRequest.class)
                .build());

        // 用户订单工具：闭包强制注入当前 userId，AI 传什么参数都忽略
        if (userId != null) {
            Function<AiFunctionConfig.UserOrdersRequest, String> boundOrderFn =
                    req -> queryUserOrdersFunction.apply(
                            new AiFunctionConfig.UserOrdersRequest(userId));
            tools.add(FunctionToolCallback.builder("queryUserOrdersFunction", boundOrderFn)
                    .description("查询当前用户的最近订单列表，无需传参，系统已自动绑定用户身份")
                    .inputType(AiFunctionConfig.UserOrdersRequest.class)
                    .build());
        } else {
            // 未登录：返回提示
            Function<AiFunctionConfig.UserOrdersRequest, String> noLoginFn =
                    req -> "请先登录后再查询订单哦~";
            tools.add(FunctionToolCallback.builder("queryUserOrdersFunction", noLoginFn)
                    .description("查询当前用户的订单列表（当前用户未登录）")
                    .inputType(AiFunctionConfig.UserOrdersRequest.class)
                    .build());
        }
        return tools;
    }

    /**
     * 管理端工具集
     */
    private List<ToolCallback> buildAdminTools() {
        List<ToolCallback> tools = new ArrayList<>();
        tools.add(FunctionToolCallback.builder("queryOrderFunction", queryOrderFunction)
                .description("根据订单号查询订单状态、金额和详情")
                .inputType(AiFunctionConfig.OrderRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("searchDishFunction", searchDishFunction)
                .description("根据菜品名称或关键字搜索菜品信息及价格")
                .inputType(AiFunctionConfig.DishRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryCategoryListFunction", queryCategoryListFunction)
                .description("查询菜品分类列表，type=1为菜品分类，type=2为套餐分类")
                .inputType(AiFunctionConfig.CategoryTypeRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryShopStatusFunction", queryShopStatusFunction)
                .description("查询店铺当前的营业状态")
                .inputType(AiFunctionConfig.ShopStatusRequest.class)
                .build());
        tools.add(FunctionToolCallback.builder("queryHotDishesFunction", queryHotDishesFunction)
                .description("查询当前热销菜品排行榜")
                .inputType(AiFunctionConfig.HotDishesRequest.class)
                .build());
        return tools;
    }

    private List<AiChatSessionVO> toSessionVOs(List<AiChatSession> sessions) {
        return sessions.stream()
                .map(s -> AiChatSessionVO.builder()
                        .sessionId(s.getSessionId())
                        .title(s.getTitle())
                        .createTime(s.getCreateTime())
                        .build())
                .toList();
    }

    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        String trimmed = message.strip();
        return trimmed.length() <= MAX_TITLE_LENGTH ? trimmed : trimmed.substring(0, MAX_TITLE_LENGTH);
    }
}
