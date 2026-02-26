package com.restaurant.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 菜品规格实体
 * 用于支持同一菜品的多种份量/规格选择
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_spec")
public class DishSpec extends BaseEntity {
    
    private Long dishId;           // 菜品ID
    private String name;           // 规格名称: 小份/中份/大份/例份等
    private BigDecimal price;      // 规格价格
    private Integer sortOrder;     // 排序
    private Integer status;        // 状态: 0禁用 1启用
}
