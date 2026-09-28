package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI会话实体，一次完整对话对应一条记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //会话唯一标识(UUID)，对外暴露的会话ID
    private String sessionId;

    //用户ID，未登录会话为null
    private Long userId;

    //会话归属端: user-用户端 admin-管理端
    private String role;

    //会话标题(取首条消息截断)
    private String title;

    //创建时间
    private LocalDateTime createTime;
}
