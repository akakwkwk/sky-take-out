package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //消息角色: user-用户 assistant-AI回复
    private String role;

    //消息内容
    private String content;

    //发送时间
    private LocalDateTime createTime;
}
