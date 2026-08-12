package fr.corehost.lobby.listeners;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class ProfileLoadListener implements Listener {

    private final CoreHostLobby plugin;

    public ProfileLoadListener(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        
        // Asynchronously load the profile from Redis/Database into the Caffeine cache
        // before the player physically joins the server.
        // This prevents the main thread from lagging during the PlayerJoinEvent or Scoreboard updates.
        if (plugin.getProfileManager() != null) {
            plugin.getProfileManager().getProfile(event.getUniqueId()).join();
        }
    }
}
