package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import fr.corehost.lobby.utils.Constants;
import fr.corehost.lobby.utils.ItemBuilder;

public class SettingsMenu implements CustomMenu {

    private final CoreHostLobby plugin;
    private Inventory inventory;

    public SettingsMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        open(player, true);
    }

    public void open(Player player, boolean playSound) {
        this.inventory = Bukkit.createInventory(this, 27,
                ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "Paramètres");

        if (plugin.getFriendManager() == null) {
            player.sendMessage(ChatColor.RED + "Système d'amis indisponible.");
            return;
        }

        if (playSound) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isBlocked = plugin.getFriendManager().areFriendRequestsBlocked(player.getUniqueId());
            boolean notificationsEnabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());
            boolean partyBlocked = plugin.getPartyManager().arePartyInvitesBlocked(player.getUniqueId());
            
            boolean pmsBlockedTemp = false;
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                pmsBlockedTemp = "true".equals(plugin.getRedisManager().get("corehost:messages:blocked:" + player.getUniqueId().toString()));
            }
            final boolean pmsBlocked = pmsBlockedTemp;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                // ── Border decoration: Pink + Purple alternating (matches Profile) ──
                MenuUtils.fillBorder(inventory);

                // ── Slot 11: Friend Requests Toggle ──
                ItemStack friendRequestToggle = new ItemBuilder(isBlocked ? Material.RED_DYE : Material.LIME_DYE)
                    .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Demandes d'amis")
                    .setLore(
                        "",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + (isBlocked ? ChatColor.RED + "Bloqué" : ChatColor.GREEN + "Autorisé"),
                        "",
                        isBlocked ? ChatColor.GRAY + "Personne ne peut vous envoyer" : ChatColor.GRAY + "Tout le monde peut vous envoyer",
                        isBlocked ? ChatColor.GRAY + "de demande d'ami." : ChatColor.GRAY + "des demandes d'amis.",
                        "",
                        isBlocked ? ChatColor.GREEN + "► Cliquez pour Autoriser" : ChatColor.RED + "► Cliquez pour Bloquer"
                    ).build();
                inventory.setItem(11, friendRequestToggle);

                // ── Slot 12: Notifications Toggle ──
                ItemStack notificationsToggle = new ItemBuilder(notificationsEnabled ? Material.LIME_DYE : Material.RED_DYE)
                    .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Notifications de Connexion")
                    .setLore(
                        "",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + (notificationsEnabled ? ChatColor.GREEN + "Activé" : ChatColor.RED + "Désactivé"),
                        "",
                        notificationsEnabled ? ChatColor.GRAY + "Vous recevrez une alerte quand" : ChatColor.GRAY + "Vous ne recevrez plus d'alerte",
                        notificationsEnabled ? ChatColor.GRAY + "un ami se connecte." : ChatColor.GRAY + "quand un ami se connecte.",
                        "",
                        notificationsEnabled ? ChatColor.RED + "► Cliquez pour Désactiver" : ChatColor.GREEN + "► Cliquez pour Activer"
                    ).build();
                inventory.setItem(12, notificationsToggle);

                // ── Slot 13: Party Invitations Toggle ──
                ItemStack partyToggle = new ItemBuilder(partyBlocked ? Material.RED_DYE : Material.LIME_DYE)
                    .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Invitations de Groupe")
                    .setLore(
                        "",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + (partyBlocked ? ChatColor.RED + "Bloqué" : ChatColor.GREEN + "Autorisé"),
                        "",
                        partyBlocked ? ChatColor.GRAY + "Personne ne peut vous envoyer" : ChatColor.GRAY + "Tout le monde peut vous envoyer",
                        partyBlocked ? ChatColor.GRAY + "d'invitation de groupe." : ChatColor.GRAY + "des invitations de groupe.",
                        "",
                        partyBlocked ? ChatColor.GREEN + "► Cliquez pour Autoriser" : ChatColor.RED + "► Cliquez pour Bloquer"
                    ).build();
                inventory.setItem(13, partyToggle);

                // ── Slot 14: Private Messages Toggle ──
                ItemStack pmsToggle = new ItemBuilder(pmsBlocked ? Material.RED_DYE : Material.LIME_DYE)
                    .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Messages Privés")
                    .setLore(
                        "",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + (pmsBlocked ? ChatColor.RED + "Bloqué" : ChatColor.GREEN + "Autorisé"),
                        "",
                        pmsBlocked ? ChatColor.GRAY + "Personne ne peut vous envoyer" : ChatColor.GRAY + "Tout le monde peut vous envoyer",
                        pmsBlocked ? ChatColor.GRAY + "de messages privés." : ChatColor.GRAY + "des messages privés.",
                        "",
                        pmsBlocked ? ChatColor.GREEN + "► Cliquez pour Autoriser" : ChatColor.RED + "► Cliquez pour Bloquer"
                    ).build();
                inventory.setItem(14, pmsToggle);

                // ── Slot 15: Ignored Players ──
                ItemStack ignoredPlayers = new ItemBuilder(Material.BARRIER)
                    .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Joueurs Ignorés")
                    .setLore(
                        "",
                        ChatColor.GRAY + "Gérez la liste des joueurs",
                        ChatColor.GRAY + "que vous avez ignorés.",
                        "",
                        ChatColor.YELLOW + "► Cliquez pour Gérer"
                    ).build();
                inventory.setItem(15, ignoredPlayers);

                // ── Slot 22: Back to Profile ──
                inventory.setItem(22, MenuUtils.getBackButton());

                player.openInventory(inventory);
            });
        });
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Ignore glass pane clicks
        if (clickedItem.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getSlot();

        // ── Back to Profile ──
        if (slot == 22) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new PlayerProfileMenu(plugin, player).open(player);
            return;
        }

        // ── Friend Requests Toggle ──
        if (slot == 11) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean isBlocked = plugin.getFriendManager().areFriendRequestsBlocked(player.getUniqueId());
                plugin.getFriendManager().setFriendRequestsBlocked(player.getUniqueId(), !isBlocked);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String prefix = Constants.PREFIX;
                    if (isBlocked) {
                        player.sendMessage(prefix + "Demandes d'amis " + ChatColor.GREEN + "autorisées" + ChatColor.GRAY + ".");
                    } else {
                        player.sendMessage(prefix + "Demandes d'amis " + ChatColor.RED + "bloquées" + ChatColor.GRAY + ".");
                    }
                    open(player, false); // Refresh menu
                });
            });
            return;
        }

        // ── Notifications Toggle ──
        if (slot == 12) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean enabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());
                plugin.getFriendManager().setNotificationsEnabled(player.getUniqueId(), !enabled);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String prefix = Constants.PREFIX;
                    if (enabled) {
                        player.sendMessage(prefix + "Notifications " + ChatColor.RED + "désactivées" + ChatColor.GRAY + ".");
                    } else {
                        player.sendMessage(prefix + "Notifications " + ChatColor.GREEN + "activées" + ChatColor.GRAY + ".");
                    }
                    open(player, false); // Refresh menu
                });
            });
            return;
        }

        // ── Party Invitations Toggle ──
        if (slot == 13) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean partyBlocked = plugin.getPartyManager().arePartyInvitesBlocked(player.getUniqueId());
                plugin.getPartyManager().setPartyInvitesBlocked(player.getUniqueId(), !partyBlocked);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String prefix = Constants.PREFIX;
                    if (partyBlocked) {
                        player.sendMessage(prefix + "Invitations de groupe " + ChatColor.GREEN + "autorisées" + ChatColor.GRAY + ".");
                    } else {
                        player.sendMessage(prefix + "Invitations de groupe " + ChatColor.RED + "bloquées" + ChatColor.GRAY + ".");
                    }
                    open(player, false); // Refresh menu
                });
            });
            return;
        }

        // ── Private Messages Toggle ──
        if (slot == 14) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    boolean pmsBlocked = "true".equals(plugin.getRedisManager().get("corehost:messages:blocked:" + player.getUniqueId().toString()));
                    if (pmsBlocked) {
                        plugin.getRedisManager().del("corehost:messages:blocked:" + player.getUniqueId().toString());
                    } else {
                        plugin.getRedisManager().set("corehost:messages:blocked:" + player.getUniqueId().toString(), "true");
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String prefix = Constants.PREFIX;
                        if (pmsBlocked) {
                            player.sendMessage(prefix + "Messages privés " + ChatColor.GREEN + "autorisés" + ChatColor.GRAY + ".");
                        } else {
                            player.sendMessage(prefix + "Messages privés " + ChatColor.RED + "bloqués" + ChatColor.GRAY + ".");
                        }
                        open(player, false); // Refresh menu
                    });
                }
            });
            return;
        }

        // ── Ignored Players ──
        if (slot == 15) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new IgnoreMenu(plugin, player).open(player);
            return;
        }
    }
}
