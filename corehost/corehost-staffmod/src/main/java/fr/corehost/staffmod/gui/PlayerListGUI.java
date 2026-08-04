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

public class PlayerListGUI {

    private final StaffModPlugin plugin;

    public PlayerListGUI(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Joueurs (Freeze)", NamedTextColor.DARK_AQUA));

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) break;
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            boolean isFrozen = plugin.getFreezeManager().isFrozen(online.getUniqueId());
            
            meta.displayName(Component.text(online.getName(), isFrozen ? NamedTextColor.RED : NamedTextColor.GREEN));
            
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Clic pour ").append(Component.text(isFrozen ? "Degeler" : "Geler", isFrozen ? NamedTextColor.GREEN : NamedTextColor.RED)));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }
}