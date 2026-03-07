package com.campus.trade.service;

import com.campus.trade.dto.item.CreateItemRequest;
import com.campus.trade.dto.item.UpdateItemRequest;
import com.campus.trade.entity.Item;
import com.campus.trade.mapper.ItemImageMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.ItemMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    public static final String ITEM_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String ITEM_STATUS_ON_SALE = "ON_SALE";
    public static final String ITEM_STATUS_OFF_SHELF = "OFF_SHELF";
    public static final String ITEM_STATUS_LOCKED = "LOCKED";
    public static final String ITEM_STATUS_SOLD = "SOLD";
    public static final String ITEM_STATUS_REJECTED = "REJECTED";

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ItemMessageMapper itemMessageMapper;
    private final UploadStorageService uploadStorageService;

    public ItemService(
            ItemMapper itemMapper,
            ItemImageMapper itemImageMapper,
            ItemMessageMapper itemMessageMapper,
            UploadStorageService uploadStorageService
    ) {
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.itemMessageMapper = itemMessageMapper;
        this.uploadStorageService = uploadStorageService;
    }

    public Item create(long sellerId, CreateItemRequest request) {
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setConditionLevel(request.getConditionLevel());
        item.setCoverImageUrl(request.getCoverImageUrl());
        item.setStatus(ITEM_STATUS_PENDING_REVIEW);
        itemMapper.insert(item);
        return itemMapper.findById(item.getId());
    }

    public Item update(long userId, long itemId, UpdateItemRequest request) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (item.getSellerId() == null || item.getSellerId() != userId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ITEM_STATUS_PENDING_REVIEW.equals(item.getStatus()) &&
                !ITEM_STATUS_REJECTED.equals(item.getStatus()) &&
                !ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item cannot be edited in current status");
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setConditionLevel(request.getConditionLevel());
        item.setCoverImageUrl(request.getCoverImageUrl());
        if (ITEM_STATUS_REJECTED.equals(item.getStatus())) {
            item.setStatus(ITEM_STATUS_PENDING_REVIEW);
            item.setAuditReason(null);
        }
        itemMapper.update(item);
        return itemMapper.findById(itemId);
    }

    @Transactional
    public void delete(long userId, long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            return;
        }
        if (item.getSellerId() == null || item.getSellerId() != userId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ITEM_STATUS_PENDING_REVIEW.equals(item.getStatus()) &&
                !ITEM_STATUS_REJECTED.equals(item.getStatus()) &&
                !ITEM_STATUS_ON_SALE.equals(item.getStatus()) &&
                !ITEM_STATUS_OFF_SHELF.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item cannot be deleted in current status");
        }
        List<String> imageUrls = itemImageMapper.listUrlsByItemId(itemId);
        itemMessageMapper.deleteByItemId(itemId);
        itemImageMapper.deleteByItemId(itemId);
        itemMapper.deleteById(itemId);

        uploadStorageService.deleteLocalFileByUrl(item.getCoverImageUrl());
        for (String url : imageUrls) {
            uploadStorageService.deleteLocalFileByUrl(url);
        }
    }

    public Item offShelf(long userId, long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (item.getSellerId() == null || item.getSellerId() != userId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item cannot be off-shelved in current status");
        }
        itemMapper.updateStatus(itemId, ITEM_STATUS_OFF_SHELF);
        return itemMapper.findById(itemId);
    }

    public Item onShelf(long userId, long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (item.getSellerId() == null || item.getSellerId() != userId) {
            throw new IllegalArgumentException("No permission");
        }
        if (!ITEM_STATUS_OFF_SHELF.equals(item.getStatus())) {
            throw new IllegalArgumentException("Item cannot be on-shelved in current status");
        }
        itemMapper.updateStatus(itemId, ITEM_STATUS_ON_SALE);
        return itemMapper.findById(itemId);
    }

    public Item getById(Long userId, long id) {
        Item item = itemMapper.findById(id);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            return item;
        }
        if (userId != null && item.getSellerId() != null && item.getSellerId().equals(userId)) {
            return item;
        }
        throw new IllegalArgumentException("No permission");
    }

    public List<Item> list(String status, String category, String keyword, int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        return itemMapper.list(status, category, keyword, limit, offset);
    }

    public List<Item> listMy(long sellerId, String status, String category, String keyword, int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        return itemMapper.listBySellerId(sellerId, status, category, keyword, limit, offset);
    }
}
