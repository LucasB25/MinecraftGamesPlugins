package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ModGUI {

    private final StaffModPlugin plugin;

    public ModGUI(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Menu de Moderation", NamedTextColor.DARK_RED));

        // Vanish Item
        ItemStack vanishItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta vanishMeta = vanishItem.getItemMeta();
        boolean isVanished = plugin.getVanishManager().isVanished(player.getUniqueId());
        vanishMeta.displayName(Component.text("Vanish: " + (isVanished ? "ON" : "OFF"), isVanished ? NamedTextColor.GREEN : NamedTextColor.RED));
        vanishItem.setItemMeta(vanishMeta);
        inv.setItem(10, vanishItem);

        // Staff List Item
        ItemStack staffItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta staffMeta = staffItem.getItemMeta();
        staffMeta.displayName(Component.text("Staff en Ligne", NamedTextColor.GOLD));
        staffItem.setItemMeta(staffMeta);
        inv.setItem(12, staffItem);

        // Reports Item
        ItemStack reportsItem = new ItemStack(Material.PAPER);
        ItemMeta reportsMeta = reportsItem.getItemMeta();
        reportsMeta.displayName(Component.text("Signalements (Reports)", NamedTextColor.YELLOW));
        reportsItem.setItemMeta(reportsMeta);
        inv.setItem(14, reportsItem);

        // Freeze/Players Item
        ItemStack freezeItem = new ItemStack(Material.PACKED_ICE);
        ItemMeta freezeMeta = freezeItem.getItemMeta();
        freezeMeta.displayName(Component.text("Joueurs en ligne (Freeze)", NamedTextColor.AQUA));
        freezeItem.setItemMeta(freezeMeta);
        inv.setItem(16, freezeItem);

        player.openInventory(inv);
    }
}