package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PlayerProfileMenu {

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "Profil de " + player.getName());

        // Example item - Player head
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.setDisplayName(ChatColor.AQUA + "Statistiques");
            headItem.setItemMeta(headMeta);
        }

        inventory.setItem(13, headItem);
        
        // TODO: Fetch data from Redis to display rank, coins, stats, etc.
        
        player.openInventory(inventory);
    }
}
