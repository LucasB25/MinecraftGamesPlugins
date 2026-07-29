package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FriendsMenu implements CustomMenu {

    private final CoreHostLobby plugin;
    private Inventory inventory;
    private int page;

    public FriendsMenu(CoreHostLobby plugin) {
        this(plugin, 0);
    }

    public FriendsMenu(CoreHostLobby plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        if (plugin.getFriendManager() == null) {
            player.sendMessage(ChatColor.RED + "Système d'amis indisponible.");
            return;
        }

        this.inventory = Bukkit.createInventory(this, 54,
                ChatColor.DARK_GRAY + "» " + ChatColor.GREEN + "Amis" + ChatColor.DARK_GRAY + " - Page " + (page + 1));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> friendUuids = plugin.getFriendManager().getFriends(player.getUniqueId());
            List<String> friends = new ArrayList<>(friendUuids);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                // ── Bottom bar: Pink + Purple alternating (matches Profile) ──
                ItemStack filler1 = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
                ItemMeta meta1 = filler1.getItemMeta();
                if (meta1 != null) { meta1.setDisplayName(" "); filler1.setItemMeta(meta1); }

                ItemStack filler2 = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
                ItemMeta meta2 = filler2.getItemMeta();
                if (meta2 != null) { meta2.setDisplayName(" "); filler2.setItemMeta(meta2); }

                for (int i = 45; i < 54; i++) {
                    inventory.setItem(i, (i % 2 == 0) ? filler1 : filler2);
                }

                // ── Friend heads ──
                int slot = 0;
                int startIndex = page * 45;
                int endIndex = Math.min(startIndex + 45, friends.size());

                if (friends.isEmpty()) {
                    ItemStack noFriends = new ItemStack(Material.COBWEB);
                    ItemMeta noMeta = noFriends.getItemMeta();
                    if (noMeta != null) {
                        noMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Aucun ami");
                        noMeta.setLore(Arrays.asList(
                            "",
                            ChatColor.GRAY + "Utilisez le bouton " + ChatColor.GREEN + "Ajouter un ami",
                            ChatColor.GRAY + "ou la commande " + ChatColor.YELLOW + "/friend add <pseudo>",
                            ""
                        ));
                        noFriends.setItemMeta(noMeta);
                    }
                    inventory.setItem(22, noFriends);
                }

                for (int i = startIndex; i < endIndex; i++) {
                    String fUuid = friends.get(i);
                    UUID friendId = UUID.fromString(fUuid);
                    String friendName = plugin.getFriendManager().getNameByUuid(friendId);
                    if (friendName == null) friendName = "Inconnu";

                    boolean isOnlineLocally = Bukkit.getPlayer(friendId) != null;

                    String rank = ChatColor.GRAY + "Joueur";
                    String accountType = (friendId.version() == 4) ? ChatColor.GOLD + "Premium" : ChatColor.RED + "Crack";

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName((isOnlineLocally ? ChatColor.GREEN : ChatColor.GRAY) + "" + ChatColor.BOLD + friendName);
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(friendId));

                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Grade : " + rank);
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Compte : " + accountType);
                        if (isOnlineLocally) {
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + ChatColor.GREEN + "En ligne");
                        } else {
                            long lastSeen = plugin.getFriendManager().getLastSeen(friendId);
                            String lastSeenStr;
                            if (lastSeen > 0) {
                                lastSeenStr = ChatColor.YELLOW + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(lastSeen));
                            } else {
                                lastSeenStr = ChatColor.RED + "Inconnue";
                            }
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Dernière connexion : " + lastSeenStr);
                        }
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Clic-Gauche " + ChatColor.GRAY + "Inviter en Party");
                        lore.add(ChatColor.RED + "► Clic-Droit " + ChatColor.GRAY + "Retirer des amis");
                        meta.setLore(lore);
                        
                        org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "friend_uuid");
                        meta.getPersistentDataContainer().set(uuidKey, org.bukkit.persistence.PersistentDataType.STRING, friendId.toString());
                        
                        head.setItemMeta(meta);
                    }
                    inventory.setItem(slot++, head);
                }

                // ── Pagination: Previous Page (slot 45) ──
                if (page > 0) {
                    ItemStack prev = new ItemStack(Material.ARROW);
                    ItemMeta prevMeta = prev.getItemMeta();
                    if (prevMeta != null) {
                        prevMeta.setDisplayName(ChatColor.YELLOW + "◄ Page Précédente");
                        prev.setItemMeta(prevMeta);
                    }
                    inventory.setItem(45, prev);
                }

                // ── Pagination: Next Page (slot 53) ──
                if (endIndex < friends.size()) {
                    ItemStack next = new ItemStack(Material.ARROW);
                    ItemMeta nextMeta = next.getItemMeta();
                    if (nextMeta != null) {
                        nextMeta.setDisplayName(ChatColor.YELLOW + "Page Suivante ►");
                        next.setItemMeta(nextMeta);
                    }
                    inventory.setItem(53, next);
                }

                // ── Info: Friend Count (slot 48) ──
                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = info.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Informations");
                    infoMeta.setLore(Arrays.asList(
                        "",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Amis : " + ChatColor.WHITE + friends.size() + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + "50",
                        ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Page : " + ChatColor.WHITE + (page + 1),
                        ""
                    ));
                    info.setItemMeta(infoMeta);
                }
                inventory.setItem(48, info);

                // ── Back to Profile (slot 49) ──
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "◄ Retour au Profil");
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);

                // ── Add Friend (slot 50) ──
                ItemStack addFriend = new ItemStack(Material.EMERALD);
                ItemMeta addMeta = addFriend.getItemMeta();
                if (addMeta != null) {
                    addMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Ajouter un ami");
                    addMeta.setLore(Arrays.asList(
                        "",
                        ChatColor.GRAY + "Cliquez pour envoyer une",
                        ChatColor.GRAY + "demande d'ami à un joueur.",
                        ""
                    ));
                    addFriend.setItemMeta(addMeta);
                }
                inventory.setItem(50, addFriend);

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

        // ── Previous Page ──
        if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new FriendsMenu(plugin, page - 1).open(player);
            return;
        }

        // ── Next Page ──
        if (slot == 53 && clickedItem.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new FriendsMenu(plugin, page + 1).open(player);
            return;
        }

        // ── Back to Profile ──
        if (slot == 49 && clickedItem.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new PlayerProfileMenu(plugin, player).open(player);
            return;
        }

        // ── Add Friend ──
        if (slot == 50 && clickedItem.getType() == Material.EMERALD) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player.closeInventory();
            fr.corehost.lobby.listeners.LobbyListener.pendingFriendAdd.add(player.getUniqueId());
            String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
            player.sendMessage(prefix + "Tapez le " + ChatColor.GREEN + "pseudo" + ChatColor.GRAY + " de votre ami dans le chat.");
            player.sendMessage(prefix + "Tapez " + ChatColor.YELLOW + "'annuler'" + ChatColor.GRAY + " pour annuler.");
            return;
        }

        // ── Click on a Friend Head ──
        if (clickedItem.getType() == Material.PLAYER_HEAD && slot < 45) {
            String friendName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            ClickType clickType = event.getClick();
            
            org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "friend_uuid");
            String uuidStr = clickedItem.getItemMeta().getPersistentDataContainer().get(uuidKey, org.bukkit.persistence.PersistentDataType.STRING);
            
            if (uuidStr == null) return;
            UUID targetUuid = UUID.fromString(uuidStr);

            if (clickType == ClickType.RIGHT) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
                    String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
                    player.sendMessage(prefix + "Vous n'êtes plus ami avec " + ChatColor.YELLOW + friendName + ChatColor.GRAY + ".");
                    Bukkit.getScheduler().runTask(plugin, () -> new FriendsMenu(plugin, page).open(player));
                });
            } else if (clickType == ClickType.LEFT) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
                player.sendMessage(prefix + "Système de Party bientôt disponible.");
            }
        }
    }
}
