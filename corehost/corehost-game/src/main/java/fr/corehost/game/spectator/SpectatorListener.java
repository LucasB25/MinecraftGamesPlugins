package fr.corehost.game.spectator;

import fr.corehost.game.CoreHostGame;
import fr.corehost.game.spectator.gui.SpectatorTeleportMenu;

import fr.corehost.api.utils.CC;

import org.bukkit.Material;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpectatorListener implements Listener {

    private final SpectatorManager manager;
    private final SpectatorTeleportMenu teleportMenu;
    private final CoreHostGame plugin;

    public SpectatorListener(CoreHostGame plugin, SpectatorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.teleportMenu = new SpectatorTeleportMenu(manager);
    }

    // --- Interaction Cancellations ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!manager.isSpectator(player)) return;

        // Prevent physical interactions (pressure plates, crops) and block interactions (chests, doors)
        if (event.getAction() == Action.PHYSICAL || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }

        // Handle item clicks (Right click air/block)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = item.getItemMeta().getDisplayName();
                
                if (item.getType() == Material.COMPASS && name.contains("Téléportation")) {
                    event.setCancelled(true);
                    teleportMenu.openMenu(player);
                } 
                else if (item.getType() == Material.FEATHER && name.contains("Vitesse")) {
                    event.setCancelled(true);
                    float currentSpeed = player.getFlySpeed();
                    float level1 = (float) plugin.getConfig().getDouble("spectator.speeds.level-1", 0.1);
                    float level2 = (float) plugin.getConfig().getDouble("spectator.speeds.level-2", 0.2);
                    float level3 = (float) plugin.getConfig().getDouble("spectator.speeds.level-3", 0.3);
                    float newSpeed = level1; // x1
                    String speedText = "x1";
                    
                    if (currentSpeed < (level1 + 0.05f)) { // Currently x1
                        newSpeed = level2;
                        speedText = "x2";
                    } else if (currentSpeed < (level2 + 0.05f)) { // Currently x2
                        newSpeed = level3;
                        speedText = "x3";
                    }
                    
                    player.setFlySpeed(newSpeed);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(CC.YELLOW + "Vitesse de vol: " + speedText);
                    item.setItemMeta(meta);
                    player.sendMessage(CC.GRAY + "Vitesse de vol réglée sur " + CC.AQUA + speedText + CC.GRAY + ".");
                }
                else if (item.getType() == Material.RED_BED && name.contains("Quitter")) {
                    event.setCancelled(true);
                    player.sendMessage(CC.GRAY + "Retour au lobby...");
                    
                    try {
                        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(plugin.getConfig().getString("bungeecord.fallback-server", "lobby"));
                        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                    } catch (Exception e) {
                        player.kickPlayer("Retour au Hub");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (manager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (manager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && manager.isSpectator((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            if (manager.isSpectator(p)) {
                event.setCancelled(true);
            }
        }
        // Handle GUI teleport click
        teleportMenu.handleClick(event);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player && manager.isSpectator((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    // --- Damage and Entities Protections ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && manager.isSpectator((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Prevent spectators from dealing damage
        if (event.getDamager() instanceof Player && manager.isSpectator((Player) event.getDamager())) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent spectators from being damaged by anything, including projectiles
        if (event.getEntity() instanceof Player && manager.isSpectator((Player) event.getEntity())) {
            event.setCancelled(true);
            
            // If it's a projectile, we might want it to pass through, but canceling damage doesn't remove the physical block.
            // ProjectileHitEvent handles the pass-through better in modern versions or we just accept it vanishes.
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        // In 1.8-1.12, projectiles might hit invisible adventure players. 
        // If it hits a spectator, we can't easily "unhit" it, but we canceled the damage.
        if (event.getHitEntity() instanceof Player) {
            Player hit = (Player) event.getHitEntity();
            if (manager.isSpectator(hit)) {
                // To simulate passing through, you could potentially re-spawn the projectile, but it's complex.
                // We'll leave it as just absorbing the arrow but doing no damage, which is standard on some networks if no-collision fails.
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && manager.isSpectator((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && manager.isSpectator((Player) event.getEntered())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && manager.isSpectator((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    // --- Player State and Edge Cases ---

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (manager.isSpectator(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setDeathMessage(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (manager.isSpectator(event.getPlayer())) {
            manager.setSpectator(event.getPlayer(), false);
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Handled in IsolationListener mainly, but good to ensure if someone joins in spectator list (e.g. reload)
    }

    // --- Optional Suggestions ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (manager.isSpectator(sender)) {
            // Anti-ghosting: Only spectators hear spectators
            event.getRecipients().removeIf(recipient -> !manager.isSpectator(recipient));
            event.setFormat(CC.GRAY + "[SPEC] " + CC.WHITE + "%s: %s");
        } else {
            // Optional: Alive players shouldn't hear spectators either, but already handled above.
        }
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!manager.isSpectator(event.getPlayer())) return;
        
        // Very basic distance limit from world spawn to prevent chunk loading lag
        int maxDist = plugin.getConfig().getInt("spectator.max-distance", 500);
        int maxDistSq = maxDist * maxDist;
        if (event.getTo() != null && event.getTo().distanceSquared(event.getPlayer().getWorld().getSpawnLocation()) > maxDistSq) {
            event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
            event.getPlayer().sendMessage(CC.RED + "Vous ne pouvez pas vous éloigner plus.");
        }
    }
}
