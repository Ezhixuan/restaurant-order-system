package com.restaurant.order.controller;

import com.restaurant.common.Result;
import com.restaurant.order.dto.*;
import com.restaurant.order.entity.Order;
import com.restaurant.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
        return Result.success();
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> pay(@PathVariable Long orderId, @RequestBody PayOrderRequest request) {
        orderService.payOrder(orderId, request);
        return Result.success();
    }

    @PostMapping("/items/{itemId}/status")
    public Result<Void> updateItemStatus(@PathVariable Long itemId, @RequestParam Integer status) {
        orderService.updateItemStatus(itemId, status);
        return Result.success();
    }

    @PostMapping("/{orderId}/complete")
    public Result<Void> complete(@PathVariable Long orderId) {
        orderService.completeOrder(orderId);
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
