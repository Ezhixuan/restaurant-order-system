package com.restaurant.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.dish.dto.*;
import com.restaurant.dish.entity.Dish;
import com.restaurant.dish.entity.DishCategory;
import com.restaurant.dish.mapper.CategoryMapper;
import com.restaurant.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;

    // ========== 分类管理 ==========

    public List<DishCategory> listCategories() {
        LambdaQueryWrapper<DishCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DishCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    public void createCategory(CreateCategoryRequest request) {
        DishCategory category = new DishCategory();
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    public void updateCategory(Long id, CreateCategoryRequest request) {
        DishCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());
        categoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        // 检查是否有菜品使用此分类
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getCategoryId, id);
        if (dishMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该分类下还有菜品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    // ========== 菜品管理 ==========

    public List<Dish> listDishes(Long categoryId, Integer status) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Dish::getStatus, status);
        }
        wrapper.orderByDesc(Dish::getIsRecommend)
               .orderByAsc(Dish::getSortOrder);
        return dishMapper.selectList(wrapper);
    }

    public List<CategoryWithDishesDTO> listDishesByCategory() {
        List<DishCategory> categories = listCategories();
        return categories.stream()
            .filter(c -> c.getStatus() == 1)
            .map(category -> {
                CategoryWithDishesDTO dto = new CategoryWithDishesDTO();
                dto.setId(category.getId());
                dto.setName(category.getName());
                
                LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Dish::getCategoryId, category.getId())
                       .eq(Dish::getStatus, 1)
                       .orderByDesc(Dish::getIsRecommend)
                       .orderByAsc(Dish::getSortOrder);
                dto.setDishes(dishMapper.selectList(wrapper));
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    public Dish getDishById(Long id) {
        return dishMapper.selectById(id);
    }

    public void createDish(CreateDishRequest request) {
        Dish dish = new Dish();
        dish.setCategoryId(request.getCategoryId());
        dish.setName(request.getName());
        dish.setDescription(request.getDescription());
        dish.setPrice(request.getPrice());
        dish.setImage(request.getImage());
        dish.setStock(request.getStock());
        dish.setIsRecommend(request.getIsRecommend());
        dish.setStatus(1); // 默认上架
        dish.setSortOrder(request.getSortOrder());
        dishMapper.insert(dish);
    }

    public void updateDish(Long id, UpdateDishRequest request) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }

        if (request.getCategoryId() != null) {
            dish.setCategoryId(request.getCategoryId());
        }
        if (request.getName() != null) {
            dish.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dish.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            dish.setPrice(request.getPrice());
        }
        if (request.getImage() != null) {
            dish.setImage(request.getImage());
        }
        if (request.getStock() != null) {
            dish.setStock(request.getStock());
        }
        if (request.getIsRecommend() != null) {
            dish.setIsRecommend(request.getIsRecommend());
        }
        if (request.getStatus() != null) {
            dish.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            dish.setSortOrder(request.getSortOrder());
        }

        dishMapper.updateById(dish);
    }

    public void deleteDish(Long id) {
        dishMapper.deleteById(id);
    }

    public void toggleStatus(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        dish.setStatus(dish.getStatus() == 1 ? 0 : 1);
        dishMapper.updateById(dish);
    }
}
