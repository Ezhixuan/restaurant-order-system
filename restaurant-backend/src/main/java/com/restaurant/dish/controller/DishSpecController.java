package com.restaurant.dish.controller;

import com.restaurant.common.Result;
import com.restaurant.dish.dto.*;
import com.restaurant.dish.entity.DishSpec;
import com.restaurant.dish.service.DishSpecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品规格管理接口
 * 主要用于 Pad 端配置菜品份额
 */
@RestController
@RequestMapping("/api/dish-specs")
@RequiredArgsConstructor
public class DishSpecController {
    
    private final DishSpecService dishSpecService;
    
    /**
     * 获取菜品的所有规格
     */
    @GetMapping("/dish/{dishId}")
    public Result<List<DishSpec>> listByDish(@PathVariable Long dishId) {
        return Result.success(dishSpecService.listByDishId(dishId));
    }
    
    /**
     * 获取菜品的所有规格(包含禁用)
     */
    @GetMapping("/dish/{dishId}/all")
    public Result<List<DishSpec>> listAllByDish(@PathVariable Long dishId) {
        return Result.success(dishSpecService.listAllByDishId(dishId));
    }
    
    /**
     * 根据ID获取规格
     */
    @GetMapping("/{id}")
    public Result<DishSpec> getById(@PathVariable Long id) {
        return Result.success(dishSpecService.getById(id));
    }
    
    /**
     * 创建规格
     */
    @PostMapping
    public Result<DishSpec> create(@RequestBody @Valid CreateSpecRequest request) {
        return Result.success(dishSpecService.create(request));
    }
    
    /**
     * 更新规格
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateSpecRequest request) {
        dishSpecService.update(id, request);
        return Result.success();
    }
    
    /**
     * 删除规格
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishSpecService.delete(id);
        return Result.success();
    }
    
    /**
     * 批量更新菜品规格 (Pad 端专用)
     * 一次性保存菜品的所有规格
     */
    @PostMapping("/batch/{dishId}")
    public Result<Void> batchUpdate(
            @PathVariable Long dishId,
            @RequestBody @Valid List<SpecItemRequest> specs) {
        dishSpecService.batchUpdate(dishId, specs);
        return Result.success();
    }
    
    /**
     * 启用/禁用规格
     */
    @PostMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        dishSpecService.toggleStatus(id);
        return Result.success();
    }
}
