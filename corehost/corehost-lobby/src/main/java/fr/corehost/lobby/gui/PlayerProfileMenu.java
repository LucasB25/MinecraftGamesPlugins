package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

import fr.corehost.lobby.CoreHostLobby;

public class PlayerProfileMenu implements CustomMenu {

    private final Inventory inventory;
    private final CoreHostLobby plugin;

    public PlayerProfileMenu(CoreHostLobby plugin, Player player) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.LIGHT_PURPLE + "Profil de " + player.getName());
        initializeItems(player);
    }

    private void initializeItems(Player player) {
        // Border decoration (Pink + Purple alternating)
        ItemStack filler1 = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta meta1 = filler1.getItemMeta();
        if (meta1 != null) { meta1.setDisplayName(" "); filler1.setItemMeta(meta1); }

        ItemStack filler2 = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta2 = filler2.getItemMeta();
        if (meta2 != null) { meta2.setDisplayName(" "); filler2.setItemMeta(meta2); }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) {
                inventory.setItem(i, (i % 2 == 0) ? filler1 : filler2);
            }
        }

        // ── Slot 10: Player Head (General Info) ──
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + player.getName());

            long firstPlayed = player.getFirstPlayed();
            String firstPlayedDate = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(firstPlayed));
            
            String accountType = (player.getUniqueId().version() == 4) ? ChatColor.GOLD + "Premium" : ChatColor.RED + "Crack";

            headMeta.setLore(Arrays.asList(
                "",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Grade : " + ChatColor.GREEN + "Joueur",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Compte : " + accountType,
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Première connexion : " + ChatColor.WHITE + firstPlayedDate,
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Ping : " + ChatColor.YELLOW + player.getPing() + "ms",
                ""
            ));
            headItem.setItemMeta(headMeta);
        }
        inventory.setItem(10, headItem);

        // ── Slot 11: Game Stats (Sword) ──
        ItemStack statsItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Statistiques de Jeu");
            statsMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Cliquez pour voir vos",
                ChatColor.GRAY + "statistiques dans les",
                ChatColor.GRAY + "différents mini-jeux.",
                ""
            ));
            statsMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(11, statsItem);

        // ── Slot 12: Friends (Name Tag) ──
        ItemStack friendsItem = new ItemStack(Material.NAME_TAG);
        ItemMeta friendsMeta = friendsItem.getItemMeta();
        if (friendsMeta != null) {
            friendsMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Amis");
            friendsMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Gérez votre liste d'amis",
                ChatColor.GRAY + "et voyez qui est en ligne.",
                ""
            ));
            friendsItem.setItemMeta(friendsMeta);
        }
        inventory.setItem(12, friendsItem);

        // ── Slot 13: Empty (glass pane) ──
        inventory.setItem(13, filler2);

        // ── Slot 14: Party (Cake) ──
        ItemStack partyItem = new ItemStack(Material.CAKE);
        ItemMeta partyMeta = partyItem.getItemMeta();
        if (partyMeta != null) {
            partyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Party");
            partyMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Bientôt disponible...",
                ""
            ));
            partyItem.setItemMeta(partyMeta);
        }
        inventory.setItem(14, partyItem);

        ItemStack discordItem = new ItemStack(Material.LAPIS_LAZULI);
        ItemMeta discordMeta = discordItem.getItemMeta();
        if (discordMeta != null) {
            discordMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Sécurité du Compte");
            
            if (player.getUniqueId().version() == 4) {
                // Premium Account
                discordMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GREEN + "✔ Compte Premium",
                    ChatColor.GRAY + "Sécurisé par Mojang",
                    ""
                ));
            } else {
                // Cracked Account
                boolean isLinked = false;
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    String discordId = plugin.getRedisManager().get("corehost:discord_link:player:" + player.getUniqueId().toString());
                    if (discordId != null) isLinked = true;
                }
                
                if (isLinked) {
                    discordMeta.setLore(Arrays.asList(
                        "",
                        ChatColor.GREEN + "✔ Compte Lié",
                        ChatColor.GRAY + "Sécurisé par Discord",
                        ""
                    ));
                } else {
                    discordMeta.setLore(Arrays.asList(
                        "",
                        ChatColor.RED + "✖ Compte Non Lié",
                        ChatColor.GRAY + "Liez votre compte Discord",
                        ChatColor.GRAY + "pour sécuriser ce profil.",
                        ""
                    ));
                }
            }
            discordItem.setItemMeta(discordMeta);
        }
        inventory.setItem(15, discordItem);

        // ── Slot 16: Settings (Redstone Torch) ──
        ItemStack settingsItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Paramètres");
            settingsMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Gérez vos préférences",
                ChatColor.GRAY + "et votre confidentialité.",
                ""
            ));
            settingsItem.setItemMeta(settingsMeta);
        }
        inventory.setItem(16, settingsItem);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Play a small click sound for any interactive item
        Material type = clicked.getType();
        if (type == Material.DIAMOND_SWORD || type == Material.NAME_TAG || type == Material.CAKE || type == Material.REDSTONE_TORCH) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            if (type == Material.NAME_TAG) {
                new FriendsMenu(plugin).open(player);
            } else if (type == Material.REDSTONE_TORCH) {
                new SettingsMenu(plugin).open(player);
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Cette fonctionnalité arrive bientôt !");
            }
        }
    }
}
