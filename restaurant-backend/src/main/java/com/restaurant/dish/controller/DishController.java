package com.restaurant.dish.controller;

import com.restaurant.common.Result;
import com.restaurant.dish.dto.*;
import com.restaurant.dish.entity.Dish;
import com.restaurant.dish.entity.DishCategory;
import com.restaurant.dish.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    // ========== 分类接口 ==========

    @GetMapping("/categories")
    public Result<List<DishCategory>> listCategories() {
        return Result.success(dishService.listCategories());
    }

    @PostMapping("/categories")
    public Result<Void> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        dishService.createCategory(request);
        return Result.success();
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        dishService.updateCategory(id, request);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        dishService.deleteCategory(id);
        return Result.success();
    }

    // ========== 菜品接口 ==========

    @GetMapping
    public Result<List<Dish>> list(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Integer status) {
        return Result.success(dishService.listDishes(categoryId, status));
    }

    @GetMapping("/by-category")
    public Result<List<CategoryWithDishesDTO>> listByCategory() {
        return Result.success(dishService.listDishesByCategory());
    }

    @GetMapping("/{id}")
    public Result<Dish> getById(@PathVariable Long id) {
        return Result.success(dishService.getDishById(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateDishRequest request) {
        dishService.createDish(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateDishRequest request) {
        dishService.updateDish(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.deleteDish(id);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        dishService.toggleStatus(id);
        return Result.success();
    }
}
