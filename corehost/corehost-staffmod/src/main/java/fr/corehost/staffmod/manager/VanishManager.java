package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final StaffModPlugin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishManager(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public void setVanished(Player player, boolean vanish) {
        setVanished(player, vanish, true);
    }

    public void setVanished(Player player, boolean vanish, boolean notify) {
        UUID uuid = player.getUniqueId();
        if (vanish) {
            if (isVanished(uuid)) return;
            vanishedPlayers.add(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:vanish:" + uuid.toString(), "true", 86400);
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("staffmod.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
            if (notify && player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Invisibilité (Vanish) activée !", NamedTextColor.GREEN)));
            }
        } else {
            if (!isVanished(uuid)) return;
            vanishedPlayers.remove(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:vanish:" + uuid.toString(), "false", 86400);
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            if (notify && player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Invisibilité (Vanish) désactivée !", NamedTextColor.RED)));
            }
        }
    }

    public void toggleVanish(Player player) {
        setVanished(player, !isVanished(player.getUniqueId()));
    }

    public void handleJoin(Player player) {
        // Si un autre joueur rejoint, cacher les staff vanish
        for (UUID vId : vanishedPlayers) {
            Player vPlayer = Bukkit.getPlayer(vId);
            if (vPlayer != null && !player.hasPermission("staffmod.vanish.see")) {
                player.hidePlayer(plugin, vPlayer);
            }
        }

        if (player.hasPermission("staffmod.mod")) {
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:vanish:" + player.getUniqueId().toString(), "false", 86400);
            }
            if (isVanished(player.getUniqueId())) {
                setVanished(player, false, false);
            }
        }
    }

    public void handleQuit(Player player) {
        if (isVanished(player.getUniqueId())) {
            setVanished(player, false, false);
        } else {
            vanishedPlayers.remove(player.getUniqueId());
        }
    }
}