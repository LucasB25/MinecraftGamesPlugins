package fr.corehost.staffmod.listeners;

import fr.corehost.staffmod.manager.FreezeManager;
import fr.corehost.staffmod.manager.ModManager;
import fr.corehost.staffmod.manager.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StaffListener implements Listener {

    private final ModManager modManager;
    private final FreezeManager freezeManager;
    private final VanishManager vanishManager;

    public StaffListener(ModManager modManager, FreezeManager freezeManager, VanishManager vanishManager) {
        this.modManager = modManager;
        this.freezeManager = freezeManager;
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        modManager.handleJoin(event.getPlayer());
        vanishManager.handleJoin(event.getPlayer());
        freezeManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        modManager.handleQuit(event.getPlayer());
        vanishManager.handleQuit(event.getPlayer());
        freezeManager.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
                event.getPlayer().sendActionBar(Component.text("Vous êtes gelé !", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (modManager.isModMode(event.getPlayer().getUniqueId()) || freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (modManager.isModMode(event.getPlayer().getUniqueId()) || freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (modManager.isModMode(player.getUniqueId()) || freezeManager.isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (freezeManager.isFrozen(damager.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            if (modManager.isModMode(damager.getUniqueId())) {
                org.bukkit.inventory.ItemStack item = damager.getInventory().getItemInMainHand();
                if (item.getType() == org.bukkit.Material.WOODEN_SWORD || item.getType() == org.bukkit.Material.STONE_SWORD) {
                    if (item.containsEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK)) {
                        event.setDamage(0);
                        return;
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (modManager.isModMode(player.getUniqueId()) || freezeManager.isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (modManager.isModMode(event.getPlayer().getUniqueId()) || freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}