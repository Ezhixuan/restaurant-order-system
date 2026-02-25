package com.restaurant.report.service;

import com.restaurant.order.mapper.OrderItemMapper;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public TodayStatsDTO getTodayStats() {
        TodayStatsDTO stats = new TodayStatsDTO();
        
        BigDecimal revenue = orderMapper.selectTodayRevenue();
        Long count = orderMapper.selectTodayOrderCount();
        
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);
        stats.setOrderCount(count != null ? count : 0L);
        
        if (stats.getOrderCount() > 0) {
            stats.setAvgAmount(stats.getTotalRevenue()
                .divide(BigDecimal.valueOf(stats.getOrderCount()), 2, RoundingMode.HALF_UP));
        } else {
            stats.setAvgAmount(BigDecimal.ZERO);
        }
        
        return stats;
    }

    public List<TopDishDTO> getTopDishes(Integer limit) {
        if (limit == null) limit = 10;
        return orderItemMapper.selectTopDishes(limit);
    }

    public List<TableStatsDTO> getTableStats() {
        return orderMapper.selectTableStats();
    }
}
