package com.campus.trade.service;

import com.campus.trade.entity.Item;
import com.campus.trade.mapper.ItemImageMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.ItemMessageMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.sse.SseBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ItemMessageMapper itemMessageMapper;
    private final OrderMapper orderMapper;
    private final UploadStorageService uploadStorageService;
    private final SseBroadcaster sseBroadcaster;

    public AdminService(
            ItemMapper itemMapper,
            ItemImageMapper itemImageMapper,
            ItemMessageMapper itemMessageMapper,
            OrderMapper orderMapper,
            UploadStorageService uploadStorageService,
            SseBroadcaster sseBroadcaster
    ) {
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.itemMessageMapper = itemMessageMapper;
        this.orderMapper = orderMapper;
        this.uploadStorageService = uploadStorageService;
        this.sseBroadcaster = sseBroadcaster;
    }

    public List<Item> listPendingItems(int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        return itemMapper.list(ItemService.ITEM_STATUS_PENDING_REVIEW, null, null, limit, offset);
    }

    public List<Item> listItems(String status, String category, String keyword, int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        String s = (status == null || status.isBlank()) ? null : status;
        return itemMapper.list(s, category, keyword, limit, offset);
    }

    public void approveItem(long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (!ItemService.ITEM_STATUS_PENDING_REVIEW.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item is not pending review");
        }
        itemMapper.updateAudit(itemId, ItemService.ITEM_STATUS_ON_SALE, null);
        sseBroadcaster.broadcastItemApproved(itemId);
    }

    public void rejectItem(long itemId, String reason) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (!ItemService.ITEM_STATUS_PENDING_REVIEW.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item is not pending review");
        }
        itemMapper.updateAudit(itemId, ItemService.ITEM_STATUS_REJECTED, reason);
    }

    public Item offShelfItem(long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (!ItemService.ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item cannot be off-shelved in current status");
        }
        itemMapper.updateStatus(itemId, ItemService.ITEM_STATUS_OFF_SHELF);
        return itemMapper.findById(itemId);
    }

    @Transactional
    public void deleteItem(long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            return;
        }

        List<String> imageUrls = itemImageMapper.listUrlsByItemId(itemId);
        itemMessageMapper.deleteByItemId(itemId);
        orderMapper.deleteByItemId(itemId);
        itemImageMapper.deleteByItemId(itemId);
        itemMapper.deleteById(itemId);

        uploadStorageService.deleteLocalFileByUrl(item.getCoverImageUrl());
        for (String url : imageUrls) {
            uploadStorageService.deleteLocalFileByUrl(url);
        }
    }

    public int offShelfAllOnSaleItems(String confirm) {
        if (!"OFF_SHELF_ALL".equals(confirm)) {
            throw new IllegalArgumentException("Confirm required");
        }
        return itemMapper.updateStatusByStatus(ItemService.ITEM_STATUS_ON_SALE, ItemService.ITEM_STATUS_OFF_SHELF);
    }

    @Transactional
    public int deleteAllItems(String confirm) {
        if (!"DELETE_ALL".equals(confirm)) {
            throw new IllegalArgumentException("Confirm required");
        }

        List<String> coverUrls = itemMapper.listAllCoverUrls();
        List<String> imageUrls = itemImageMapper.listAllUrls();

        itemMessageMapper.deleteAll();
        orderMapper.deleteAll();
        itemImageMapper.deleteAll();
        int deleted = itemMapper.deleteAll();

        for (String url : coverUrls) {
            uploadStorageService.deleteLocalFileByUrl(url);
        }
        for (String url : imageUrls) {
            uploadStorageService.deleteLocalFileByUrl(url);
        }

        return deleted;
    }
}
