package com.campus.trade.dto.message;

import jakarta.validation.constraints.NotBlank;

public class CreateMessageRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
