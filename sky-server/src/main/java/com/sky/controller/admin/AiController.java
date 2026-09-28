package com.sky.controller.admin;

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

@RestController("adminAiController")
@RequestMapping("/admin/common/ai")
@Slf4j
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * AI对话（带会话记忆）：首次不传sessionId自动新建，后续回传延续上下文
     */
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@RequestBody AiChatDTO aiChatDTO) {
        log.info("管理端AI请求：{}", aiChatDTO.getMessage());
        AiChatVO aiChatVO = aiService.chatWithAdmin(aiChatDTO.getMessage(), aiChatDTO.getSessionId());
        return Result.success(aiChatVO);
    }

    /**
     * 会话列表
     */
    @GetMapping("/sessions")
    public Result<List<AiChatSessionVO>> sessions() {
        return Result.success(aiService.listAdminSessions());
    }

    /**
     * 会话聊天记录
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<AiChatMessageVO>> history(@PathVariable String sessionId) {
        return Result.success(aiService.listMessages(sessionId, null));
    }
}
