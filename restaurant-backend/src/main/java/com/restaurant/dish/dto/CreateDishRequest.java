package com.restaurant.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDishRequest {
    
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    
    @NotBlank(message = "菜品名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    
    private String image;
    
    private Integer stock = 999;
    
    private Integer isRecommend = 0;
    
    private Integer sortOrder = 0;
}
