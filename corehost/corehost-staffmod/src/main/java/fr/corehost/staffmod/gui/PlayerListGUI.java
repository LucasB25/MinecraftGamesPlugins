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
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("» ", NamedTextColor.DARK_GRAY).append(Component.text("Modération - Joueurs", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)));
        MenuUtils.fillBorder(inv);
        inv.setItem(45, MenuUtils.getCloseButton());

        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int slotIndex = 0;
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slotIndex >= slots.length) break;
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                boolean isFrozen = plugin.getFreezeManager().isFrozen(online.getUniqueId());
                
                meta.displayName(Component.text(online.getName(), isFrozen ? NamedTextColor.RED : NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD));
                
                java.util.List<Component> lore = new java.util.ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Statut : ", NamedTextColor.GRAY)).append(Component.text(isFrozen ? "Gelé" : "Actif", isFrozen ? NamedTextColor.RED : NamedTextColor.GREEN)));
                lore.add(Component.empty());
                lore.add(Component.text("► Clic pour ", NamedTextColor.YELLOW).append(Component.text(isFrozen ? "Dégeler" : "Geler", isFrozen ? NamedTextColor.GREEN : NamedTextColor.RED)));
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[slotIndex++], item);
        }

        player.openInventory(inv);
    }
}