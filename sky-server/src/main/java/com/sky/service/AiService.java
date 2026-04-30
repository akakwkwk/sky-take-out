package com.sky.service;

public interface AiService {
    /**
     * 用户端：智能店小二
     * @param message 用户消息
     * @param userId  当前用户ID（可为null，表示未登录用户）
     */
    String chatWithUser(String message, Long userId);

    /**
     * 管理端：经营助手
     * @param message 管理员消息
     */
    String chatWithAdmin(String message);
}