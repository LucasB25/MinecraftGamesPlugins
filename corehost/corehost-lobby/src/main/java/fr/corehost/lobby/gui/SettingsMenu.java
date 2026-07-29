package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        this.inventory = Bukkit.createInventory(this, 27, "Paramètres");

        if (plugin.getFriendManager() == null) {
            player.sendMessage(ChatColor.RED + "Système d'amis indisponible.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isBlocked = plugin.getFriendManager().areFriendRequestsBlocked(player.getUniqueId());
            boolean notificationsEnabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                ItemStack friendRequestToggle = new ItemStack(isBlocked ? Material.RED_DYE : Material.LIME_DYE);
                ItemMeta toggleMeta = friendRequestToggle.getItemMeta();
                if (toggleMeta != null) {
                    toggleMeta.setDisplayName(ChatColor.GOLD + "Demandes d'amis");
                    List<String> lore = new ArrayList<>();
                    if (isBlocked) {
                        lore.add(ChatColor.RED + "État actuel : Bloqué");
                        lore.add(ChatColor.GRAY + "Personne ne peut vous envoyer");
                        lore.add(ChatColor.GRAY + "de demande d'ami.");
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "► Cliquez pour " + ChatColor.GREEN + "Autoriser");
                    } else {
                        lore.add(ChatColor.GREEN + "État actuel : Autorisé");
                        lore.add(ChatColor.GRAY + "Tout le monde peut vous envoyer");
                        lore.add(ChatColor.GRAY + "des demandes d'amis.");
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "► Cliquez pour " + ChatColor.RED + "Bloquer");
                    }
                    toggleMeta.setLore(lore);
                    friendRequestToggle.setItemMeta(toggleMeta);
                }
                inventory.setItem(11, friendRequestToggle);

                ItemStack notificationsToggle = new ItemStack(notificationsEnabled ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta notifMeta = notificationsToggle.getItemMeta();
                if (notifMeta != null) {
                    notifMeta.setDisplayName(ChatColor.GOLD + "Notifications de Connexion");
                    List<String> lore = new ArrayList<>();
                    if (notificationsEnabled) {
                        lore.add(ChatColor.GREEN + "État actuel : Activé");
                        lore.add(ChatColor.GRAY + "Vous recevrez une alerte quand");
                        lore.add(ChatColor.GRAY + "un ami se connecte.");
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "► Cliquez pour " + ChatColor.RED + "Désactiver");
                    } else {
                        lore.add(ChatColor.RED + "État actuel : Désactivé");
                        lore.add(ChatColor.GRAY + "Vous ne recevrez plus d'alerte");
                        lore.add(ChatColor.GRAY + "quand un ami se connecte.");
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "► Cliquez pour " + ChatColor.GREEN + "Activer");
                    }
                    notifMeta.setLore(lore);
                    notificationsToggle.setItemMeta(notifMeta);
                }
                inventory.setItem(13, notificationsToggle);

                // Placeholder for party invites
                ItemStack partyToggle = new ItemStack(Material.LIME_DYE);
                ItemMeta partyMeta = partyToggle.getItemMeta();
                if (partyMeta != null) {
                    partyMeta.setDisplayName(ChatColor.GOLD + "Invitations de Groupe (Party)");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GREEN + "État actuel : Autorisé");
                    lore.add("");
                    lore.add(ChatColor.GRAY + "Prochainement disponible.");
                    partyMeta.setLore(lore);
                    partyToggle.setItemMeta(partyMeta);
                }
                inventory.setItem(15, partyToggle);

                ItemStack back = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "Retour au Profil");
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

        int slot = event.getSlot();

        if (slot == 22) {
            new PlayerProfileMenu(plugin, player).open(player);
            return;
        }

        if (slot == 11) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean isBlocked = plugin.getFriendManager().areFriendRequestsBlocked(player.getUniqueId());
                plugin.getFriendManager().setFriendRequestsBlocked(player.getUniqueId(), !isBlocked);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.YELLOW + "Paramètres de demandes d'amis mis à jour !");
                    open(player); // Refresh menu
                });
            });
            return;
        }

        if (slot == 13) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean enabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());
                plugin.getFriendManager().setNotificationsEnabled(player.getUniqueId(), !enabled);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.YELLOW + "Paramètres de notifications mis à jour !");
                    open(player); // Refresh menu
                });
            });
            return;
        }
    }
}
