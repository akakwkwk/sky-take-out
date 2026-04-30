package com.sky.controller.admin;

import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminAiController")
@RequestMapping("/admin/common/ai")
@Slf4j
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatDTO aiChatDTO) {
        log.info("管理端AI请求：{}", aiChatDTO.getMessage());
        String response = aiService.chatWithAdmin(aiChatDTO.getMessage());
        return Result.success(response);
    }
}