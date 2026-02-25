package com.restaurant.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish")
public class Dish extends BaseEntity {
    
    private Long categoryId;       // 分类ID
    private String name;           // 菜品名称
    private String description;    // 菜品描述
    private BigDecimal price;      // 售价
    private String image;          // 菜品图片
    private Integer stock;         // 库存(-1表示不限)
    private Integer isRecommend;   // 是否推荐: 0否 1是
    private Integer status;        // 状态: 0下架 1上架
    private Integer sortOrder;     // 排序
}
