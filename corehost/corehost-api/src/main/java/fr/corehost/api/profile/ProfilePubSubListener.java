package fr.corehost.api.profile;

import redis.clients.jedis.JedisPubSub;
import java.util.UUID;
import java.util.logging.Logger;

public class ProfilePubSubListener extends JedisPubSub {

    private final ProfileManager profileManager;
    private final Logger logger;

    public ProfilePubSubListener(ProfileManager profileManager, Logger logger) {
        this.profileManager = profileManager;
        this.logger = logger;
    }

    @Override
    public void onMessage(String channel, String message) {
        if ("corehost:profile:update".equals(channel)) {
            try {
                UUID uuid = UUID.fromString(message);
                profileManager.invalidateProfile(uuid);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid UUID received on profile update channel: " + message);
            }
        }
    }
}
