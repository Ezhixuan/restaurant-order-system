package com.restaurant.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TableStatsDTO {
    
    private String tableNo;
    private String tableName;
    private Long orderCount;
    private BigDecimal totalAmount;
}
