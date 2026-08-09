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

public class StaffMenuUtils {

    public static ItemStack getPinkFiller() {
        ItemStack item = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPurpleFiller() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("◄ Retour", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Fermer ou retourner", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPrevPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("◄ Page Précédente", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getNextPageButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Page Suivante ►", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fermer", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Fermer le menu", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

