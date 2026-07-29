package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PlayerProfileMenu implements CustomMenu {

    private final Inventory inventory;

    public PlayerProfileMenu(Player player) {
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.LIGHT_PURPLE + "Profil de " + player.getName());
        initializeItems(player);
    }

    private void initializeItems(Player player) {
        // Player head
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.setDisplayName(ChatColor.AQUA + "Statistiques");
            headItem.setItemMeta(headMeta);
        }
        inventory.setItem(13, headItem);
        
        // TODO: Fetch data from Redis to display rank, coins, stats, etc.
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        // Profil is currently display only, nothing to click
    }
}
