package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MuteTimeGUI {

    private final StaffModPlugin plugin;
    private final String targetName;

    public MuteTimeGUI(StaffModPlugin plugin, String targetName) {
        this.plugin = plugin;
        this.targetName = targetName;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 36, Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Mute (Durée) : " + targetName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));

        StaffMenuUtils.fillBorder(inv);
        inv.setItem(27, StaffMenuUtils.getBackButton());

        ConfigurationSection mutes = plugin.getConfig().getConfigurationSection("mute_durations");
        if (mutes != null) {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
            int index = 0;
            for (String key : mutes.getKeys(false)) {
                int duration = mutes.getInt(key + ".duration", 0);

                if (index >= slots.length) break;
                int slot = slots[index++];

                String name = mutes.getString(key + ".name", "Inconnu");
                String matStr = mutes.getString(key + ".material", "PAPER");
                
                Material material;
                try {
                    material = Material.valueOf(matStr.toUpperCase());
                } catch (Exception e) {
                    material = Material.PAPER;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(name, NamedTextColor.RED, TextDecoration.BOLD));
                    meta.lore(Arrays.asList(
                            Component.empty(),
                            Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Durée : ", NamedTextColor.GRAY)).append(Component.text(formatDuration(duration), NamedTextColor.WHITE)),
                            Component.empty(),
                            Component.text("► Cliquez pour sélectionner", NamedTextColor.YELLOW)
                    ));
                    item.setItemMeta(meta);
                }
                
                inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
    }
    
    private String formatDuration(int seconds) {
        if (seconds == -1) return "Unmute";
        if (seconds == 0) return "Permanent";
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        if (hours < 24) return hours + "h";
        int days = hours / 24;
        return days + "j";
    }
}
