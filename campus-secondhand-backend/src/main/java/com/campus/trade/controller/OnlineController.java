package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.OnlineService;
import com.campus.trade.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/online")
public class OnlineController {

    private final OnlineService onlineService;

    public OnlineController(OnlineService onlineService) {
        this.onlineService = onlineService;
    }

    @PostMapping("/ping")
    public ApiResponse<Long> ping() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(onlineService.ping(principal.getUserId()));
    }

    @GetMapping("/count")
    public ApiResponse<Long> count() {
        return ApiResponse.ok(onlineService.count());
    }
}
