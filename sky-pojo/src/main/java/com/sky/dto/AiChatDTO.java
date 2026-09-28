package com.sky.dto;

import lombok.Data;

@Data
public class AiChatDTO {
    private String message;

    //会话ID，首次对话不传(新建会话)，后续回传以延续上下文
    private String sessionId;
}