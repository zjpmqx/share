package com.campus.trade.mapper;

import com.campus.trade.entity.ShareComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShareCommentMapper {

    @Insert("INSERT INTO t_share_comment(share_id, user_id, content) VALUES(#{shareId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShareComment comment);

    @Select("SELECT c.*, u.username AS userName, u.avatar_url AS avatarUrl FROM t_share_comment c LEFT JOIN t_user u ON u.id = c.user_id WHERE c.share_id = #{shareId} ORDER BY c.created_at ASC")
    List<ShareComment> listByShareId(@Param("shareId") Long shareId);

    @Select("SELECT content FROM t_share_comment WHERE content IS NOT NULL AND content <> ''")
    List<String> listAllContents();

    @Delete("DELETE FROM t_share_comment WHERE share_id = #{shareId}")
    int deleteByShareId(@Param("shareId") Long shareId);
}
