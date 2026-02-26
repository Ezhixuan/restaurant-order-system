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
     * - 待上菜(0)：没有任何菜品在制作中或已完成（即使已结账）
     * - 上菜中(1)：有菜品在制作中或已完成，但未全部完成
     * - 待结账(2)：所有菜品都已完成，但未结账
     * - 已完成(3)：所有菜品都已完成，且已结账
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

        // 计算菜品状态
        boolean hasCookingOrCompleted = items.stream().anyMatch(i -> i.getStatus() == 1 || i.getStatus() == 2);
        boolean allCompleted = items.stream().allMatch(i -> i.getStatus() == 2);
        
        // 检查是否全部已结账
        boolean allPaid = items.stream().allMatch(i -> i.getIsPaid() != null && i.getIsPaid() == 1);

        int newStatus;
        if (allCompleted) {
            // 所有菜品都已完成
            if (allPaid) {
                newStatus = 3; // 已完成
            } else {
                newStatus = 2; // 待结账
            }
        } else if (hasCookingOrCompleted) {
            // 有菜品在制作中或已完成，但未全部完成
            newStatus = 1; // 上菜中
        } else {
            // 没有任何菜品在制作中或已完成（即使已结账也是待上菜）
            newStatus = 0; // 待上菜
        }

        // 只有在状态真正变化时才更新
        if (order.getStatus() != newStatus) {
            order.setStatus(newStatus);
            orderMapper.updateById(order);
            
            // 如果订单完成，更新桌台状态为待清台
            if (newStatus == 3) {
                RestaurantTable table = tableMapper.selectById(order.getTableId());
                if (table != null && table.getStatus() != 2) {
                    table.setStatus(2); // 待清台
                    tableMapper.updateById(table);
                }
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
     * 支持抹零优惠（支付金额可以小于应付金额）
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

        // 验证金额（允许支付金额小于等于应付金额，支持抹零优惠）
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额必须大于0");
        }
        if (amount.compareTo(shouldPay) > 0) {
            throw new BusinessException("支付金额不能超过应付金额：¥" + shouldPay);
        }

        // 计算优惠金额（应付 - 实付）
        BigDecimal discount = shouldPay.subtract(amount);

        // 标记未结账的菜品为已结账
        for (OrderItem item : unpaidItems) {
            item.setIsPaid(1);
            orderItemMapper.updateById(item);
        }

        // 更新订单支付信息
        // 注意：这里直接设置实付金额，不是累加
        // 因为createOrder时payAmount被设为totalAmount（应付），所以这里要减去应付，加上实付
        BigDecimal currentPayAmount = order.getPayAmount();
        BigDecimal newPayAmount = currentPayAmount.subtract(shouldPay).add(amount);
        order.setPayAmount(newPayAmount);
        order.setDiscountAmount(order.getDiscountAmount().add(discount));
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 结账后根据菜品状态更新订单状态
        updateOrderStatus(orderId);
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
