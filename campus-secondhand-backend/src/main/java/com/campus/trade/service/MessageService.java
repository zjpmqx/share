package com.campus.trade.service;

import com.campus.trade.dto.message.CreateMessageRequest;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.ItemMessage;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.ItemMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final ItemMapper itemMapper;
    private final ItemMessageMapper itemMessageMapper;

    public MessageService(ItemMapper itemMapper, ItemMessageMapper itemMessageMapper) {
        this.itemMapper = itemMapper;
        this.itemMessageMapper = itemMessageMapper;
    }

    public ItemMessage postMessage(long fromUserId, long itemId, CreateMessageRequest request) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        ItemMessage message = new ItemMessage();
        message.setItemId(itemId);
        message.setFromUserId(fromUserId);
        message.setContent(request.getContent());
        itemMessageMapper.insert(message);
        return message;
    }

    public List<ItemMessage> listMessages(long itemId) {
        return itemMessageMapper.listByItemId(itemId);
    }
}
