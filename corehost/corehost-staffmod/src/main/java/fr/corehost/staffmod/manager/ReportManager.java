package fr.corehost.staffmod.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReportManager {
    
    public static class CachedMessage {
        private final String senderName;
        private final String content;

        public CachedMessage(String senderName, String content) {
            this.senderName = senderName;
            this.content = content;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getContent() {
            return content;
        }
    }

    // Cache messages for 15 minutes, maximum 5000 elements to prevent memory leaks
    private final Cache<UUID, CachedMessage> messageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    public UUID cacheMessage(String senderName, String content) {
        UUID id = UUID.randomUUID();
        messageCache.put(id, new CachedMessage(senderName, content));
        return id;
    }

    public CachedMessage getMessage(UUID id) {
        return messageCache.getIfPresent(id);
    }
}
