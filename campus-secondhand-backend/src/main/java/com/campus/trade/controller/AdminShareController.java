package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.AdminShareService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shares")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShareController {

    private final AdminShareService adminShareService;

    public AdminShareController(AdminShareService adminShareService) {
        this.adminShareService = adminShareService;
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        adminShareService.deleteShare(id);
        return ApiResponse.ok(null);
    }
}
