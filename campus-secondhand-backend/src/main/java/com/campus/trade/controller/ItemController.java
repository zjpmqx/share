package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.item.CreateItemRequest;
import com.campus.trade.dto.item.UpdateItemRequest;
import com.campus.trade.entity.Item;
import com.campus.trade.service.ItemService;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ApiResponse<List<Item>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ON_SALE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // B flow: public list only shows ON_SALE items
        String safeStatus = ItemService.ITEM_STATUS_ON_SALE;
        return ApiResponse.ok(itemService.list(safeStatus, category, keyword, page, size));
    }

    @GetMapping("/my")
    public ApiResponse<List<Item>> myItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(itemService.listMy(principal.getUserId(), status, category, keyword, page, size));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<Item> detail(@PathVariable long id) {
        return ApiResponse.ok(itemService.getById(id));
    }

    @PostMapping
    public ApiResponse<Item> create(@Valid @RequestBody CreateItemRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(itemService.create(principal.getUserId(), request));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<Item> update(@PathVariable long id, @Valid @RequestBody UpdateItemRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(itemService.update(principal.getUserId(), id, request));
    }

    @PostMapping("/{id:\\d+}/off-shelf")
    public ApiResponse<Item> offShelf(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(itemService.offShelf(principal.getUserId(), id));
    }

    @PostMapping("/{id:\\d+}/on-shelf")
    public ApiResponse<Item> onShelf(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(itemService.onShelf(principal.getUserId(), id));
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        itemService.delete(principal.getUserId(), id);
        return ApiResponse.ok(null);
    }
}
