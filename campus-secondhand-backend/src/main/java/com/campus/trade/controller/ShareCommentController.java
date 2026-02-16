package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.share.CreateShareCommentRequest;
import com.campus.trade.entity.ShareComment;
import com.campus.trade.service.ShareService;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares/{shareId}/comments")
public class ShareCommentController {

    private final ShareService shareService;

    public ShareCommentController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping
    public ApiResponse<List<ShareComment>> list(@PathVariable long shareId) {
        return ApiResponse.ok(shareService.listComments(shareId));
    }

    @PostMapping
    public ApiResponse<ShareComment> post(@PathVariable long shareId, @Valid @RequestBody CreateShareCommentRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(shareService.postComment(principal.getUserId(), shareId, request));
    }
}
