package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.AuditItemRequest;
import com.campus.trade.entity.Item;
import com.campus.trade.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/items/pending")
    public ApiResponse<List<Item>> listPending(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.listPendingItems(page, size));
    }

    @GetMapping("/items")
    public ApiResponse<List<Item>> listItems(
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.listItems(status, category, keyword, page, size));
    }

    @PostMapping("/items/{id}/audit")
    public ApiResponse<Void> audit(@PathVariable long id, @Valid @RequestBody AuditItemRequest request) {
        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            adminService.approveItem(id);
            return ApiResponse.ok(null);
        }
        if ("REJECT".equalsIgnoreCase(action)) {
            adminService.rejectItem(id, request.getReason());
            return ApiResponse.ok(null);
        }
        throw new IllegalArgumentException("Unknown action");
    }

    @PostMapping("/items/{id}/off-shelf")
    public ApiResponse<Item> offShelfItem(@PathVariable long id) {
        return ApiResponse.ok(adminService.offShelfItem(id));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable long id) {
        adminService.deleteItem(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/items/off-shelf-all")
    public ApiResponse<Integer> offShelfAll(@RequestParam String confirm) {
        return ApiResponse.ok(adminService.offShelfAllOnSaleItems(confirm));
    }

    @DeleteMapping("/items")
    public ApiResponse<Integer> deleteAll(@RequestParam String confirm) {
        return ApiResponse.ok(adminService.deleteAllItems(confirm));
    }
}
