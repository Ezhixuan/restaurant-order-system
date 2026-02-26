package com.restaurant.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.dish.dto.*;
import com.restaurant.dish.entity.Dish;
import com.restaurant.dish.entity.DishCategory;
import com.restaurant.dish.entity.DishSpec;
import com.restaurant.dish.mapper.CategoryMapper;
import com.restaurant.dish.mapper.DishMapper;
import com.restaurant.dish.mapper.DishSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;
    private final DishSpecMapper dishSpecMapper;

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

    /**
     * 按分类获取菜品（包含规格）- Pad端点餐使用
     */
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
                List<Dish> dishes = dishMapper.selectList(wrapper);
                
                // 加载每个菜品的规格
                for (Dish dish : dishes) {
                    if (dish.getHasSpecs() != null && dish.getHasSpecs() == 1) {
                        List<DishSpec> specs = dishSpecMapper.selectByDishId(dish.getId());
                        dish.setSpecs(specs);
                    }
                }
                
                dto.setDishes(dishes);
                return dto;
            })
            .collect(Collectors.toList());
    }

    public Dish getDishById(Long id) {
        return dishMapper.selectById(id);
    }

    /**
     * 获取菜品详情（包含规格）
     */
    public DishDetailDTO getDetailWithSpecs(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        DishDetailDTO dto = new DishDetailDTO();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setImage(dish.getImage());
        dto.setStock(dish.getStock());
        dto.setStatus(dish.getStatus());
        dto.setHasSpecs(dish.getHasSpecs());
        dto.setPrice(dish.getPrice());
        
        // 加载规格
        if (dish.getHasSpecs() != null && dish.getHasSpecs() == 1) {
            List<DishSpec> specs = dishSpecMapper.selectAllByDishId(dish.getId());
            List<DishSpecVO> specVOs = specs.stream().map(this::convertToSpecVO).collect(Collectors.toList());
            dto.setSpecs(specVOs);
        }
        
        return dto;
    }

    private DishSpecVO convertToSpecVO(DishSpec spec) {
        DishSpecVO vo = new DishSpecVO();
        vo.setId(spec.getId());
        vo.setName(spec.getName());
        vo.setPrice(spec.getPrice());
        vo.setSortOrder(spec.getSortOrder());
        vo.setStatus(spec.getStatus());
        return vo;
    }

    public void createDish(CreateDishRequest request) {
        Dish dish = new Dish();
        dish.setCategoryId(request.getCategoryId());
        dish.setName(request.getName());
        dish.setDescription(request.getDescription());
        dish.setPrice(request.getPrice());
        dish.setImage(request.getImage());
        dish.setStock(request.getStock() != null ? request.getStock() : 999);
        dish.setIsRecommend(request.getIsRecommend());
        dish.setStatus(1); // 默认上架
        dish.setSortOrder(request.getSortOrder());
        dish.setHasSpecs(0); // 默认无规格
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

    /**
     * 切换菜品规格模式
     */
    @Transactional
    public void toggleHasSpecs(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        // 切换 hasSpecs 状态
        int newHasSpecs = (dish.getHasSpecs() == null || dish.getHasSpecs() == 0) ? 1 : 0;
        dish.setHasSpecs(newHasSpecs);
        
        // 如果关闭规格模式，清空规格
        if (newHasSpecs == 0) {
            LambdaQueryWrapper<DishSpec> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DishSpec::getDishId, id);
            dishSpecMapper.delete(wrapper);
        }
        
        dishMapper.updateById(dish);
    }

    /**
     * 快速更新价格 (Pad端专用)
     */
    public void updatePrice(Long id, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于0");
        }
        
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        dish.setPrice(price);
        dishMapper.updateById(dish);
    }
}
