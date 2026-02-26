package com.restaurant.table.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.table.dto.CreateTableRequest;
import com.restaurant.table.dto.UpdateTableRequest;
import com.restaurant.order.entity.Order;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.order.service.OrderService;
import com.restaurant.table.entity.RestaurantTable;
import com.restaurant.table.mapper.TableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableMapper tableMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public List<RestaurantTable> listAll() {
        LambdaQueryWrapper<RestaurantTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RestaurantTable::getSortOrder);
        return tableMapper.selectList(wrapper);
    }

    public List<RestaurantTable> listByType(Integer type) {
        LambdaQueryWrapper<RestaurantTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RestaurantTable::getType, type)
               .orderByAsc(RestaurantTable::getSortOrder);
        return tableMapper.selectList(wrapper);
    }

    public RestaurantTable getById(Long id) {
        return tableMapper.selectById(id);
    }

    public void create(CreateTableRequest request) {
        // 检查桌号是否已存在
        LambdaQueryWrapper<RestaurantTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RestaurantTable::getTableNo, request.getTableNo());
        if (tableMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("桌号已存在");
        }

        RestaurantTable table = new RestaurantTable();
        table.setTableNo(request.getTableNo());
        table.setName(request.getName());
        table.setType(request.getType());
        table.setCapacity(request.getCapacity());
        table.setSortOrder(request.getSortOrder());
        table.setStatus(0); // 空闲
        
        // 固定座位生成二维码URL (实际项目中使用域名)
        if (table.getType() == 1) {
            table.setQrcode("/m/" + table.getTableNo());
        }

        tableMapper.insert(table);
    }

    public void update(Long id, UpdateTableRequest request) {
        RestaurantTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }

        table.setName(request.getName());
        table.setCapacity(request.getCapacity());
        table.setSortOrder(request.getSortOrder());
        
        tableMapper.updateById(table);
    }

    public void delete(Long id) {
        RestaurantTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }
        if (table.getStatus() == 1) {
            throw new BusinessException("桌台正在使用中，无法删除");
        }
        tableMapper.deleteById(id);
    }

    public void openTable(Long id, Integer customerCount) {
        RestaurantTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }
        if (table.getStatus() != 0) {
            throw new BusinessException("桌台不是空闲状态");
        }
        
        table.setStatus(1); // 使用中
        tableMapper.updateById(table);
    }

    public void clearTable(Long id) {
        RestaurantTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }
        if (table.getStatus() != 2) {
            throw new BusinessException("桌台不是待清台状态");
        }
        
        // 查找该桌台下的所有活跃订单（状态小于4的订单）
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getTableId, id)
                   .lt(Order::getStatus, 4); // 活跃订单：待支付(0)、已支付(1)、制作中(2)、待上菜(3)
        
        List<Order> activeOrders = orderMapper.selectList(orderWrapper);
        
        // 将所有活跃订单标记为已完成
        for (Order order : activeOrders) {
            if (order.getStatus() < 4) { // 只处理未完成的订单
                orderService.completeOrder(order.getId());
            }
        }
        
        // 设置桌台为空闲状态
        table.setStatus(0); // 空闲
        tableMapper.updateById(table);
    }

    public void setPendingClear(Long id) {
        RestaurantTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }
        
        table.setStatus(2); // 待清台
        tableMapper.updateById(table);
    }
}
