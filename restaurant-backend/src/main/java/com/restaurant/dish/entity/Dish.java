package com.restaurant.dish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish")
public class Dish extends BaseEntity {
    
    private Long categoryId;       // 分类ID
    private String name;           // 菜品名称
    private String description;    // 菜品描述
    private BigDecimal price;      // 售价(无规格时的基础价格)
    private String image;          // 菜品图片
    private Integer stock;         // 库存(-1表示不限)
    private Integer isRecommend;   // 是否推荐: 0否 1是
    private Integer status;        // 状态: 0下架 1上架
    private Integer sortOrder;     // 排序
    private Integer hasSpecs;      // 是否有规格: 0无 1有
    
    // 非持久化字段
    @TableField(exist = false)
    private List<DishSpec> specs;  // 规格列表
}
