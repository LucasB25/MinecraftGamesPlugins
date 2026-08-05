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
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("» ", NamedTextColor.DARK_GRAY).append(Component.text("Modération : " + targetName, NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)));
        MenuUtils.fillBorder(inv);
        inv.setItem(36, MenuUtils.getCloseButton());

        // Center: Player Head
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) headItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
            skullMeta.displayName(Component.text("Joueur : ", NamedTextColor.GRAY).append(Component.text(targetName, NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD)));
            
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
                    Component.empty(),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Statut : ", NamedTextColor.GRAY)).append(Component.text("En Ligne", NamedTextColor.GREEN)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Grade : ", NamedTextColor.GRAY)).append(Component.text(rankText, NamedTextColor.AQUA)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Compte : ", NamedTextColor.GRAY)).append(Component.text(accountType, isPremium ? NamedTextColor.GOLD : NamedTextColor.GRAY)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Ping : ", NamedTextColor.GRAY)).append(Component.text(targetPlayer.getPing() + " ms", NamedTextColor.WHITE)),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Santé : ", NamedTextColor.GRAY)).append(Component.text((int)targetPlayer.getHealth() + "/20 ❤", NamedTextColor.RED))
                ));
            } else {
                skullMeta.lore(Arrays.asList(
                    Component.empty(),
                    Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Statut : ", NamedTextColor.GRAY)).append(Component.text("Autre serveur ou Hors-Ligne", NamedTextColor.YELLOW))
                ));
            }
            headItem.setItemMeta(skullMeta);
        }
        inv.setItem(13, headItem);

        // 1. Invsee (Slot 19)
        ItemStack invItem = new ItemStack(Material.CHEST);
        ItemMeta invMeta = invItem.getItemMeta();
        if (invMeta != null) {
            invMeta.displayName(Component.text("Inventaire", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            invMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Consulter l'inventaire principal", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Cliquez pour ouvrir", NamedTextColor.YELLOW)
            ));
            invItem.setItemMeta(invMeta);
        }
        inv.setItem(19, invItem);

        // 2. Enderchest (Slot 20)
        ItemStack ecItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta ecMeta = ecItem.getItemMeta();
        if (ecMeta != null) {
            ecMeta.displayName(Component.text("Enderchest", NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD));
            ecMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Consulter le coffre de l'Ender", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Cliquez pour ouvrir", NamedTextColor.YELLOW)
            ));
            ecItem.setItemMeta(ecMeta);
        }
        inv.setItem(20, ecItem);

        // 3. Teleport (Slot 21)
        ItemStack tpItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta tpMeta = tpItem.getItemMeta();
        if (tpMeta != null) {
            tpMeta.displayName(Component.text("Téléportation", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD));
            tpMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Se téléporter sur le serveur du joueur", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Cliquez pour vous téléporter", NamedTextColor.YELLOW)
            ));
            tpItem.setItemMeta(tpMeta);
        }
        inv.setItem(21, tpItem);

        // 4. Freeze (Slot 23)
        ItemStack freezeItem = new ItemStack(Material.PACKED_ICE);
        ItemMeta freezeMeta = freezeItem.getItemMeta();
        if (freezeMeta != null) {
            freezeMeta.displayName(Component.text("Geler le Joueur", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            freezeMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Immobiliser ou libérer le joueur", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Cliquez pour basculer le freeze", NamedTextColor.YELLOW)
            ));
            freezeItem.setItemMeta(freezeMeta);
        }
        inv.setItem(23, freezeItem);

        // 7. Sanctions History (Slot 29)
        ItemStack historyItem = new ItemStack(Material.BOOK);
        ItemMeta historyMeta = historyItem.getItemMeta();
        if (historyMeta != null) {
            historyMeta.displayName(Component.text("Historique", NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD));
            historyMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Historique des sanctions du joueur", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(Bientôt disponible)", NamedTextColor.DARK_GRAY)
            ));
            historyItem.setItemMeta(historyMeta);
        }
        inv.setItem(29, historyItem);
        
        // 8. Mute Action (Slot 31)
        ItemStack muteItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta muteMeta = muteItem.getItemMeta();
        if (muteMeta != null) {
            muteMeta.displayName(Component.text("Rendre Muet", NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD));
            muteMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Rendre muet temporairement", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(Bientôt disponible)", NamedTextColor.DARK_GRAY)
            ));
            muteItem.setItemMeta(muteMeta);
        }
        inv.setItem(31, muteItem);
        
        // 9. Ban Action (Slot 33)
        ItemStack banItem = new ItemStack(Material.BARRIER);
        ItemMeta banMeta = banItem.getItemMeta();
        if (banMeta != null) {
            banMeta.displayName(Component.text("Bannir", NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD));
            banMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Bannir le joueur du réseau", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(Bientôt disponible)", NamedTextColor.DARK_GRAY)
            ));
            banItem.setItemMeta(banMeta);
        }
        inv.setItem(33, banItem);

        player.openInventory(inv);
    }
}
