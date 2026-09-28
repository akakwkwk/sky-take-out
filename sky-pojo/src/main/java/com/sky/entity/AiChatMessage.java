package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天消息实体，会话中的一条用户消息或AI回复
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //所属会话ID
    private String sessionId;

    //消息角色: user-用户 assistant-AI回复
    private String role;

    //消息内容
    private String content;

    //创建时间
    private LocalDateTime createTime;
}
