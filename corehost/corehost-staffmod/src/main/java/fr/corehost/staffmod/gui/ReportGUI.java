package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import fr.corehost.staffmod.manager.ReportManager;
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
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("» ", NamedTextColor.DARK_GRAY).append(Component.text("Signalements Actifs", NamedTextColor.RED, TextDecoration.BOLD)));
        StaffMenuUtils.fillBorder(inv);
        inv.setItem(45, StaffMenuUtils.getCloseButton());

        Map<UUID, ReportManager.CachedMessage> reports = plugin.getReportManager().getAllActiveReports();
        
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int slotIndex = 0;
        
        for (Map.Entry<UUID, ReportManager.CachedMessage> entry : reports.entrySet()) {
            if (slotIndex >= slots.length) break;
            ReportManager.CachedMessage msg = entry.getValue();

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(msg.getSenderName(), NamedTextColor.GOLD, TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Message : ", NamedTextColor.GRAY)).append(Component.text(msg.getContent(), NamedTextColor.WHITE)));
                lore.add(Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Serveur : ", NamedTextColor.GRAY)).append(Component.text(msg.getServer(), NamedTextColor.WHITE)));
                lore.add(Component.text("► Clic Gauche : Se téléporter", NamedTextColor.GREEN));
                lore.add(Component.text("► Clic Droit : Marquer comme résolu", NamedTextColor.YELLOW));
                lore.add(Component.text(entry.getKey().toString(), NamedTextColor.BLACK)); // Hidden ID
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[slotIndex++], item);
        }

        player.openInventory(inv);
    }
}
