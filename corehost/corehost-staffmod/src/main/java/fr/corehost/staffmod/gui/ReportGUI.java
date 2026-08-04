package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import fr.corehost.staffmod.manager.ReportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportGUI {

    private final StaffModPlugin plugin;

    public ReportGUI(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Signalements Actifs", NamedTextColor.DARK_RED));

        Map<UUID, ReportManager.CachedMessage> reports = plugin.getReportManager().getAllActiveReports();
        int slot = 0;
        for (Map.Entry<UUID, ReportManager.CachedMessage> entry : reports.entrySet()) {
            if (slot >= 54) break;
            ReportManager.CachedMessage msg = entry.getValue();

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(msg.getSenderName(), NamedTextColor.RED));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Message: " + msg.getContent(), NamedTextColor.GRAY));
            lore.add(Component.text("Serveur: " + msg.getServer(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Clic Gauche: Se teleporter", NamedTextColor.GREEN));
            lore.add(Component.text("Clic Droit: Marquer comme resolu", NamedTextColor.YELLOW));
            lore.add(Component.text(entry.getKey().toString(), NamedTextColor.BLACK)); // Hidden ID
            
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }
}