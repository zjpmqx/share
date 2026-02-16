package com.campus.trade.mapper;

import com.campus.trade.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO t_order(item_id, buyer_id, seller_id, amount, status) VALUES(#{itemId}, #{buyerId}, #{sellerId}, #{amount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("SELECT * FROM t_order WHERE id = #{id}")
    Order findById(@Param("id") Long id);

    @Select("SELECT * FROM t_order WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<Order> listByBuyerId(@Param("buyerId") Long buyerId);

    @Select("SELECT * FROM t_order WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<Order> listBySellerId(@Param("sellerId") Long sellerId);

    @Update("UPDATE t_order SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM t_order WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM t_order")
    int deleteAll();
}
