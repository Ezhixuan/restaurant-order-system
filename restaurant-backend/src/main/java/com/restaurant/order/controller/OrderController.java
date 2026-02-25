package com.restaurant.order.controller;

import com.restaurant.common.Result;
import com.restaurant.order.dto.*;
import com.restaurant.order.entity.Order;
import com.restaurant.order.service.OrderService;
import com.restaurant.order.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderStatusService orderStatusService;

    @GetMapping
    public Result<List<Order>> list(@RequestParam(required = false) Integer status) {
        return Result.success(orderService.listOrders(status));
    }

    @GetMapping("/active")
    public Result<List<Order>> listActive() {
        return Result.success(orderService.listActiveOrders());
    }

    @GetMapping("/{id}")
    public Result<OrderDetailDTO> getDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PostMapping
    public Result<Order> create(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @PostMapping("/{orderId}/add")
    public Result<Void> addDish(@PathVariable Long orderId, @RequestBody AddDishRequest request) {
        orderService.addDishToOrder(orderId, request);
        // 加菜后自动更新订单状态
        orderStatusService.updateOrderStatus(orderId);
        return Result.success();
    }

    @PostMapping("/batch-add")
    public Result<Order> batchAddDish(@RequestBody BatchAddDishRequest request) {
        Order order = orderService.batchAddDishToOrder(request);
        // 加菜后自动更新订单状态
        orderStatusService.updateOrderStatus(order.getId());
        return Result.success(order);
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> pay(@PathVariable Long orderId, @RequestBody PayOrderRequest request) {
        // 使用新的结账逻辑，支持部分结账
        orderStatusService.checkout(orderId, request.getPayType(), request.getAmount());
        return Result.success();
    }

    @GetMapping("/{orderId}/unpaid-amount")
    public Result<BigDecimal> getUnpaidAmount(@PathVariable Long orderId) {
        return Result.success(orderStatusService.getUnpaidAmount(orderId));
    }

    @PostMapping("/items/{itemId}/status")
    public Result<Void> updateItemStatus(@PathVariable Long itemId, @RequestParam Integer status) {
        // 使用新的状态更新逻辑，自动更新订单状态
        orderStatusService.updateItemStatus(itemId, status);
        return Result.success();
    }

    @PostMapping("/{orderId}/complete")
    public Result<Void> complete(@PathVariable Long orderId) {
        // 使用新的完成逻辑
        orderStatusService.completeOrder(orderId);
        return Result.success();
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return Result.success();
    }

    @GetMapping("/by-table/{tableId}")
    public Result<OrderDetailDTO> getByTable(@PathVariable Long tableId) {
        return Result.success(orderService.getOrderByTable(tableId));
    }

    @GetMapping("/stats/today-count")
    public Result<Long> getTodayOrderCount() {
        return Result.success(orderService.getTodayOrderCount());
    }
}
