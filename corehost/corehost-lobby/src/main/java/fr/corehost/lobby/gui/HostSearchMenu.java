package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HostSearchMenu implements CustomMenu {

    private final Inventory inventory;

    public HostSearchMenu() {
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_AQUA + "Recherche de Host");
        initializeItems();
    }

    private void initializeItems() {
        // Bottom bar decoration
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Create Host item (Centered on last line: slot 49)
        ItemStack createItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Créer un Host");
            createMeta.setLore(java.util.Arrays.asList(
                "",
                ChatColor.GRAY + "Cliquez pour créer votre propre",
                ChatColor.GRAY + "serveur de jeu personnalisé !"
            ));
            createItem.setItemMeta(createMeta);
        }
        inventory.setItem(49, createItem);

        // Refresh item (Next to it: slot 50)
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Rafraîchir la liste");
            refreshMeta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Recharger la liste des serveurs",
                ChatColor.GRAY + "actuellement disponibles."
            ));
            refreshItem.setItemMeta(refreshMeta);
        }
        inventory.setItem(50, refreshItem);
        
        // TODO: Fetch running CloudNet instances for minigames and display them here
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
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.EMERALD) {
            player.sendMessage(ChatColor.YELLOW + "Rafraîchissement de la liste des serveurs...");
            // TODO: refresh logic
        } else if (clicked.getType() == Material.NETHER_STAR) {
            new HostCreateMenu().open(player);
        }
    }
}
