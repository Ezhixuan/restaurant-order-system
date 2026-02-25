package com.restaurant.order.dto;

import lombok.Data;

@Data
public class AddDishRequest {
    
    private Long dishId;
    private Integer quantity;
    private String remark;
}
