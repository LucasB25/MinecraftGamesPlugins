package fr.corehost.lobby.parkour;

import fr.corehost.api.utils.CC;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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

        long currentTime = System.currentTimeMillis();

        if (parkourManager.isPlayerInModMode(player)) {
            ParkourCourse startedCourse = parkourManager.getCourseByStartPlate(block.getLocation());
            if (startedCourse != null || parkourManager.isInParkour(player)) {
                event.setCancelled(true);
                if (parkourManager.isInParkour(player)) {
                    parkourManager.cancelParkour(player);
                }
                long lastMsg = interactCooldown.getOrDefault(player.getUniqueId(), 0L);
                if (currentTime - lastMsg > 3000) {
                    player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + CC.RED + "Vous ne pouvez pas participer au parkour en mode Modération !");
                    interactCooldown.put(player.getUniqueId(), currentTime);
                }
                return;
            }
        }
        
        // Cooldown de 2 secondes
        if (interactCooldown.containsKey(player.getUniqueId())) {
            long lastInteract = interactCooldown.get(player.getUniqueId());
            if (currentTime - lastInteract < 2000) {
                return;
            }
        }

        ParkourCourse startedCourse = parkourManager.getCourseByStartPlate(block.getLocation());
        if (startedCourse != null) {
            parkourManager.startParkour(player, startedCourse);
            interactCooldown.put(player.getUniqueId(), currentTime);
            return;
        }
        
        if (parkourManager.isInParkour(player)) {
            ActiveParkourSession session = parkourManager.getSession(player);
            ParkourCourse course = session.getCourse();
            
            // Check End plate
            if (course.getEndPlate() != null && course.getEndPlate().getBlockX() == block.getX() && course.getEndPlate().getBlockY() == block.getY() && course.getEndPlate().getBlockZ() == block.getZ()) {
                interactCooldown.put(player.getUniqueId(), currentTime);
                parkourManager.endParkour(player, block.getLocation());
                return;
            }
            
            // Check Checkpoints
            parkourManager.hitCheckpoint(player, block.getLocation());
        }
        
        interactCooldown.put(player.getUniqueId(), currentTime);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        interactCooldown.remove(event.getPlayer().getUniqueId());
    }
}
