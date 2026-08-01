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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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

                // ── Slot 11: Friend Requests Toggle ──
                ItemStack friendRequestToggle = new ItemStack(isBlocked ? Material.RED_DYE : Material.LIME_DYE);
                ItemMeta toggleMeta = friendRequestToggle.getItemMeta();
                if (toggleMeta != null) {
                    toggleMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Demandes d'amis");
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    if (isBlocked) {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.RED + "Bloqué");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Personne ne peut vous envoyer");
                        lore.add(ChatColor.GRAY + "de demande d'ami.");
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Cliquez pour Autoriser");
                    } else {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.GREEN + "Autorisé");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Tout le monde peut vous envoyer");
                        lore.add(ChatColor.GRAY + "des demandes d'amis.");
                        lore.add("");
                        lore.add(ChatColor.RED + "► Cliquez pour Bloquer");
                    }
                    toggleMeta.setLore(lore);
                    friendRequestToggle.setItemMeta(toggleMeta);
                }
                inventory.setItem(11, friendRequestToggle);

                // ── Slot 12: Notifications Toggle ──
                ItemStack notificationsToggle = new ItemStack(notificationsEnabled ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta notifMeta = notificationsToggle.getItemMeta();
                if (notifMeta != null) {
                    notifMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Notifications de Connexion");
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    if (notificationsEnabled) {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.GREEN + "Activé");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Vous recevrez une alerte quand");
                        lore.add(ChatColor.GRAY + "un ami se connecte.");
                        lore.add("");
                        lore.add(ChatColor.RED + "► Cliquez pour Désactiver");
                    } else {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.RED + "Désactivé");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Vous ne recevrez plus d'alerte");
                        lore.add(ChatColor.GRAY + "quand un ami se connecte.");
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Cliquez pour Activer");
                    }
                    notifMeta.setLore(lore);
                    notificationsToggle.setItemMeta(notifMeta);
                }
                inventory.setItem(12, notificationsToggle);

                // ── Slot 13: Party Invitations Toggle ──
                ItemStack partyToggle = new ItemStack(partyBlocked ? Material.RED_DYE : Material.LIME_DYE);
                ItemMeta partyMeta = partyToggle.getItemMeta();
                if (partyMeta != null) {
                    partyMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Invitations de Groupe");
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    if (partyBlocked) {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.RED + "Bloqué");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Personne ne peut vous envoyer");
                        lore.add(ChatColor.GRAY + "d'invitation de groupe.");
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Cliquez pour Autoriser");
                    } else {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.GREEN + "Autorisé");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Tout le monde peut vous envoyer");
                        lore.add(ChatColor.GRAY + "des invitations de groupe.");
                        lore.add("");
                        lore.add(ChatColor.RED + "► Cliquez pour Bloquer");
                    }
                    partyMeta.setLore(lore);
                    partyToggle.setItemMeta(partyMeta);
                }
                inventory.setItem(13, partyToggle);

                // ── Slot 14: Private Messages Toggle ──
                ItemStack pmsToggle = new ItemStack(pmsBlocked ? Material.RED_DYE : Material.LIME_DYE);
                ItemMeta pmsMeta = pmsToggle.getItemMeta();
                if (pmsMeta != null) {
                    pmsMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Messages Privés");
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    if (pmsBlocked) {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.RED + "Bloqué");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Personne ne peut vous envoyer");
                        lore.add(ChatColor.GRAY + "de messages privés.");
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Cliquez pour Autoriser");
                    } else {
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "État : " + ChatColor.GREEN + "Autorisé");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Tout le monde peut vous envoyer");
                        lore.add(ChatColor.GRAY + "des messages privés.");
                        lore.add("");
                        lore.add(ChatColor.RED + "► Cliquez pour Bloquer");
                    }
                    pmsMeta.setLore(lore);
                    pmsToggle.setItemMeta(pmsMeta);
                }
                inventory.setItem(14, pmsToggle);

                // ── Slot 15: Ignored Players ──
                ItemStack ignoredPlayers = new ItemStack(Material.BARRIER);
                ItemMeta ignoredMeta = ignoredPlayers.getItemMeta();
                if (ignoredMeta != null) {
                    ignoredMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Joueurs Ignorés");
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(ChatColor.GRAY + "Gérez la liste des joueurs");
                    lore.add(ChatColor.GRAY + "que vous avez ignorés.");
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "► Cliquez pour Gérer");
                    ignoredMeta.setLore(lore);
                    ignoredPlayers.setItemMeta(ignoredMeta);
                }
                inventory.setItem(15, ignoredPlayers);

                // ── Slot 22: Back to Profile ──
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "◄ Retour au Profil");
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(22, back);

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
                    String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
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
                    String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
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
                    String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
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
                        String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
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
