package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI会话列表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //会话ID
    private String sessionId;

    //会话标题
    private String title;

    //创建时间
    private LocalDateTime createTime;
}
