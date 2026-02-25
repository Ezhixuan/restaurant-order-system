package com.restaurant.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(String username);
}
