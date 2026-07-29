package fr.corehost.lobby.listeners;

import fr.corehost.lobby.gui.CustomMenu;
import fr.corehost.lobby.gui.HostCreateMenu;
import fr.corehost.lobby.gui.HostSearchMenu;
import fr.corehost.lobby.gui.PlayerProfileMenu;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyListener implements Listener {

    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

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
            searchMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Jouer " + ChatColor.GRAY + "(Clic-Droit)");
            searchHost.setItemMeta(searchMeta);
        }
        player.getInventory().setItem(4, searchHost);

        // Slot 8: Profile
        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Mon Profil " + ChatColor.GRAY + "(Clic-Droit)");
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
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - clickCooldowns.getOrDefault(player.getUniqueId(), 0L) < 500) {
            event.setCancelled(true);
            return;
        }
        clickCooldowns.put(player.getUniqueId(), currentTime);

        if (item.getType() == Material.COMPASS || item.getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
        }

        if (item.getType() == Material.COMPASS) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            new HostSearchMenu().open(player);
        } else if (item.getType() == Material.PLAYER_HEAD) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            new PlayerProfileMenu(player).open(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Always cancel clicks in lobby to prevent moving hotbar items
        event.setCancelled(true);

        long currentTime = System.currentTimeMillis();
        if (currentTime - clickCooldowns.getOrDefault(player.getUniqueId(), 0L) < 500) {
            return; // Spam protection
        }

        // If clicking a custom menu, let it handle the logic
        if (event.getInventory().getHolder() instanceof CustomMenu) {
            clickCooldowns.put(player.getUniqueId(), currentTime);
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
