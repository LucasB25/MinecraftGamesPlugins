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

public class MuteReasonGUI {

    private final StaffModPlugin plugin;
    private final String targetName;
    private final String durationStr;

    public MuteReasonGUI(StaffModPlugin plugin, String targetName, int durationSeconds, String durationStr) {
        this.plugin = plugin;
        this.targetName = targetName;
        this.durationStr = durationStr;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 36, Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Mute (Raison) : " + targetName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));

        StaffMenuUtils.fillBorder(inv);
        inv.setItem(27, StaffMenuUtils.getBackButton());

        ConfigurationSection reasons = plugin.getConfig().getConfigurationSection("mute_reasons");
        if (reasons != null) {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
            int index = 0;
            for (String key : reasons.getKeys(false)) {
                if (index >= slots.length) break;
                int slot = slots[index++];

                String name = reasons.getString(key + ".name", "Inconnu");
                String matStr = reasons.getString(key + ".material", "PAPER");
                
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
                            Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Durée : ", NamedTextColor.GRAY)).append(Component.text(durationStr, NamedTextColor.WHITE)),
                            Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Raison : ", NamedTextColor.GRAY)).append(Component.text(name, NamedTextColor.WHITE)),
                            Component.empty(),
                            Component.text("► Cliquez pour continuer", NamedTextColor.YELLOW)
                    ));
                    item.setItemMeta(meta);
                }
                
                inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
    }
}
