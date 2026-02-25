package com.restaurant.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.common.exception.BusinessException;
import com.restaurant.dish.entity.Dish;
import com.restaurant.dish.mapper.DishMapper;
import com.restaurant.order.dto.*;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final TableMapper tableMapper;

    public List<Order> listOrders(Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper);
    }

    public List<Order> listActiveOrders() {
        return orderMapper.selectActiveOrders();
    }

    public OrderDetailDTO getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrder(order);
        dto.setItems(items);
        return dto;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 检查桌台
        RestaurantTable table = tableMapper.selectById(request.getTableId());
        if (table == null) {
            throw new BusinessException("桌台不存在");
        }

        // 先计算总金额和准备订单项
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        
        for (CartItemDTO cartItem : request.getCartItems()) {
            Dish dish = dishMapper.selectById(cartItem.getDishId());
            if (dish == null || dish.getStatus() != 1) {
                throw new BusinessException("菜品不存在或已下架: " + cartItem.getDishName());
            }
            if (dish.getStock() > 0 && dish.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("菜品库存不足: " + dish.getName());
            }

            OrderItem item = new OrderItem();
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setPrice(dish.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setRemark(cartItem.getRemark());
            item.setSubtotal(dish.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            item.setStatus(0); // 待制作
            items.add(item);
            
            totalAmount = totalAmount.add(item.getSubtotal());

            // 扣减库存
            if (dish.getStock() > 0) {
                dish.setStock(dish.getStock() - cartItem.getQuantity());
                dishMapper.updateById(dish);
            }
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setTableId(table.getId());
        order.setTableNo(table.getTableNo());
        order.setCustomerCount(request.getCustomerCount());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setRemark(request.getRemark());

        // 先插入订单，获取ID
        orderMapper.insert(order);

        // 再插入订单项（此时有orderId了）
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 更新桌台状态
        table.setStatus(1); // 使用中
        tableMapper.updateById(table);

        return order;
    }

    @Transactional
    public void addDishToOrder(Long orderId, AddDishRequest request) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() >= 4) {
            throw new BusinessException("订单已完成，无法加菜");
        }

        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null || dish.getStatus() != 1) {
            throw new BusinessException("菜品不存在或已下架");
        }

        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setDishId(dish.getId());
        item.setDishName(dish.getName());
        item.setDishImage(dish.getImage());
        item.setPrice(dish.getPrice());
        item.setQuantity(request.getQuantity());
        item.setRemark(request.getRemark());
        item.setSubtotal(dish.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        item.setStatus(0);

        orderItemMapper.insert(item);

        // 更新订单金额
        order.setTotalAmount(order.getTotalAmount().add(item.getSubtotal()));
        order.setPayAmount(order.getPayAmount().add(item.getSubtotal()));
        orderMapper.updateById(order);

        // 扣减库存
        if (dish.getStock() > 0) {
            dish.setStock(dish.getStock() - request.getQuantity());
            dishMapper.updateById(dish);
        }
    }

    @Transactional
    public void payOrder(Long orderId, PayOrderRequest request) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态错误");
        }

        order.setStatus(1); // 已支付
        order.setPayType(request.getPayType());
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void updateItemStatus(Long itemId, Integer status) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("订单项不存在");
        }

        item.setStatus(status);
        orderItemMapper.updateById(item);

        // 检查是否所有菜品都已完成
        List<OrderItem> items = orderItemMapper.selectByOrderId(item.getOrderId());
        boolean allCompleted = items.stream().allMatch(i -> i.getStatus() == 2);
        
        if (allCompleted) {
            Order order = orderMapper.selectById(item.getOrderId());
            order.setStatus(3); // 待上菜
            orderMapper.updateById(order);
        } else if (status == 1) {
            // 有菜品开始制作，订单状态变为制作中
            Order order = orderMapper.selectById(item.getOrderId());
            if (order.getStatus() == 1) {
                order.setStatus(2); // 制作中
                orderMapper.updateById(order);
            }
        }
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        order.setStatus(4); // 已完成
        orderMapper.updateById(order);

        // 更新桌台状态为待清台
        RestaurantTable table = tableMapper.selectById(order.getTableId());
        if (table != null) {
            table.setStatus(2); // 待清台
            tableMapper.updateById(table);
        }
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() >= 2) {
            throw new BusinessException("订单已开始制作，无法取消");
        }

        order.setStatus(5); // 已取消
        orderMapper.updateById(order);

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null && dish.getStock() >= 0) {
                dish.setStock(dish.getStock() + item.getQuantity());
                dishMapper.updateById(dish);
            }
        }

        // 检查桌台是否还有其他订单，如果没有则恢复空闲
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getTableId, order.getTableId())
               .lt(Order::getStatus, 4);
        if (orderMapper.selectCount(wrapper) == 0) {
            RestaurantTable table = tableMapper.selectById(order.getTableId());
            table.setStatus(0); // 空闲
            tableMapper.updateById(table);
        }
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD" + date + uuid;
    }

    public BigDecimal getTodayRevenue() {
        return orderMapper.selectTodayRevenue();
    }

    public Long getTodayOrderCount() {
        return orderMapper.selectTodayOrderCount();
    }
}
