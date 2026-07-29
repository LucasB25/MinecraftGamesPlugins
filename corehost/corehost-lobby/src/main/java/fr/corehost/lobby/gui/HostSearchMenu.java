package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HostSearchMenu {

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Recherche de Host");

        // Example items
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName(ChatColor.GREEN + "Rafraîchir la liste");
            refreshItem.setItemMeta(refreshMeta);
        }

        inventory.setItem(49, refreshItem);
        
        // TODO: Fetch running CloudNet instances for minigames and display them here
        
        player.openInventory(inventory);
    }
}
