package com.restaurant.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TodayStatsDTO {
    
    private BigDecimal totalRevenue;  // 今日营业额
    private Long orderCount;          // 今日订单数
    private BigDecimal avgAmount;     // 平均客单价
}
