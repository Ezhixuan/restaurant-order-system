package com.restaurant.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer role;      // 1服务员 2管理员
    private Integer status;    // 0禁用 1启用
}
