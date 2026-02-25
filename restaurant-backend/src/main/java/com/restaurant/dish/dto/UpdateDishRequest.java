package com.restaurant.dish.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDishRequest {
    
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private Integer stock;
    private Integer isRecommend;
    private Integer status;
    private Integer sortOrder;
}
