package fr.corehost.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
            plugin.getFriendManager().setOnline(player.getUniqueId(), true);
            
            // Notify friends that the player has joined
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                Set<String> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
                for (String friendUuidStr : friends) {
                    try {
                        UUID friendUuid = UUID.fromString(friendUuidStr);
                        Optional<Player> onlineFriend = plugin.getServer().getPlayer(friendUuid);
                        onlineFriend.ifPresent(p -> {
                            if (plugin.getFriendManager().areNotificationsEnabled(friendUuid)) {
                                p.sendMessage(ProxyPrefix.get()
                                    .append(Component.text("Votre ami ").color(NamedTextColor.YELLOW))
                                    .append(Component.text(player.getUsername()).color(NamedTextColor.GOLD))
                                    .append(Component.text(" vient de se connecter !").color(NamedTextColor.YELLOW)));
                            }
                        });
                    } catch (Exception ignored) {}
                }
            }).schedule();
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFriendManager() != null) {
            // Update last seen timestamp and set offline status asynchronously
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                plugin.getFriendManager().updateLastSeen(player.getUniqueId());
                plugin.getFriendManager().setOnline(player.getUniqueId(), false);
            }).schedule();
        }
    }
}
