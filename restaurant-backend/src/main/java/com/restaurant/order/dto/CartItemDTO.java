package com.restaurant.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    
    private Long dishId;
    private Long specId;           // 规格ID(可选)
    private String dishName;
    private String specName;       // 规格名称
    private String dishImage;
    private BigDecimal price;
    private Integer quantity;
    private String remark;
}
