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
import java.util.Arrays;
import java.util.List;
import fr.corehost.lobby.utils.Constants;
import fr.corehost.lobby.utils.ItemBuilder;
import fr.corehost.lobby.CoreHostLobby;

public class PlayerProfileMenu implements CustomMenu {

    private Inventory inventory;
    private final CoreHostLobby plugin;
    private final Player player;

    public PlayerProfileMenu(CoreHostLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    private void initializeItems(boolean isLinked, int coins) {
        // Border decoration (Pink + Purple alternating)
        LobbyMenuUtils.fillBorder(inventory);

        long firstPlayed = player.getFirstPlayed();
        String firstPlayedDate = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(firstPlayed));
        String accountType = (player.getUniqueId().version() == 4) ? ChatColor.GOLD + "Premium" : ChatColor.RED + "Crack";

        // ── Slot 10: Player Head (General Info) ──
        ItemStack headItem = new ItemBuilder(Material.PLAYER_HEAD)
            .setSkullOwner(player.getName()) // ItemBuilder uses setOwner properly
            .setName(ChatColor.AQUA + "" + ChatColor.BOLD + player.getName())
            .setLore(
                "",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Grade : " + fr.corehost.lobby.utils.LuckPermsHook.getPlayerPrefix(player),
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Compte : " + accountType,
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Coins : " + ChatColor.GOLD + coins + " ⛃",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Première connexion : " + ChatColor.WHITE + firstPlayedDate,
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Ping : " + ChatColor.YELLOW + player.getPing() + "ms",
                ""
            ).build();
        inventory.setItem(10, headItem);

        // ── Slot 11: Game Stats (Sword) ──
        ItemStack statsItem = new ItemBuilder(Material.DIAMOND_SWORD)
            .setName(ChatColor.RED + "" + ChatColor.BOLD + "Statistiques de Jeu")
            .setLore(
                "",
                ChatColor.GRAY + "Cliquez pour voir vos",
                ChatColor.GRAY + "statistiques dans les",
                ChatColor.GRAY + "différents mini-jeux.",
                ""
            ).build();
        
        // Hide attributes (like +7 Attack Damage)
        ItemMeta meta = statsItem.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            statsItem.setItemMeta(meta);
        }
        inventory.setItem(11, statsItem);

        // ── Slot 12: Friends (Name Tag) ──
        ItemStack friendsItem = new ItemBuilder(Material.NAME_TAG)
            .setName(ChatColor.GREEN + "" + ChatColor.BOLD + "Amis")
            .setLore(
                "",
                ChatColor.GRAY + "Gérez votre liste d'amis",
                ChatColor.GRAY + "et voyez qui est en ligne.",
                ""
            ).build();
        inventory.setItem(12, friendsItem);

        // ── Slot 13: Empty (glass pane) ──
        inventory.setItem(13, LobbyMenuUtils.getPurpleFiller());

        // ── Slot 14: Party (Cake) ──
        ItemStack partyItem = new ItemBuilder(Material.CAKE)
            .setName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Groupe (Party)")
            .setLore(
                "",
                ChatColor.GRAY + "Gérez votre groupe,",
                ChatColor.GRAY + "invitez des joueurs et",
                ChatColor.GRAY + "jouez ensemble !",
                ""
            ).build();
        inventory.setItem(14, partyItem);

        List<String> discordLore;
        if (player.getUniqueId().version() == 4) {
            discordLore = Arrays.asList(
                "",
                ChatColor.GREEN + "✔ Compte Premium",
                ChatColor.GRAY + "Sécurisé par Mojang",
                ""
            );
        } else {
            if (isLinked) {
                discordLore = Arrays.asList(
                    "",
                    ChatColor.GREEN + "✔ Compte Lié",
                    ChatColor.GRAY + "Sécurisé par Discord",
                    ""
                );
            } else {
                discordLore = Arrays.asList(
                    "",
                    ChatColor.RED + "✖ Compte Non Lié",
                    ChatColor.GRAY + "Liez votre compte Discord",
                    ChatColor.GRAY + "pour sécuriser ce profil.",
                    "",
                    ChatColor.GREEN + "► Cliquez pour lier"
                );
            }
        }
        
        ItemStack discordItem = new ItemBuilder(Material.LAPIS_LAZULI)
            .setName(ChatColor.BLUE + "" + ChatColor.BOLD + "Sécurité du Compte")
            .setLore(discordLore).build();
        inventory.setItem(15, discordItem);

        // ── Slot 16: Settings (Redstone Torch) ──
        ItemStack settingsItem = new ItemBuilder(Material.REDSTONE_TORCH)
            .setName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Paramètres")
            .setLore(
                "",
                ChatColor.GRAY + "Gérez vos préférences",
                ChatColor.GRAY + "et votre confidentialité.",
                ""
            ).build();
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
            
            int fetchedCoins = 0;
            if (plugin.getProfileManager() != null) {
                fr.corehost.api.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                if (profile != null) {
                    fetchedCoins = profile.getCoins();
                }
            }
            final int coins = fetchedCoins;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.LIGHT_PURPLE + "Profil de " + player.getName());
                initializeItems(isLinked, coins);
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
                player.sendMessage(Constants.PREFIX + "Cette fonctionnalité arrive bientôt !");
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
                        player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Votre compte est déjà lié à Discord !");
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
                String discordUrl = (botId != null && !botId.isEmpty()) ? "https://discord.com/users/" + botId : plugin.getConfig().getString("settings.discord-url", "https://discord.gg/corehost");

                final String finalDiscordUrl = discordUrl;
                final String finalCode = code;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    String prefix = Constants.PREFIX;

                    player.sendMessage("");
                    player.sendMessage(prefix + ChatColor.YELLOW + "Liaison de compte Discord");
                    player.sendMessage("");
                    player.sendMessage(prefix + "Votre code de liaison : " + ChatColor.GOLD + "" + ChatColor.BOLD + finalCode);
                    player.sendMessage(prefix + ChatColor.GRAY + "Ce code expire dans " + ChatColor.YELLOW + "15 minutes" + ChatColor.GRAY + ".");
                    net.md_5.bungee.api.chat.TextComponent clickMsg = new net.md_5.bungee.api.chat.TextComponent(
                        Constants.BUNGEE_PREFIX +
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
