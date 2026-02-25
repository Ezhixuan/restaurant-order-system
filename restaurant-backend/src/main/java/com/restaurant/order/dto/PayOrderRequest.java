package com.restaurant.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayOrderRequest {
    
    private Integer payType;  // 1微信 2支付宝 3现金
    private BigDecimal amount;
}
