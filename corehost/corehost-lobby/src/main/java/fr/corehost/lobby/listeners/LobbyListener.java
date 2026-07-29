package fr.corehost.lobby.listeners;

import fr.corehost.lobby.gui.CustomMenu;
import fr.corehost.lobby.gui.HostCreateMenu;
import fr.corehost.lobby.gui.HostSearchMenu;
import fr.corehost.lobby.gui.PlayerProfileMenu;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class LobbyListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setAllowFlight(false);

        // Slot 4: Play Menu (Compass)
        ItemStack searchHost = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchHost.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(ChatColor.AQUA + "Menu des Jeux");
            searchHost.setItemMeta(searchMeta);
        }
        player.getInventory().setItem(4, searchHost);

        // Slot 8: Profile
        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Profil");
            profile.setItemMeta(profileMeta);
        }
        player.getInventory().setItem(8, profile);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || !event.getAction().name().contains("RIGHT")) {
            return;
        }

        if (item.getType() == Material.COMPASS || item.getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
        }

        if (item.getType() == Material.COMPASS) {
            new HostSearchMenu().open(player);
        } else if (item.getType() == Material.PLAYER_HEAD) {
            new PlayerProfileMenu(player).open(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Always cancel clicks in lobby to prevent moving hotbar items
        event.setCancelled(true);

        // If clicking a custom menu, let it handle the logic
        if (event.getInventory().getHolder() instanceof CustomMenu) {
            CustomMenu customMenu = (CustomMenu) event.getInventory().getHolder();
            customMenu.onClick(event, player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Prevent drag-splitting items in the lobby
        if (event.getWhoClicked() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }
}
