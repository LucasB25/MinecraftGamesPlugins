package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            updateVanishItem(player, true);
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
            updateVanishItem(player, false);
            if (notify && player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Invisibilité (Vanish) désactivée !", NamedTextColor.RED)));
            }
        }
    }

    private void updateVanishItem(Player player, boolean vanish) {
        if (plugin.getModManager() != null && plugin.getModManager().isModMode(player.getUniqueId())) {
            ItemStack item = player.getInventory().getItem(7);
            if (item != null && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("Vanish : " + (vanish ? "ON" : "OFF"), vanish ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD));
                    item.setItemMeta(meta);
                    item.setType(vanish ? Material.LIME_DYE : Material.GRAY_DYE);
                }
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
            boolean shouldBeVanished = false;
            if (plugin.getRedisManager() != null) {
                String val = plugin.getRedisManager().get("corehost:vanish:" + player.getUniqueId().toString());
                if ("true".equals(val)) {
                    shouldBeVanished = true;
                }
            }
            
            if (shouldBeVanished) {
                setVanished(player, true, false);
            } else {
                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().setEx("corehost:vanish:" + player.getUniqueId().toString(), "false", 86400);
                }
                if (isVanished(player.getUniqueId())) {
                    setVanished(player, false, false);
                }
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