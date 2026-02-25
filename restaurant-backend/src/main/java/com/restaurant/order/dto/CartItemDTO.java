package com.restaurant.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    
    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal price;
    private Integer quantity;
    private String remark;
}
