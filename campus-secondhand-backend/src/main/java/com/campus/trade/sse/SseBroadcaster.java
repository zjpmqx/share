package com.campus.trade.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseBroadcaster {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"ts\":" + System.currentTimeMillis() + "}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastItemApproved(long itemId) {
        String payload = "{\"type\":\"ITEM_APPROVED\",\"itemId\":" + itemId + ",\"ts\":" + System.currentTimeMillis() + "}";

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("item-approved").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
