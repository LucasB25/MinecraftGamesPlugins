package fr.corehost.staffmod.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuUtils {

    public static ItemStack getRedFiller() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getGrayFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillBottomRow(Inventory inventory) {
        int size = inventory.getSize();
        if (size < 9) return;
        
        ItemStack red = getRedFiller();
        ItemStack gray = getGrayFiller();
        
        int start = size - 9;
        for (int i = start; i < size; i++) {
            inventory.setItem(i, (i % 2 == 0) ? red : gray);
        }
    }

    public static void fillBorder(Inventory inventory) {
        int size = inventory.getSize();
        ItemStack red = getRedFiller();
        ItemStack gray = getGrayFiller();
        
        for (int i = 0; i < size; i++) {
            // Top row or bottom row or left column or right column
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, (i % 2 == 0) ? red : gray);
            }
        }
    }
    
    public static ItemStack getBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("◄ Retour", NamedTextColor.RED));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Fermer ou retourner", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPrevPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("◄ Page Précédente", NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Page Suivante ►", NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fermer", NamedTextColor.RED, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Fermer le menu", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

