package com.campus.trade.mapper;

import com.campus.trade.entity.ItemMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ItemMessageMapper {

    @Insert("INSERT INTO t_item_message(item_id, from_user_id, content) VALUES(#{itemId}, #{fromUserId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ItemMessage message);

    @Select("SELECT m.*, u.username AS userName, u.avatar_url AS avatarUrl FROM t_item_message m LEFT JOIN t_user u ON u.id = m.from_user_id WHERE m.item_id = #{itemId} ORDER BY m.created_at ASC")
    List<ItemMessage> listByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM t_item_message WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM t_item_message")
    int deleteAll();
}
