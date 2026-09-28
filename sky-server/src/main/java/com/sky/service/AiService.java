package com.sky.service;

import com.sky.vo.AiChatMessageVO;
import com.sky.vo.AiChatSessionVO;
import com.sky.vo.AiChatVO;

import java.util.List;

public interface AiService {
    /**
     * 用户端：智能店小二（带会话记忆，消息持久化）
     * @param message   用户消息
     * @param userId    当前用户ID（可为null，表示未登录用户）
     * @param sessionId 会话ID，首次对话传null自动新建，后续回传以延续上下文
     * @return AI回复及会话ID
     */
    AiChatVO chatWithUser(String message, Long userId, String sessionId);

    /**
     * 管理端：经营助手（带会话记忆，消息持久化）
     * @param message   管理员消息
     * @param sessionId 会话ID，首次对话传null自动新建，后续回传以延续上下文
     * @return AI回复及会话ID
     */
    AiChatVO chatWithAdmin(String message, String sessionId);

    /**
     * 查询指定用户的用户端会话列表
     * @param userId 当前用户ID（可为null，返回未登录会话）
     */
    List<AiChatSessionVO> listUserSessions(Long userId);

    /**
     * 查询管理端会话列表
     */
    List<AiChatSessionVO> listAdminSessions();

    /**
     * 查询会话的聊天记录
     * @param sessionId 会话ID
     * @param userId    当前用户ID，用于归属校验（管理端传null）
     */
    List<AiChatMessageVO> listMessages(String sessionId, Long userId);
}
