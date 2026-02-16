package com.campus.trade.mapper;

import com.campus.trade.entity.SharePost;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SharePostMapper {

    @Insert("INSERT INTO t_share_post(user_id, title, content, media_type, media_url, link_url) VALUES(#{userId}, #{title}, #{content}, #{mediaType}, #{mediaUrl}, #{linkUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SharePost post);

    @Select("SELECT * FROM t_share_post WHERE id = #{id}")
    SharePost findById(@Param("id") Long id);

    @Select("SELECT * FROM t_share_post ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<SharePost> list(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT media_url FROM t_share_post WHERE media_url IS NOT NULL AND media_url <> ''")
    List<String> listAllMediaUrls();

    @Select("SELECT content FROM t_share_post WHERE content IS NOT NULL AND content <> ''")
    List<String> listAllContents();

    @Delete("DELETE FROM t_share_post WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
