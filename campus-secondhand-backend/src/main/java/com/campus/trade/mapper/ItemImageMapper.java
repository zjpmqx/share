package com.campus.trade.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ItemImageMapper {

    @Select("SELECT url FROM t_item_image WHERE item_id = #{itemId} ORDER BY sort_no ASC")
    List<String> listUrlsByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM t_item_image WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") Long itemId);

    @Select("SELECT url FROM t_item_image")
    List<String> listAllUrls();

    @Delete("DELETE FROM t_item_image")
    int deleteAll();
}
