package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI对话结果VO：返回AI回复及会话ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //会话ID，前端需保存并在后续请求中回传，以延续同一对话
    private String sessionId;

    //AI回复内容
    private String reply;
}
