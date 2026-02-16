package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.order.CreateOrderRequest;
import com.campus.trade.entity.Order;
import com.campus.trade.service.OrderService;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.createOrder(principal.getUserId(), request.getItemId()));
    }

    @GetMapping("/my/buy")
    public ApiResponse<List<Order>> myBuyOrders() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.listMyBuyOrders(principal.getUserId()));
    }

    @GetMapping("/my/sell")
    public ApiResponse<List<Order>> mySellOrders() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.listMySellOrders(principal.getUserId()));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<Order> pay(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.pay(principal.getUserId(), id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Order> ship(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.ship(principal.getUserId(), id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Order> confirm(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.confirmReceipt(principal.getUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(orderService.cancel(principal.getUserId(), id));
    }
}
