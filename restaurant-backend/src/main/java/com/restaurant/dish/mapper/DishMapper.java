package com.restaurant.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    
    @Select("SELECT * FROM dish WHERE status = 1 AND is_deleted = 0 ORDER BY is_recommend DESC, sort_order")
    List<Dish> selectAvailableDishes();
}
