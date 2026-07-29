package fr.corehost.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import fr.corehost.proxy.CoreHostProxy;

public class PlayerConnectionListener {

    private final CoreHostProxy plugin;

    public PlayerConnectionListener(CoreHostProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFriendManager() != null) {
            // Cache player name/UUID for the friends system
            plugin.getFriendManager().cachePlayer(player.getUsername(), player.getUniqueId());
        }
    }
}
