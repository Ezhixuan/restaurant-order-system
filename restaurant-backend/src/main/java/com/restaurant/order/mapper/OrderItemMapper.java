package com.restaurant.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.report.dto.TopDishDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);
    
    @Select("SELECT dish_name as dishName, SUM(quantity) as totalQuantity, SUM(subtotal) as totalAmount " +
            "FROM order_item oi JOIN orders o ON oi.order_id = o.id " +
            "WHERE DATE(o.created_at) = CURDATE() AND o.status = 4 " +
            "GROUP BY oi.dish_id, oi.dish_name " +
            "ORDER BY totalQuantity DESC LIMIT #{limit}")
    List<TopDishDTO> selectTopDishes(@Param("limit") Integer limit);
}
