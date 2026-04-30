package com.sky.service.impl;

import com.sky.config.AiFunctionConfig;
import com.sky.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final Function<AiFunctionConfig.OrderRequest, String> queryOrderFunction;
    private final Function<AiFunctionConfig.DishRequest, String> searchDishFunction;
    private final Function<AiFunctionConfig.UserOrdersRequest, String> queryUserOrdersFunction;
    private final Function<AiFunctionConfig.CategoryTypeRequest, String> queryCategoryListFunction;
    private final Function<AiFunctionConfig.ShopStatusRequest, String> queryShopStatusFunction;
    private final Function<AiFunctionConfig.HotDishesRequest, String> queryHotDishesFunction;

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
    public String chatWithUser(String message, Long userId) {
        log.info("用户AI请求: message={}, userId={}", message, userId);

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
                """;

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

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .toolCallbacks(tools.toArray(new ToolCallback[0]))
                .call()
                .content();

        log.info("AI Response length: {} chars", response != null ? response.length() : 0);
        return response;
    }

    @Override
    public String chatWithAdmin(String message) {
        log.info("管理端AI请求: {}", message);

        String systemPrompt = """
                你是苍穹外卖的后台经营助手，语气专业严谨。
                所有菜品、订单等数据必须通过调用工具函数获取，严禁编造任何数据。
                如果工具返回空结果，如实告知。
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();
    }
}
