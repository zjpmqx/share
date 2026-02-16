package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.user.UpdateAvatarRequest;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PutMapping("/avatar")
    public ApiResponse<Void> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }

        String avatarUrl = request.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("avatarUrl is required");
        }

        userMapper.updateAvatar(principal.getUserId(), avatarUrl);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<User> getPublicProfile(@PathVariable long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        User publicUser = new User();
        publicUser.setId(user.getId());
        publicUser.setUsername(user.getUsername());
        publicUser.setAvatarUrl(user.getAvatarUrl());
        publicUser.setRole(user.getRole());
        return ApiResponse.ok(publicUser);
    }
}
