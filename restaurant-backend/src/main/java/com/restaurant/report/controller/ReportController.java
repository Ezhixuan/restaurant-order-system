package com.restaurant.report.controller;

import com.restaurant.common.Result;
import com.restaurant.report.dto.*;
import com.restaurant.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/today")
    public Result<TodayStatsDTO> getTodayStats() {
        return Result.success(reportService.getTodayStats());
    }

    @GetMapping("/top-dishes")
    public Result<List<TopDishDTO>> getTopDishes(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(reportService.getTopDishes(limit));
    }

    @GetMapping("/tables")
    public Result<List<TableStatsDTO>> getTableStats() {
        return Result.success(reportService.getTableStats());
    }
}
