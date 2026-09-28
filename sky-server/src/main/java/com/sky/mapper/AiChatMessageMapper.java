package com.sky.mapper;

import com.sky.entity.AiChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI聊天消息数据访问层接口
 */
@Mapper
public interface AiChatMessageMapper {

    /**
     * 插入一条消息
     */
    @Insert("insert into ai_chat_message (session_id, role, content, create_time) " +
            "values (#{sessionId}, #{role}, #{content}, #{createTime})")
    void insert(AiChatMessage message);

    /**
     * 查询会话的最近limit条消息，按时间正序返回（用于构建多轮对话上下文）
     */
    @Select("select * from (select * from ai_chat_message where session_id = #{sessionId} " +
            "order by id desc limit #{limit}) t order by id asc")
    List<AiChatMessage> listLatest(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /**
     * 查询会话的全部消息，按时间正序返回（用于历史记录展示）
     */
    @Select("select * from ai_chat_message where session_id = #{sessionId} order by id asc")
    List<AiChatMessage> listBySessionId(String sessionId);
}
