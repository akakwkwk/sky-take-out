package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiService;
import com.sky.vo.AiChatMessageVO;
import com.sky.vo.AiChatSessionVO;
import com.sky.vo.AiChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userAiController")
@RequestMapping("/user/ai")
@Slf4j
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * AI对话（带会话记忆）：首次不传sessionId自动新建，后续回传延续上下文
     */
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@RequestBody AiChatDTO aiChatDTO) {
        log.info("用户端AI请求：{}", aiChatDTO.getMessage());

        Long userId = BaseContext.getCurrentId();
        log.info("AI对话用户ID：{}", userId != null ? userId : "未登录");

        AiChatVO aiChatVO = aiService.chatWithUser(aiChatDTO.getMessage(), userId, aiChatDTO.getSessionId());
        return Result.success(aiChatVO);
    }

    /**
     * 会话列表
     */
    @GetMapping("/sessions")
    public Result<List<AiChatSessionVO>> sessions() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(aiService.listUserSessions(userId));
    }

    /**
     * 会话聊天记录
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<AiChatMessageVO>> history(@PathVariable String sessionId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(aiService.listMessages(sessionId, userId));
    }
}
