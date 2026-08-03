package fr.corehost.lobby.parkour;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import fr.corehost.lobby.utils.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {

    private final ParkourManager parkourManager;
    private final Map<UUID, Long> interactCooldown;

    public ParkourListener(ParkourManager parkourManager) {
        this.parkourManager = parkourManager;
        this.interactCooldown = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getAction().name().contains("RIGHT")) {
            if (event.getItem() != null) {
                if (event.getItem().getType() == Material.RED_BED) {
                    if (parkourManager.isInParkour(player)) {
                        parkourManager.returnToStart(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                        event.setCancelled(true);
                    }
                } else if (event.getItem().getType() == Material.OAK_DOOR) {
                    if (parkourManager.isInParkour(player)) {
                        parkourManager.cancelParkour(player);
                        org.bukkit.Location spawn = player.getWorld().getSpawnLocation().clone();
                        spawn.setX(spawn.getBlockX() + 0.5);
                        spawn.setZ(spawn.getBlockZ() + 0.5);
                        spawn.setYaw(spawn.getYaw() + 180f);
                        player.teleport(spawn);
                        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0f, 1.0f);
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        
        // Cooldown de 2 secondes
        long currentTime = System.currentTimeMillis();
        if (interactCooldown.containsKey(player.getUniqueId())) {
            long lastInteract = interactCooldown.get(player.getUniqueId());
            if (currentTime - lastInteract < 2000) {
                return;
            }
        }

        // Start plate
        if (parkourManager.isStartPlate(block.getLocation())) {
            parkourManager.startParkour(player);
        }
        // End plate
        else if (parkourManager.isEndPlate(block.getLocation())) {
            if (parkourManager.isInParkour(player)) {
                interactCooldown.put(player.getUniqueId(), currentTime);
                parkourManager.endParkour(player);
            }
        }
        // Checkpoints
        else if (parkourManager.getCheckpointIndex(block.getLocation()) != -1) {
            parkourManager.hitCheckpoint(player, block.getLocation());
        }
        else {
            return;
        }
        
        interactCooldown.put(player.getUniqueId(), currentTime);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        interactCooldown.remove(event.getPlayer().getUniqueId());
    }
}
