package com.restaurant.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.mapper.OrderItemMapper;
import com.restaurant.order.mapper.OrderMapper;
import com.restaurant.table.entity.RestaurantTable;
import com.restaurant.table.mapper.TableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TableMapper tableMapper;

    /**
     * 根据菜品状态自动更新订单状态
     * 规则：
     * - 待上菜(0)：新建订单默认状态
     * - 上菜中(1)：有菜品在制作中或已完成
     * - 待结账(2)：所有菜品都已完成（制作完成）
     * - 已完成(3)：已结账
     * - 追加订单(4)：已结账后又加菜
     */
    public void updateOrderStatus(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        if (items.isEmpty()) {
            return;
        }

        int currentStatus = order.getStatus();

        // 已完成(3)或追加订单(4)状态不再自动变更
        if (currentStatus >= 3) {
            return;
        }

        boolean hasCooking = items.stream().anyMatch(i -> i.getStatus() == 1);
        boolean hasFinished = items.stream().anyMatch(i -> i.getStatus() == 2);
        boolean allFinished = items.stream().allMatch(i -> i.getStatus() == 2);

        if (currentStatus == 0) {
            // 待上菜状态
            if (hasCooking || hasFinished) {
                order.setStatus(1); // 转为上菜中
                orderMapper.updateById(order);
            }
        } else if (currentStatus == 1) {
            // 上菜中状态
            if (allFinished) {
                order.setStatus(2); // 转为待结账
                orderMapper.updateById(order);
            }
        }
    }

    /**
     * 获取未结账的菜品
     */
    public List<OrderItem> getUnpaidItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId)
               .eq(OrderItem::getIsPaid, 0);
        return orderItemMapper.selectList(wrapper);
    }

    /**
     * 计算未结账金额
     */
    public BigDecimal getUnpaidAmount(Long orderId) {
        List<OrderItem> unpaidItems = getUnpaidItems(orderId);
        return unpaidItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 结账
     * 支持部分结账（已结账后加菜的情况）
     */
    @Transactional
    public void checkout(Long orderId, Integer payType, BigDecimal amount) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 获取未结账的菜品
        List<OrderItem> unpaidItems = getUnpaidItems(orderId);
        if (unpaidItems.isEmpty()) {
            throw new BusinessException("没有待结账的菜品");
        }

        // 计算应付金额
        BigDecimal shouldPay = unpaidItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 验证金额
        if (amount.compareTo(shouldPay) < 0) {
            throw new BusinessException("支付金额不足，应付：¥" + shouldPay);
        }

        // 标记未结账的菜品为已结账
        for (OrderItem item : unpaidItems) {
            item.setIsPaid(1);
            orderItemMapper.updateById(item);
        }

        // 更新订单支付信息
        order.setPayAmount(order.getPayAmount().add(amount));
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());

        // 检查是否还有未结账菜品
        List<OrderItem> remainingUnpaid = getUnpaidItems(orderId);
        if (remainingUnpaid.isEmpty()) {
            // 全部结账完成
            order.setStatus(3); // 已完成
            
            // 更新桌台状态为待清台
            RestaurantTable table = tableMapper.selectById(order.getTableId());
            if (table != null) {
                table.setStatus(2); // 待清台
                tableMapper.updateById(table);
            }
        } else {
            // 还有未结账菜品（追加订单情况）
            order.setStatus(4); // 追加订单
        }

        orderMapper.updateById(order);
    }

    /**
     * 完成订单（上菜完毕）
     */
    public void completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 检查是否还有未结账菜品
        List<OrderItem> unpaidItems = getUnpaidItems(orderId);
        if (!unpaidItems.isEmpty()) {
            throw new BusinessException("还有未结账的菜品，请先结账");
        }

        order.setStatus(3); // 已完成
        orderMapper.updateById(order);
    }

    /**
     * 更新菜品状态（供服务员/后厨调用）
     * 更新后自动更新订单状态
     */
    @Transactional
    public void updateItemStatus(Long itemId, Integer status) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("菜品不存在");
        }

        item.setStatus(status);
        orderItemMapper.updateById(item);

        // 自动更新订单状态
        updateOrderStatus(item.getOrderId());
    }
}
