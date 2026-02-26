package com.restaurant.dish.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 规格VO (用于返回给前端)
 */
@Data
public class DishSpecVO {
    
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer sortOrder;
    private Integer status;
}
