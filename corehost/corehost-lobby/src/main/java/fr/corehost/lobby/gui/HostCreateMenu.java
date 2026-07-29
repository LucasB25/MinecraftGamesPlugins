package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HostCreateMenu {

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Création de Host");

        // Example item
        ItemStack createItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.YELLOW + "Créer un Mini-Jeu (UHC)");
            createItem.setItemMeta(createMeta);
        }

        inventory.setItem(13, createItem);
        
        // TODO: Handle click event to request CloudNet to start a specific task
        
        player.openInventory(inventory);
    }
}
