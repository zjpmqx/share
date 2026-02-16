package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.AdminUserResponse;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserMapper userMapper;

    public AdminUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        List<User> users = userMapper.listUsers(limit, offset);
        List<AdminUserResponse> out = new ArrayList<>();
        for (User u : users) {
            AdminUserResponse r = new AdminUserResponse();
            r.setId(u.getId());
            r.setUsername(u.getUsername());
            r.setPhone(u.getPhone());
            r.setRole(u.getRole());
            r.setAvatarUrl(u.getAvatarUrl());
            r.setStatus(u.getStatus());
            r.setLastLoginIp(u.getLastLoginIp());
            r.setLastLoginAt(u.getLastLoginAt());
            r.setCreatedAt(u.getCreatedAt());
            out.add(r);
        }
        return ApiResponse.ok(out);
    }
}
