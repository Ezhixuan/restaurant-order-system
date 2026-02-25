package com.restaurant.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);
}
