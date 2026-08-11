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

import java.util.Arrays;

public class MuteConfirmGUI {

    private final String targetName;
    private final String durationStr;
    private final String reason;

    public MuteConfirmGUI(StaffModPlugin plugin, String targetName, int durationSeconds, String durationStr, String reason) {
        this.targetName = targetName;
        this.durationStr = durationStr;
        this.reason = reason;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Mute (Confirmer) : " + targetName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));

        StaffMenuUtils.fillBorder(inv);
        inv.setItem(18, StaffMenuUtils.getBackButton());

        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("CONFIRMER LE MUTE", NamedTextColor.GREEN, TextDecoration.BOLD));
            confirmMeta.lore(Arrays.asList(
                    Component.empty(),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Joueur : ", NamedTextColor.GRAY)).append(Component.text(targetName, NamedTextColor.WHITE)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Durée : ", NamedTextColor.GRAY)).append(Component.text(durationStr, NamedTextColor.WHITE)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Raison : ", NamedTextColor.GRAY)).append(Component.text(reason, NamedTextColor.WHITE)),
                    Component.empty(),
                    Component.text("► Cliquez pour valider", NamedTextColor.YELLOW)
            ));
            confirmItem.setItemMeta(confirmMeta);
        }

        ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("ANNULER", NamedTextColor.RED, TextDecoration.BOLD));
            cancelMeta.lore(Arrays.asList(
                    Component.empty(),
                    Component.text("► Cliquez pour annuler", NamedTextColor.YELLOW)
            ));
            cancelItem.setItemMeta(cancelMeta);
        }

        inv.setItem(11, confirmItem);
        inv.setItem(15, cancelItem);

        player.openInventory(inv);
    }
}
