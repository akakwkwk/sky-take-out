package com.sky.mapper;

import com.sky.entity.AiChatSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI会话数据访问层接口
 */
@Mapper
public interface AiChatSessionMapper {

    /**
     * 插入新会话
     */
    @Insert("insert into ai_chat_session (session_id, user_id, role, title, create_time) " +
            "values (#{sessionId}, #{userId}, #{role}, #{title}, #{createTime})")
    void insert(AiChatSession session);

    /**
     * 根据会话ID查询会话
     */
    @Select("select * from ai_chat_session where session_id = #{sessionId}")
    AiChatSession getBySessionId(String sessionId);

    /**
     * 查询指定用户的用户端会话列表（按时间倒序）
     */
    @Select("select * from ai_chat_session where role = 'user' and user_id = #{userId} " +
            "order by create_time desc, id desc")
    List<AiChatSession> listByUserId(Long userId);

    /**
     * 查询未登录用户(user_id为null)的用户端会话列表（按时间倒序）
     */
    @Select("select * from ai_chat_session where role = 'user' and user_id is null " +
            "order by create_time desc, id desc")
    List<AiChatSession> listAnonymous();

    /**
     * 查询管理端会话列表（按时间倒序）
     */
    @Select("select * from ai_chat_session where role = 'admin' " +
            "order by create_time desc, id desc")
    List<AiChatSession> listAdmin();
}
