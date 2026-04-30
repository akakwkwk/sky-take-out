package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userAiController")
@RequestMapping("/user/ai")
@Slf4j
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatDTO aiChatDTO) {
        log.info("用户端AI请求：{}", aiChatDTO.getMessage());

        Long userId = BaseContext.getCurrentId();
        log.info("AI对话用户ID：{}", userId != null ? userId : "未登录");

        String response = aiService.chatWithUser(aiChatDTO.getMessage(), userId);
        return Result.success(response);
    }
}
