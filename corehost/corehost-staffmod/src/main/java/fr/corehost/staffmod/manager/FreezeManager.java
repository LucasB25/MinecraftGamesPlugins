package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {

    private final StaffModPlugin plugin;
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public FreezeManager(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void setFrozen(Player player, boolean freeze) {
        UUID uuid = player.getUniqueId();
        if (freeze) {
            frozenPlayers.add(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:freeze:" + uuid.toString(), "true", 86400);
            }
            player.sendMessage(Component.text("Vous avez ete gele par un moderateur !", NamedTextColor.RED));
        } else {
            frozenPlayers.remove(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().del("corehost:freeze:" + uuid.toString());
            }
            player.sendMessage(Component.text("Vous avez ete degele.", NamedTextColor.GREEN));
        }
    }

    public void toggleFreeze(Player player) {
        setFrozen(player, !isFrozen(player.getUniqueId()));
    }

    public void handleJoin(Player player) {
        if (plugin.getRedisManager() != null) {
            String isF = plugin.getRedisManager().get("corehost:freeze:" + player.getUniqueId().toString());
            if ("true".equals(isF)) {
                frozenPlayers.add(player.getUniqueId());
                player.sendMessage(Component.text("Vous etes toujours gele !", NamedTextColor.RED));
            }
        }
    }

    public void handleQuit(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }
}