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
        UUID uuid = player.getUniqueId();
        if (vanish) {
            vanishedPlayers.add(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:vanish:" + uuid.toString(), "true", 86400);
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("staffmod.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
            player.sendMessage(Component.text("Invisibilite (Vanish) active !", NamedTextColor.GREEN));
        } else {
            vanishedPlayers.remove(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:vanish:" + uuid.toString(), "false", 86400);
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            player.sendMessage(Component.text("Invisibilite (Vanish) desactive !", NamedTextColor.RED));
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

        // Par defaut, le vanish est actif pour les membres du staff (sauf si explicitement mis a false)
        if (player.hasPermission("staffmod.mod")) {
            boolean shouldVanish = true;
            if (plugin.getRedisManager() != null) {
                String isV = plugin.getRedisManager().get("corehost:vanish:" + player.getUniqueId().toString());
                if ("false".equals(isV)) {
                    shouldVanish = false;
                }
            }
            if (shouldVanish) {
                setVanished(player, true);
            }
        }
    }

    public void handleQuit(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
    }
}