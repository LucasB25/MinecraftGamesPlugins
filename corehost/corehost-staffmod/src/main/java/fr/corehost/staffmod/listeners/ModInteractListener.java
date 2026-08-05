package fr.corehost.staffmod.listeners;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.ArrayList;

public class ModInteractListener implements Listener {

    private final StaffModPlugin plugin;

    public ModInteractListener(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (plugin.getModManager().isModMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (plugin.getModManager().isModMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getModManager().isModMode(player.getUniqueId())) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (item.getType() == Material.COMPASS) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.2f);
                // Random TP
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                players.removeIf(p -> p.hasPermission("staffmod.mod") || p.getUniqueId().equals(player.getUniqueId()));
                
                if (players.isEmpty()) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Aucun joueur disponible pour la téléportation.", NamedTextColor.RED)));
                } else {
                    Player target = players.get(new java.util.Random().nextInt(players.size()));
                    player.teleport(target.getLocation());
                    player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté aléatoirement sur " + target.getName(), NamedTextColor.GREEN)));
                }
            } else if (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                // Vanish Toggle
                plugin.getVanishManager().toggleVanish(player);
                boolean isVanished = plugin.getVanishManager().isVanished(player.getUniqueId());
                
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("Vanish : " + (isVanished ? "ON" : "OFF"), isVanished ? NamedTextColor.GREEN : NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD));
                    item.setItemMeta(meta);
                    item.setType(isVanished ? Material.LIME_DYE : Material.GRAY_DYE);
                }
            } else if (item.getType() == Material.RED_BED) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                // Leave Mod Mode
                plugin.getModManager().setModMode(player, false);
                plugin.getVanishManager().setVanished(player, false);
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        Player player = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        
        if (!plugin.getModManager().isModMode(player.getUniqueId())) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        if (item.getType() == Material.PACKED_ICE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            boolean isFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
            
            // Freeze
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("action", "FREEZE_PLAYER");
            json.addProperty("target", target.getName());
            json.addProperty("sender", player.getName());

            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().publish("corehost:staff:events", json.toString());
            }
            if (isFrozen) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Vous avez dégelé le joueur ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW)).append(Component.text(".", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(plugin.getPrefix().append(Component.text("Vous avez gelé le joueur ", NamedTextColor.RED))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW)).append(Component.text(".", NamedTextColor.RED)));
            }
        } else if (item.getType() == Material.BOOK) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.2f, 1.2f);
            // Inspect SS
            new fr.corehost.staffmod.gui.PlayerSSGUI(plugin, target.getName()).open(player);
        }
    }
}
