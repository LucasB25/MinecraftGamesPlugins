package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

import fr.corehost.lobby.CoreHostLobby;

public class PlayerProfileMenu implements CustomMenu {

    private Inventory inventory;
    private final CoreHostLobby plugin;
    private final Player player;

    public PlayerProfileMenu(CoreHostLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    private void initializeItems(boolean isLinked) {
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
            partyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Groupe (Party)");
            partyMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Gérez votre groupe,",
                ChatColor.GRAY + "invitez des joueurs et",
                ChatColor.GRAY + "jouez ensemble !",
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
                        "",
                        ChatColor.GREEN + "► Cliquez pour lier"
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean linked = false;
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                String discordId = plugin.getRedisManager().get("corehost:discord_link:player:" + player.getUniqueId().toString());
                if (discordId != null) linked = true;
            }
            
            final boolean isLinked = linked;
            Bukkit.getScheduler().runTask(plugin, () -> {
                this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.LIGHT_PURPLE + "Profil de " + player.getName());
                initializeItems(isLinked);
                player.openInventory(inventory);
            });
        });
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
            if (type == Material.NAME_TAG) {
                new FriendsMenu(plugin).open(player);
            } else if (type == Material.CAKE) {
                new PartyMenu(plugin).open(player);
            } else if (type == Material.REDSTONE_TORCH) {
                new SettingsMenu(plugin).open(player);
            } else {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Cette fonctionnalité arrive bientôt !");
            }
        }

        // ── Link Crack (Sécurité du Compte — slot 15) ──
        if (type == Material.LAPIS_LAZULI && event.getSlot() == 15) {

            // Premium account — no link needed
            if (player.getUniqueId().version() == 4) {
                return;
            }

            player.closeInventory();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // Check if already linked
                boolean linked = false;
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    String discordId = plugin.getRedisManager().get("corehost:discord_link:player:" + player.getUniqueId().toString());
                    if (discordId != null) linked = true;
                }

                if (linked) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GREEN + "Votre compte est déjà lié à Discord !");
                    });
                    return;
                }

                // Generate a 6-digit link code
                String code = String.format("%06d", new java.util.Random().nextInt(1000000));

                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    plugin.getRedisManager().setEx("corehost:discord_link:code:" + code, player.getUniqueId().toString(), 900);
                }

                // Retrieve Discord bot ID for the link
                String botId = null;
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    botId = plugin.getRedisManager().get("corehost:discord_bot_id");
                }
                String discordUrl = (botId != null && !botId.isEmpty()) ? "https://discord.com/users/" + botId : "https://discord.gg/";

                final String finalDiscordUrl = discordUrl;
                final String finalCode = code;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

                    player.sendMessage("");
                    player.sendMessage(prefix + ChatColor.YELLOW + "Liaison de compte Discord");
                    player.sendMessage("");
                    player.sendMessage(prefix + "Votre code de liaison : " + ChatColor.GOLD + "" + ChatColor.BOLD + finalCode);
                    player.sendMessage(prefix + ChatColor.GRAY + "Ce code expire dans " + ChatColor.YELLOW + "15 minutes" + ChatColor.GRAY + ".");
                    player.sendMessage("");

                    net.md_5.bungee.api.chat.TextComponent clickMsg = new net.md_5.bungee.api.chat.TextComponent(
                        net.md_5.bungee.api.ChatColor.DARK_GRAY + "[" + net.md_5.bungee.api.ChatColor.GOLD + "CoreHost" + net.md_5.bungee.api.ChatColor.DARK_GRAY + "] " +
                        net.md_5.bungee.api.ChatColor.AQUA + "" + net.md_5.bungee.api.ChatColor.BOLD + "👉 Cliquez ici pour ouvrir le profil du Bot 👈"
                    );
                    clickMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, finalDiscordUrl));
                    clickMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.YELLOW + "Ouvrir Discord pour envoyer un MP")));
                    player.spigot().sendMessage(clickMsg);

                    player.sendMessage("");
                    player.sendMessage(prefix + "Envoyez le code " + ChatColor.GOLD + finalCode + ChatColor.GRAY + " en MP au Bot Discord !");
                    player.sendMessage("");
                });
            });
        }
    }
}
