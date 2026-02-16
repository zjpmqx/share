package com.campus.trade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlineService {

    private final long windowMillis;

    private final ConcurrentHashMap<Long, Long> lastSeenByUserId = new ConcurrentHashMap<>();

    public OnlineService(@Value("${app.online.window-seconds:90}") long windowSeconds) {
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
    }

    public long ping(long userId) {
        long now = System.currentTimeMillis();
        lastSeenByUserId.put(userId, now);
        return count();
    }

    public long count() {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;

        lastSeenByUserId.entrySet().removeIf(e -> {
            Long seen = e.getValue();
            return seen == null || seen < cutoff;
        });

        return lastSeenByUserId.size();
    }
}
