package com.restaurant.table.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("restaurant_table")
public class RestaurantTable extends BaseEntity {
    
    private String tableNo;      // 桌号: A01、临1
    private String name;         // 桌台名称
    private Integer type;        // 类型: 1固定卡座 2临时座位
    private Integer capacity;    // 容纳人数
    private String qrcode;       // 点餐二维码
    private Integer status;      // 状态: 0空闲 1使用中 2待清台
    private Integer sortOrder;   // 排序
}
