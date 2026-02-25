package com.restaurant.order.dto;

import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailDTO {
    
    private Order order;
    private List<OrderItem> items;
}
