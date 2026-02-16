package com.campus.trade.service;

import com.campus.trade.entity.Item;
import com.campus.trade.entity.Order;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    public static final String ORDER_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String ORDER_STATUS_PENDING_SHIPMENT = "PENDING_SHIPMENT";
    public static final String ORDER_STATUS_PENDING_RECEIPT = "PENDING_RECEIPT";
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String ORDER_STATUS_CANCELED = "CANCELED";

    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;

    public OrderService(OrderMapper orderMapper, ItemMapper itemMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public Order createOrder(long buyerId, long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (!ItemService.ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item not for sale");
        }
        if (item.getSellerId() != null && item.getSellerId() == buyerId) {
            throw new IllegalArgumentException("Cannot buy your own item");
        }

        itemMapper.updateStatus(itemId, ItemService.ITEM_STATUS_LOCKED);

        Order order = new Order();
        order.setItemId(itemId);
        order.setBuyerId(buyerId);
        order.setSellerId(item.getSellerId());
        order.setAmount(item.getPrice());
        order.setStatus(ORDER_STATUS_PENDING_PAYMENT);
        orderMapper.insert(order);
        return orderMapper.findById(order.getId());
    }

    public List<Order> listMyBuyOrders(long buyerId) {
        return orderMapper.listByBuyerId(buyerId);
    }

    public List<Order> listMySellOrders(long sellerId) {
        return orderMapper.listBySellerId(sellerId);
    }

    @Transactional
    public Order pay(long buyerId, long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getBuyerId() == null || order.getBuyerId() != buyerId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ORDER_STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending payment");
        }
        orderMapper.updateStatus(orderId, ORDER_STATUS_PENDING_SHIPMENT);
        return orderMapper.findById(orderId);
    }

    @Transactional
    public Order ship(long sellerId, long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getSellerId() == null || order.getSellerId() != sellerId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ORDER_STATUS_PENDING_SHIPMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending shipment");
        }
        orderMapper.updateStatus(orderId, ORDER_STATUS_PENDING_RECEIPT);
        return orderMapper.findById(orderId);
    }

    @Transactional
    public Order confirmReceipt(long buyerId, long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getBuyerId() == null || order.getBuyerId() != buyerId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ORDER_STATUS_PENDING_RECEIPT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending receipt");
        }
        orderMapper.updateStatus(orderId, ORDER_STATUS_COMPLETED);
        itemMapper.updateStatus(order.getItemId(), ItemService.ITEM_STATUS_SOLD);
        return orderMapper.findById(orderId);
    }

    @Transactional
    public Order cancel(long buyerId, long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getBuyerId() == null || order.getBuyerId() != buyerId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ORDER_STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Only pending payment orders can be canceled");
        }
        orderMapper.updateStatus(orderId, ORDER_STATUS_CANCELED);
        itemMapper.updateStatus(order.getItemId(), ItemService.ITEM_STATUS_ON_SALE);
        return orderMapper.findById(orderId);
    }
}
