package com.restaurant.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_category")
public class DishCategory extends BaseEntity {
    
    private String name;           // 分类名称
    private Integer sortOrder;     // 排序
    private Integer status;        // 状态: 0禁用 1启用
}
