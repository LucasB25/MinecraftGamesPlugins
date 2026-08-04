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

import java.util.Arrays;

public class PlayerSSGUI {

    private final StaffModPlugin plugin;
    private final String targetName;

    public PlayerSSGUI(StaffModPlugin plugin, String targetName) {
        this.plugin = plugin;
        this.targetName = targetName;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("Mod: " + targetName, NamedTextColor.DARK_RED));

        // Center: Player Head
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) headItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
            skullMeta.displayName(Component.text("Joueur: ", NamedTextColor.GRAY).append(Component.text(targetName, NamedTextColor.GOLD)));
            
            Player targetPlayer = Bukkit.getPlayerExact(targetName);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                String rankText = "Aucun";
                try {
                    net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                    net.luckperms.api.model.user.User user = api.getUserManager().getUser(targetPlayer.getUniqueId());
                    if (user != null) {
                        String group = user.getPrimaryGroup();
                        if (group != null) {
                            rankText = group.substring(0, 1).toUpperCase() + group.substring(1);
                        }
                    }
                } catch (Exception ignored) {}
                
                boolean isPremium = targetPlayer.getUniqueId().version() == 4;
                String accountType = isPremium ? "Premium" : "Crack";

                skullMeta.lore(Arrays.asList(
                    Component.text("Statut: En Ligne", NamedTextColor.GREEN),
                    Component.text("Grade: " + rankText, NamedTextColor.AQUA),
                    Component.text("Compte: " + accountType, isPremium ? NamedTextColor.GOLD : NamedTextColor.GRAY),
                    Component.text("Ping: " + targetPlayer.getPing() + " ms", NamedTextColor.GRAY),
                    Component.text("Vie: " + (int)targetPlayer.getHealth() + "/20", NamedTextColor.RED)
                ));
            } else {
                skullMeta.lore(Arrays.asList(
                    Component.text("Statut: Autre serveur ou Hors-Ligne", NamedTextColor.YELLOW)
                ));
            }
            headItem.setItemMeta(skullMeta);
        }
        inv.setItem(22, headItem);

        // 1. Invsee (Slot 12)
        ItemStack invItem = new ItemStack(Material.CHEST);
        ItemMeta invMeta = invItem.getItemMeta();
        invMeta.displayName(Component.text("Voir l'inventaire", NamedTextColor.YELLOW));
        invMeta.lore(Arrays.asList(Component.text("Cliquez pour ouvrir l'inventaire", NamedTextColor.GRAY)));
        invItem.setItemMeta(invMeta);
        inv.setItem(12, invItem);

        // 2. Freeze (Slot 14)
        ItemStack freezeItem = new ItemStack(Material.PACKED_ICE);
        ItemMeta freezeMeta = freezeItem.getItemMeta();
        freezeMeta.displayName(Component.text("Geler (Freeze)", NamedTextColor.AQUA));
        freezeMeta.lore(Arrays.asList(Component.text("Geler/Dégeler le joueur", NamedTextColor.GRAY)));
        freezeItem.setItemMeta(freezeMeta);
        inv.setItem(14, freezeItem);

        // 3. Teleport (Slot 30)
        ItemStack tpItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta tpMeta = tpItem.getItemMeta();
        tpMeta.displayName(Component.text("Se téléporter (TP)", NamedTextColor.LIGHT_PURPLE));
        tpMeta.lore(Arrays.asList(Component.text("Rejoindre le serveur du joueur", NamedTextColor.GRAY)));
        tpItem.setItemMeta(tpMeta);
        inv.setItem(30, tpItem);
        
        // 4. Sanctions History (Slot 32)
        ItemStack historyItem = new ItemStack(Material.BOOK);
        ItemMeta historyMeta = historyItem.getItemMeta();
        historyMeta.displayName(Component.text("Historique des Sanctions", NamedTextColor.GOLD));
        historyMeta.lore(Arrays.asList(Component.text("Bientôt", NamedTextColor.GRAY)));
        historyItem.setItemMeta(historyMeta);
        inv.setItem(32, historyItem);
        
        // 5. Mute/Ban Actions
        ItemStack muteItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta muteMeta = muteItem.getItemMeta();
        muteMeta.displayName(Component.text("TempMute (Bientôt)", NamedTextColor.RED));
        muteItem.setItemMeta(muteMeta);
        inv.setItem(38, muteItem);
        
        ItemStack banItem = new ItemStack(Material.BARRIER);
        ItemMeta banMeta = banItem.getItemMeta();
        banMeta.displayName(Component.text("TempBan (Bientôt)", NamedTextColor.DARK_RED));
        banItem.setItemMeta(banMeta);
        inv.setItem(42, banItem);

        player.openInventory(inv);
    }
}
