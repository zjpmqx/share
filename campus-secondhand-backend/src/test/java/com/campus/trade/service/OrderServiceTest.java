package com.campus.trade.service;

import com.campus.trade.entity.Item;
import com.campus.trade.entity.Order;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    @Test
    void createOrderShouldLockItemBeforeInsert() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Item item = new Item();
        item.setId(1L);
        item.setSellerId(2L);
        item.setPrice(new BigDecimal("88.00"));
        item.setStatus(ItemService.ITEM_STATUS_ON_SALE);
        itemMapper.item = item;
        orderMapper.findByIdResult = new Order();
        orderMapper.findByIdResult.setId(99L);
        orderMapper.findByIdResult.setItemId(1L);
        orderMapper.findByIdResult.setBuyerId(3L);
        orderMapper.findByIdResult.setSellerId(2L);
        orderMapper.findByIdResult.setAmount(new BigDecimal("88.00"));
        orderMapper.findByIdResult.setStatus(OrderService.ORDER_STATUS_PENDING_PAYMENT);

        Order order = orderService.createOrder(3L, 1L);

        assertEquals(1, itemMapper.updateStatusIfCurrentCount);
        assertEquals(ItemService.ITEM_STATUS_ON_SALE, itemMapper.lastFromStatus);
        assertEquals(ItemService.ITEM_STATUS_LOCKED, itemMapper.lastToStatus);
        assertNotNull(order);
        assertEquals(99L, order.getId());
    }

    @Test
    void createOrderShouldRejectWhenConditionalLockFails() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Item item = new Item();
        item.setId(1L);
        item.setSellerId(2L);
        item.setPrice(new BigDecimal("88.00"));
        item.setStatus(ItemService.ITEM_STATUS_ON_SALE);
        itemMapper.item = item;
        itemMapper.updateStatusIfCurrentResult = 0;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(3L, 1L));

        assertEquals("Item not for sale", ex.getMessage());
        assertEquals(0, orderMapper.insertCount);
    }

    @Test
    void createOrderShouldTranslateDuplicateKey() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Item item = new Item();
        item.setId(1L);
        item.setSellerId(2L);
        item.setPrice(new BigDecimal("88.00"));
        item.setStatus(ItemService.ITEM_STATUS_ON_SALE);
        itemMapper.item = item;
        orderMapper.throwDuplicateKey = true;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(3L, 1L));

        assertEquals("Item already ordered", ex.getMessage());
        assertEquals(1, orderMapper.insertCount);
    }

    @Test
    void payShouldMoveOrderToPendingShipment() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Order order = new Order();
        order.setId(11L);
        order.setBuyerId(3L);
        order.setSellerId(2L);
        order.setStatus(OrderService.ORDER_STATUS_PENDING_PAYMENT);
        orderMapper.findByIdResult = order;

        orderService.pay(3L, 11L);

        assertEquals(1, orderMapper.updateStatusIfCurrentCount);
        assertEquals(OrderService.ORDER_STATUS_PENDING_PAYMENT, orderMapper.lastFromStatus);
        assertEquals(OrderService.ORDER_STATUS_PENDING_SHIPMENT, orderMapper.lastToStatus);
    }

    @Test
    void shipShouldMoveOrderToPendingReceipt() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Order order = new Order();
        order.setId(12L);
        order.setBuyerId(3L);
        order.setSellerId(2L);
        order.setStatus(OrderService.ORDER_STATUS_PENDING_SHIPMENT);
        orderMapper.findByIdResult = order;

        orderService.ship(2L, 12L);

        assertEquals(1, orderMapper.updateStatusIfCurrentCount);
        assertEquals(OrderService.ORDER_STATUS_PENDING_SHIPMENT, orderMapper.lastFromStatus);
        assertEquals(OrderService.ORDER_STATUS_PENDING_RECEIPT, orderMapper.lastToStatus);
    }

    @Test
    void confirmReceiptShouldCompleteOrderAndMarkItemSold() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Order order = new Order();
        order.setId(13L);
        order.setItemId(1L);
        order.setBuyerId(3L);
        order.setSellerId(2L);
        order.setStatus(OrderService.ORDER_STATUS_PENDING_RECEIPT);
        orderMapper.findByIdResult = order;

        orderService.confirmReceipt(3L, 13L);

        assertEquals(1, orderMapper.updateStatusIfCurrentCount);
        assertEquals(OrderService.ORDER_STATUS_PENDING_RECEIPT, orderMapper.lastFromStatus);
        assertEquals(OrderService.ORDER_STATUS_COMPLETED, orderMapper.lastToStatus);
        assertEquals(1L, itemMapper.lastUpdateStatusItemId);
        assertEquals(ItemService.ITEM_STATUS_LOCKED, itemMapper.lastFromStatus);
        assertEquals(ItemService.ITEM_STATUS_SOLD, itemMapper.lastToStatus);
    }

    @Test
    void cancelShouldCancelOrderAndRestoreItemOnSale() {
        FakeItemMapper itemMapper = new FakeItemMapper();
        FakeOrderMapper orderMapper = new FakeOrderMapper();
        OrderService orderService = new OrderService(orderMapper, itemMapper);

        Order order = new Order();
        order.setId(14L);
        order.setItemId(1L);
        order.setBuyerId(3L);
        order.setSellerId(2L);
        order.setStatus(OrderService.ORDER_STATUS_PENDING_PAYMENT);
        orderMapper.findByIdResult = order;

        orderService.cancel(3L, 14L);

        assertEquals(1, orderMapper.updateStatusIfCurrentCount);
        assertEquals(OrderService.ORDER_STATUS_PENDING_PAYMENT, orderMapper.lastFromStatus);
        assertEquals(OrderService.ORDER_STATUS_CANCELED, orderMapper.lastToStatus);
        assertEquals(1L, itemMapper.lastUpdateStatusItemId);
        assertEquals(ItemService.ITEM_STATUS_LOCKED, itemMapper.lastFromStatus);
        assertEquals(ItemService.ITEM_STATUS_ON_SALE, itemMapper.lastToStatus);
    }

    private static class FakeItemMapper implements ItemMapper {
        private Item item;
        private int updateStatusIfCurrentResult = 1;
        private int updateStatusIfCurrentCount;
        private Long lastUpdateStatusItemId;
        private String lastFromStatus;
        private String lastToStatus;

        @Override
        public Item findById(Long id) {
            return item;
        }

        @Override
        public int updateStatusIfCurrent(Long id, String fromStatus, String toStatus) {
            updateStatusIfCurrentCount++;
            lastUpdateStatusItemId = id;
            lastFromStatus = fromStatus;
            lastToStatus = toStatus;
            return updateStatusIfCurrentResult;
        }

        @Override
        public int insert(Item item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(Item item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<String> listAllCoverUrls() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatusByStatus(String fromStatus, String toStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateAudit(Long id, String status, String auditReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(Long id, String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Item> list(String status, String category, String keyword, int limit, int offset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Item> listBySellerId(long sellerId, String status, String category, String keyword, int limit, int offset) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeOrderMapper implements OrderMapper {
        private boolean throwDuplicateKey;
        private int insertCount;
        private int updateStatusIfCurrentCount;
        private String lastFromStatus;
        private String lastToStatus;
        private Order findByIdResult;

        @Override
        public int insert(Order order) {
            insertCount++;
            if (throwDuplicateKey) {
                throw new DuplicateKeyException("duplicate");
            }
            order.setId(99L);
            return 1;
        }

        @Override
        public Order findById(Long id) {
            return findByIdResult;
        }

        @Override
        public java.util.List<Order> listByBuyerId(Long buyerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Order> listBySellerId(Long sellerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(Long id, String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatusIfCurrent(Long id, String fromStatus, String toStatus) {
            updateStatusIfCurrentCount++;
            lastFromStatus = fromStatus;
            lastToStatus = toStatus;
            return 1;
        }

        @Override
        public int deleteByItemId(Long itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteAll() {
            throw new UnsupportedOperationException();
        }
    }
}
