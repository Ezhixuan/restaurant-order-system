package com.restaurant.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopDishDTO {
    
    private String dishName;
    private Long totalQuantity;
    private BigDecimal totalAmount;
}
