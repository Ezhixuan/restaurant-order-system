package com.restaurant.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.dish.entity.DishCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<DishCategory> {
    
    @Select("SELECT * FROM dish_category WHERE status = 1 ORDER BY sort_order")
    List<DishCategory> selectActiveCategories();
}
