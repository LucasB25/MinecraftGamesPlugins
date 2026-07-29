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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.inventory.ClickType;

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

        this.inventory = Bukkit.createInventory(this, 54, "Liste d'Amis - Page " + (page + 1));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> friendUuids = plugin.getFriendManager().getFriends(player.getUniqueId());
            List<String> friends = new ArrayList<>(friendUuids);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                int slot = 0;
                int startIndex = page * 45;
                int endIndex = Math.min(startIndex + 45, friends.size());

                for (int i = startIndex; i < endIndex; i++) {
                    String fUuid = friends.get(i);
                    UUID friendId = UUID.fromString(fUuid);
                    String friendName = plugin.getFriendManager().getNameByUuid(friendId);
                    if (friendName == null) friendName = "Inconnu";

                    boolean isOnlineLocally = Bukkit.getPlayer(friendId) != null;
                    
                    // Default Rank for now (could be hooked into LuckPerms later)
                    String rank = ChatColor.GRAY + "Joueur";
                    long lastSeen = plugin.getFriendManager().getLastSeen(friendId);
                    String lastSeenStr;
                    if (isOnlineLocally) {
                        lastSeenStr = ChatColor.GREEN + "Maintenant";
                    } else if (lastSeen > 0) {
                        lastSeenStr = ChatColor.YELLOW + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(lastSeen));
                    } else {
                        lastSeenStr = ChatColor.RED + "Inconnue";
                    }

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GOLD + friendName);
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(friendId));
                        
                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Grade : " + rank);
                        if (isOnlineLocally) {
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + ChatColor.GREEN + "En ligne (Ici)");
                        } else {
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + ChatColor.RED + "Hors ligne ou autre serveur");
                        }
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Dernière connexion : " + lastSeenStr);
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Clic-Gauche pour inviter en Party");
                        lore.add(ChatColor.GRAY + "Clic-Droit pour retirer des amis");
                        meta.setLore(lore);
                        head.setItemMeta(meta);
                    }
                    inventory.setItem(slot++, head);
                }

                // Pagination Controls
                if (page > 0) {
                    ItemStack prev = new ItemStack(Material.ARROW);
                    ItemMeta prevMeta = prev.getItemMeta();
                    if (prevMeta != null) {
                        prevMeta.setDisplayName(ChatColor.YELLOW + "Page Précédente");
                        prev.setItemMeta(prevMeta);
                    }
                    inventory.setItem(45, prev);
                }

                if (endIndex < friends.size()) {
                    ItemStack next = new ItemStack(Material.ARROW);
                    ItemMeta nextMeta = next.getItemMeta();
                    if (nextMeta != null) {
                        nextMeta.setDisplayName(ChatColor.YELLOW + "Page Suivante");
                        next.setItemMeta(nextMeta);
                    }
                    inventory.setItem(53, next);
                }

                ItemStack back = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "Retour au Profil");
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);

                ItemStack addFriend = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta addMeta = addFriend.getItemMeta();
                if (addMeta != null) {
                    addMeta.setDisplayName(ChatColor.GREEN + "Ajouter un ami");
                    List<String> addLore = new ArrayList<>();
                    addLore.add(ChatColor.GRAY + "Cliquez pour envoyer une");
                    addLore.add(ChatColor.GRAY + "demande d'ami à un joueur.");
                    addMeta.setLore(addLore);
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
        
        int slot = event.getSlot();

        if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            new FriendsMenu(plugin, page - 1).open(player);
            return;
        }

        if (slot == 53 && clickedItem.getType() == Material.ARROW) {
            new FriendsMenu(plugin, page + 1).open(player);
            return;
        }

        if (slot == 49) {
            new PlayerProfileMenu(plugin, player).open(player);
            return;
        }
        
        if (slot == 50) {
            player.closeInventory();
            fr.corehost.lobby.listeners.LobbyListener.pendingFriendAdd.add(player.getUniqueId());
            player.sendMessage(ChatColor.AQUA + "===============================");
            player.sendMessage(ChatColor.GREEN + "► Tapez le pseudo de votre ami dans le chat.");
            player.sendMessage(ChatColor.GRAY + "► Tapez 'annuler' pour annuler.");
            player.sendMessage(ChatColor.AQUA + "===============================");
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            String friendName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            ClickType clickType = event.getClick();
            
            if (clickType == ClickType.RIGHT) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    UUID targetUuid = plugin.getFriendManager().getUuidByName(friendName);
                    if (targetUuid != null) {
                        plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
                        player.sendMessage(ChatColor.YELLOW + "Vous n'êtes plus ami avec " + friendName + ".");
                        Bukkit.getScheduler().runTask(plugin, () -> new FriendsMenu(plugin, page).open(player)); // Refresh
                    }
                });
            } else if (clickType == ClickType.LEFT) {
                player.sendMessage(ChatColor.GRAY + "Système de Party bientôt disponible.");
            }
        }
    }
}
