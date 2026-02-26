package com.restaurant.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.dish.entity.DishSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishSpecMapper extends BaseMapper<DishSpec> {
    
    /**
     * 根据菜品ID查询所有启用的规格
     */
    @Select("SELECT * FROM dish_spec WHERE dish_id = #{dishId} AND status = 1 AND is_deleted = 0 ORDER BY sort_order")
    List<DishSpec> selectByDishId(@Param("dishId") Long dishId);
    
    /**
     * 根据菜品ID查询所有规格(包含禁用)
     */
    @Select("SELECT * FROM dish_spec WHERE dish_id = #{dishId} AND is_deleted = 0 ORDER BY sort_order")
    List<DishSpec> selectAllByDishId(@Param("dishId") Long dishId);
    
    /**
     * 批量插入规格
     */
    void batchInsert(@Param("list") List<DishSpec> specs);
    
    /**
     * 根据菜品ID删除所有规格
     */
    @Select("DELETE FROM dish_spec WHERE dish_id = #{dishId}")
    void deleteByDishId(@Param("dishId") Long dishId);
}
