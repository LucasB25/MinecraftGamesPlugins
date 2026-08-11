package fr.corehost.lobby.gui;

import fr.corehost.api.utils.CC;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import fr.corehost.lobby.utils.ItemBuilder;

public class LobbyMenuUtils {

    public static ItemStack getPinkFiller() {
        return new ItemBuilder(Material.PINK_STAINED_GLASS_PANE).setName(" ").build();
    }

    public static ItemStack getPurpleFiller() {
        return new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).setName(" ").build();
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
        return new ItemBuilder(Material.ARROW).setName(CC.RED + "◄ Retour au Profil").build();
    }

    public static ItemStack getPrevPageButton() {
        return new ItemBuilder(Material.ARROW).setName(CC.YELLOW + "◄ Page Précédente").build();
    }

    public static ItemStack getNextPageButton() {
        return new ItemBuilder(Material.ARROW).setName(CC.YELLOW + "Page Suivante ►").build();
    }

    public static ItemStack getBackToSettingsButton() {
        return new ItemBuilder(Material.ARROW).setName(CC.RED + "◄ Retour aux Paramètres").build();
    }

    public static ItemStack getBackToCreateButton() {
        return new ItemBuilder(Material.ARROW).setName(CC.RED + "◄ Retour à la Création").build();
    }
}
