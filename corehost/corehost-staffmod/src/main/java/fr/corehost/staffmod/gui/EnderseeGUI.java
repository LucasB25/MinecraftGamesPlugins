package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnderseeGUI {

    private final Player target;

    public EnderseeGUI(StaffModPlugin plugin, Player target) {
        this.target = target;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 36, Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Enderchest : " + target.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));

        // Copier l'enderchest (0 à 26)
        for (int i = 0; i < 27; i++) {
            ItemStack item = target.getEnderChest().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                inv.setItem(i, item.clone());
            }
        }

        // Ligne de séparation et boutons (27 à 35)
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, StaffMenuUtils.getPurpleFiller());
        }

        // Bouton Retour (35)
        inv.setItem(35, StaffMenuUtils.getBackButton());

        player.openInventory(inv);
    }
}
