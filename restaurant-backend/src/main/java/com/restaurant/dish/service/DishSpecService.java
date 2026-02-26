package com.restaurant.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.dish.dto.*;
import com.restaurant.dish.entity.Dish;
import com.restaurant.dish.entity.DishSpec;
import com.restaurant.dish.mapper.DishMapper;
import com.restaurant.dish.mapper.DishSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishSpecService {
    
    private final DishSpecMapper specMapper;
    private final DishMapper dishMapper;
    
    /**
     * 获取菜品的所有启用的规格
     */
    public List<DishSpec> listByDishId(Long dishId) {
        LambdaQueryWrapper<DishSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishSpec::getDishId, dishId)
               .eq(DishSpec::getStatus, 1)
               .orderByAsc(DishSpec::getSortOrder);
        return specMapper.selectList(wrapper);
    }
    
    /**
     * 获取菜品的所有规格(包含禁用)
     */
    public List<DishSpec> listAllByDishId(Long dishId) {
        LambdaQueryWrapper<DishSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishSpec::getDishId, dishId)
               .orderByAsc(DishSpec::getSortOrder);
        return specMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取规格
     */
    public DishSpec getById(Long id) {
        return specMapper.selectById(id);
    }
    
    /**
     * 创建规格
     */
    public DishSpec create(CreateSpecRequest request) {
        // 验证菜品存在
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        DishSpec spec = new DishSpec();
        spec.setDishId(request.getDishId());
        spec.setName(request.getName());
        spec.setPrice(request.getPrice());
        spec.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        spec.setStatus(1);
        specMapper.insert(spec);
        
        // 更新菜品 hasSpecs 标志
        if (dish.getHasSpecs() == null || dish.getHasSpecs() == 0) {
            dish.setHasSpecs(1);
            dishMapper.updateById(dish);
        }
        
        return spec;
    }
    
    /**
     * 更新规格
     */
    public void update(Long id, UpdateSpecRequest request) {
        DishSpec spec = specMapper.selectById(id);
        if (spec == null) {
            throw new BusinessException("规格不存在");
        }
        
        if (request.getName() != null) {
            spec.setName(request.getName());
        }
        if (request.getPrice() != null) {
            spec.setPrice(request.getPrice());
        }
        if (request.getSortOrder() != null) {
            spec.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            spec.setStatus(request.getStatus());
        }
        
        specMapper.updateById(spec);
    }
    
    /**
     * 删除规格
     */
    public void delete(Long id) {
        DishSpec spec = specMapper.selectById(id);
        if (spec == null) {
            throw new BusinessException("规格不存在");
        }
        
        specMapper.deleteById(id);
        
        // 检查是否还有规格，如果没有则更新菜品标志
        List<DishSpec> remainingSpecs = listByDishId(spec.getDishId());
        if (remainingSpecs.isEmpty()) {
            Dish dish = dishMapper.selectById(spec.getDishId());
            if (dish != null) {
                dish.setHasSpecs(0);
                dishMapper.updateById(dish);
            }
        }
    }
    
    /**
     * 批量更新规格 (Pad 端专用)
     * 一次性保存菜品的所有规格
     */
    @Transactional
    public void batchUpdate(Long dishId, List<SpecItemRequest> specs) {
        // 1. 验证菜品存在
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        // 2. 获取现有规格
        List<DishSpec> existingSpecs = listAllByDishId(dishId);
        Map<Long, DishSpec> existingMap = existingSpecs.stream()
            .collect(Collectors.toMap(DishSpec::getId, s -> s));
        
        // 3. 处理传入的规格列表
        Set<Long> processedIds = new HashSet<>();
        int sortOrder = 0;
        
        for (SpecItemRequest specReq : specs) {
            // 验证必填字段
            if (specReq.getName() == null || specReq.getName().trim().isEmpty()) {
                throw new BusinessException("规格名称不能为空");
            }
            if (specReq.getPrice() == null || specReq.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("规格价格必须大于0");
            }
            
            if (specReq.getId() != null && existingMap.containsKey(specReq.getId())) {
                // 更新现有规格
                DishSpec existing = existingMap.get(specReq.getId());
                existing.setName(specReq.getName().trim());
                existing.setPrice(specReq.getPrice());
                existing.setSortOrder(sortOrder++);
                if (specReq.getStatus() != null) {
                    existing.setStatus(specReq.getStatus());
                }
                specMapper.updateById(existing);
                processedIds.add(specReq.getId());
            } else {
                // 创建新规格
                DishSpec newSpec = new DishSpec();
                newSpec.setDishId(dishId);
                newSpec.setName(specReq.getName().trim());
                newSpec.setPrice(specReq.getPrice());
                newSpec.setSortOrder(sortOrder++);
                newSpec.setStatus(specReq.getStatus() != null ? specReq.getStatus() : 1);
                specMapper.insert(newSpec);
            }
        }
        
        // 4. 删除未包含的规格
        for (DishSpec existing : existingSpecs) {
            if (!processedIds.contains(existing.getId())) {
                specMapper.deleteById(existing.getId());
            }
        }
        
        // 5. 更新菜品 hasSpecs 标志
        boolean hasActiveSpecs = specs.stream()
            .anyMatch(s -> s.getStatus() == null || s.getStatus() == 1);
        dish.setHasSpecs(hasActiveSpecs ? 1 : 0);
        dishMapper.updateById(dish);
    }
    
    /**
     * 启用/禁用规格
     */
    public void toggleStatus(Long id) {
        DishSpec spec = specMapper.selectById(id);
        if (spec == null) {
            throw new BusinessException("规格不存在");
        }
        
        spec.setStatus(spec.getStatus() == 1 ? 0 : 1);
        specMapper.updateById(spec);
        
        // 检查是否还有启用的规格
        List<DishSpec> activeSpecs = listByDishId(spec.getDishId());
        Dish dish = dishMapper.selectById(spec.getDishId());
        if (dish != null) {
            dish.setHasSpecs(activeSpecs.isEmpty() ? 0 : 1);
            dishMapper.updateById(dish);
        }
    }
    
    /**
     * 获取规格价格
     */
    public BigDecimal getSpecPrice(Long specId) {
        DishSpec spec = specMapper.selectById(specId);
        if (spec == null || spec.getStatus() != 1) {
            throw new BusinessException("规格不存在或已禁用");
        }
        return spec.getPrice();
    }
}
