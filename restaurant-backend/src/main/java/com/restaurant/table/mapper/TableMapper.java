package com.restaurant.table.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.table.entity.RestaurantTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TableMapper extends BaseMapper<RestaurantTable> {
    
    @Update("UPDATE restaurant_table SET status = #{status} WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
}
