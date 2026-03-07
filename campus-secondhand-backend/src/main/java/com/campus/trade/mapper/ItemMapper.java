package com.campus.trade.mapper;

import com.campus.trade.entity.Item;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ItemMapper {

    @Insert("INSERT INTO t_item(seller_id, title, description, category, price, condition_level, cover_image_url, status) " +
            "VALUES(#{sellerId}, #{title}, #{description}, #{category}, #{price}, #{conditionLevel}, #{coverImageUrl}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Item item);

    @Select("SELECT * FROM t_item WHERE id = #{id}")
    Item findById(@Param("id") Long id);

    @Update("UPDATE t_item SET title=#{title}, description=#{description}, category=#{category}, price=#{price}, condition_level=#{conditionLevel}, cover_image_url=#{coverImageUrl}, updated_at=NOW() WHERE id=#{id}")
    int update(Item item);

    @Delete("DELETE FROM t_item WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT cover_image_url FROM t_item WHERE cover_image_url IS NOT NULL AND cover_image_url <> ''")
    List<String> listAllCoverUrls();

    @Update("UPDATE t_item SET status=#{toStatus}, updated_at=NOW() WHERE status=#{fromStatus}")
    int updateStatusByStatus(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

    @Delete("DELETE FROM t_item")
    int deleteAll();

    @Update("UPDATE t_item SET status=#{status}, audit_reason=#{auditReason}, updated_at=NOW() WHERE id=#{id}")
    int updateAudit(@Param("id") Long id, @Param("status") String status, @Param("auditReason") String auditReason);

    @Update("UPDATE t_item SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE t_item SET status=#{toStatus}, updated_at=NOW() WHERE id=#{id} AND status=#{fromStatus}")
    int updateStatusIfCurrent(@Param("id") Long id, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

    @Select({
            "<script>",
            "SELECT * FROM t_item",
            "<where>",
            "  <if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "  <if test='category != null and category != \"\"'> AND category = #{category} </if>",
            "  <if test='keyword != null and keyword != \"\"'> AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) </if>",
            "</where>",
            "ORDER BY created_at DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Item> list(@Param("status") String status,
                    @Param("category") String category,
                    @Param("keyword") String keyword,
                    @Param("limit") int limit,
                    @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT * FROM t_item",
            "<where>",
            "  seller_id = #{sellerId}",
            "  <if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "  <if test='category != null and category != \"\"'> AND category = #{category} </if>",
            "  <if test='keyword != null and keyword != \"\"'> AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) </if>",
            "</where>",
            "ORDER BY created_at DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Item> listBySellerId(@Param("sellerId") long sellerId,
                             @Param("status") String status,
                             @Param("category") String category,
                             @Param("keyword") String keyword,
                             @Param("limit") int limit,
                             @Param("offset") int offset);
}
