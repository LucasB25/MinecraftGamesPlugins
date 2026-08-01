package fr.corehost.lobby.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuUtils {

    public static ItemStack getPinkFiller() {
        ItemStack filler = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    public static ItemStack getPurpleFiller() {
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    public static void fillBottomRow(Inventory inventory) {
        int size = inventory.getSize();
        if (size < 9) return;
        
        ItemStack pink = getPinkFiller();
        ItemStack purple = getPurpleFiller();
        
        int start = size - 9;
        for (int i = start; i < size; i++) {
            inventory.setItem(i, (i % 2 == 0) ? pink : purple);
        }
    }

    public static void fillBorder(Inventory inventory) {
        int size = inventory.getSize();
        ItemStack pink = getPinkFiller();
        ItemStack purple = getPurpleFiller();
        
        for (int i = 0; i < size; i++) {
            // Top row or bottom row or left column or right column
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, (i % 2 == 0) ? pink : purple);
            }
        }
    }
    
    public static ItemStack getBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "◄ Retour au Profil");
            back.setItemMeta(backMeta);
        }
        return back;
    }

    public static ItemStack getPrevPageButton() {
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prev.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(ChatColor.YELLOW + "◄ Page Précédente");
            prev.setItemMeta(prevMeta);
        }
        return prev;
    }

    public static ItemStack getNextPageButton() {
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(ChatColor.YELLOW + "Page Suivante ►");
            next.setItemMeta(nextMeta);
        }
        return next;
    }
}
