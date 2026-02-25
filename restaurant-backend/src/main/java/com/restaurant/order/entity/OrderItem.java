package com.restaurant.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {
    
    private Long orderId;         // 订单ID
    private Long dishId;          // 菜品ID
    private String dishName;      // 菜品名称(快照)
    private String dishImage;     // 菜品图片(快照)
    private BigDecimal price;     // 单价(快照)
    private Integer quantity;     // 数量
    private BigDecimal subtotal;  // 小计金额
    private String remark;        // 备注
    private Integer status;       // 状态: 0待制作 1制作中 2已完成
}
