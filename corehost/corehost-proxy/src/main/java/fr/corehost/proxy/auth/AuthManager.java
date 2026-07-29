package fr.corehost.proxy.auth;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.EventTask;
import fr.corehost.proxy.CoreHostProxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AuthManager {

    private final CoreHostProxy plugin;
    private final HttpClient httpClient;

    public AuthManager(CoreHostProxy plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Checks if a username is a Premium account according to Mojang.
     * Caches the result in Redis for 24 hours to prevent rate limiting.
     */
    public CompletableFuture<Boolean> isPremium(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = "corehost:auth:premium_status:" + username.toLowerCase();
            
            // 1. Check Cache
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                String cached = plugin.getRedisManager().get(cacheKey);
                if (cached != null) {
                    return Boolean.parseBoolean(cached);
                }
            }

            // 2. Fetch Mojang API
            boolean isPremium = false;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // HTTP 200 OK means the account exists (Premium)
                // HTTP 204 No Content or 404 means it doesn't exist (Cracked)
                if (response.statusCode() == 200) {
                    isPremium = true;
                }
            } catch (Exception e) {
                plugin.getLogger().error("Failed to fetch Mojang API for " + username, e);
                // On error, fallback to false (crack) to let player join, 
                // but we don't cache it so it tries again next time.
                return false;
            }

            // 3. Save to Cache (Expire after 24 hours = 86400 seconds)
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                plugin.getRedisManager().setEx(cacheKey, String.valueOf(isPremium), 86400);
            }

            return isPremium;
        });
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();

        return EventTask.async(() -> {
            try {
                boolean premium = isPremium(username).join();
                if (premium) {
                    event.setResult(PreLoginComponentResult.forceOnlineMode());
                } else {
                    event.setResult(PreLoginComponentResult.forceOfflineMode());
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error determining auth mode for " + username, e);
                event.setResult(PreLoginComponentResult.forceOfflineMode());
            }
        });
    }
}
