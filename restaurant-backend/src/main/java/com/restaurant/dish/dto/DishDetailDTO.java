package com.restaurant.dish.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品详情DTO (含规格)
 */
@Data
public class DishDetailDTO {
    
    private Long id;
    private String name;
    private String description;
    private String image;
    private Integer stock;
    private Integer status;
    private Integer hasSpecs;
    private BigDecimal price;  // 基础价格(无规格时)
    private List<DishSpecVO> specs;  // 规格列表
}
