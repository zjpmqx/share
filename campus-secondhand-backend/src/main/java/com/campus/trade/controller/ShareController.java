package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.share.CreateShareRequest;
import com.campus.trade.entity.SharePost;
import com.campus.trade.service.ShareService;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping
    public ApiResponse<List<SharePost>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(shareService.list(page, size));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<SharePost> detail(@PathVariable long id) {
        return ApiResponse.ok(shareService.getById(id));
    }

    @PostMapping
    public ApiResponse<SharePost> create(@Valid @RequestBody CreateShareRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(shareService.create(principal.getUserId(), request));
    }
}
