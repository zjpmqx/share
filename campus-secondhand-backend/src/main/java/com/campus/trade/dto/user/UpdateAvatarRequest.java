package com.campus.trade.dto.user;

import jakarta.validation.constraints.NotBlank;

public class UpdateAvatarRequest {

    @NotBlank
    private String avatarUrl;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
