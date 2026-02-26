package com.restaurant.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {
    
    private String orderNo;           // 订单编号
    private Long tableId;             // 桌台ID
    private String tableNo;           // 桌号
    private Integer customerCount;    // 用餐人数
    private BigDecimal totalAmount;   // 订单总金额
    private BigDecimal discountAmount;// 优惠金额
    private BigDecimal payAmount;     // 实付金额
    private Integer payType;          // 支付方式: 0未支付 1微信 2支付宝 3现金
    private LocalDateTime payTime;    // 支付时间
    private Integer status;           // 状态: 0待上菜 1上菜中 2待结账 3已完成 4已取消
    private String remark;            // 订单备注
}
