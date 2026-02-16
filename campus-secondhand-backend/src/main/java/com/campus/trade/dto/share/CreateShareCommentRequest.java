package com.campus.trade.dto.share;

import jakarta.validation.constraints.NotBlank;

public class CreateShareCommentRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
