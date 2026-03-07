package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.message.CreateMessageRequest;
import com.campus.trade.entity.ItemMessage;
import com.campus.trade.service.MessageService;
import com.campus.trade.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items/{itemId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<ItemMessage>> list(@PathVariable long itemId) {
        var principal = SecurityUtils.currentUser();
        Long userId = principal == null ? null : principal.getUserId();
        return ApiResponse.ok(messageService.listMessages(userId, itemId));
    }

    @PostMapping
    public ApiResponse<ItemMessage> post(@PathVariable long itemId, @Valid @RequestBody CreateMessageRequest request) {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        return ApiResponse.ok(messageService.postMessage(principal.getUserId(), itemId, request));
    }
}
