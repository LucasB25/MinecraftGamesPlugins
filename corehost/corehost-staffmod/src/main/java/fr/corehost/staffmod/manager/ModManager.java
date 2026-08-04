package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModManager {

    private final StaffModPlugin plugin;
    private final Set<UUID> modPlayers = new HashSet<>();

    public ModManager(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isModMode(UUID uuid) {
        return modPlayers.contains(uuid);
    }

    public void setModMode(Player player, boolean mod) {
        UUID uuid = player.getUniqueId();
        if (mod) {
            modPlayers.add(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "true", 86400);
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(0.2f);
            
            player.sendMessage(plugin.getPrefix().append(Component.text("Mode Moderation active !", NamedTextColor.GREEN)));
        } else {
            modPlayers.remove(uuid);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "false", 86400);
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);
            
            player.sendMessage(plugin.getPrefix().append(Component.text("Mode Moderation desactive !", NamedTextColor.RED)));
        }
    }

    public void handleJoin(Player player) {
        if (player.hasPermission("staffmod.mod")) {
            boolean shouldMod = true;
            if (plugin.getRedisManager() != null) {
                String isM = plugin.getRedisManager().get("corehost:modmode:" + player.getUniqueId().toString());
                if ("false".equals(isM)) {
                    shouldMod = false;
                }
            }
            if (shouldMod) {
                setModMode(player, true);
            }
        }
    }

    public void handleQuit(Player player) {
        modPlayers.remove(player.getUniqueId());
    }
}
