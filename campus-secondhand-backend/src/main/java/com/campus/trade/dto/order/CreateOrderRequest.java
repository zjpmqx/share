package com.campus.trade.dto.order;

import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {

    @NotNull
    private Long itemId;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
