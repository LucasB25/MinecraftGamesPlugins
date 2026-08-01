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
import fr.corehost.lobby.utils.Constants;

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

            // Fetch online status for all friends to sort them properly
            java.util.Map<UUID, Boolean> friendOnline = new java.util.HashMap<>();
            for (String fUuid : friends) {
                UUID friendId = UUID.fromString(fUuid);
                friendOnline.put(friendId, plugin.getFriendManager().isOnline(friendId));
            }

            // Sort friends: online first
            friends.sort((u1, u2) -> {
                boolean o1 = friendOnline.getOrDefault(UUID.fromString(u1), false);
                boolean o2 = friendOnline.getOrDefault(UUID.fromString(u2), false);
                return Boolean.compare(o2, o1);
            });

            // Preload data asynchronously to avoid lag spikes
            java.util.Map<UUID, String> friendNames = new java.util.HashMap<>();
            java.util.Map<UUID, Long> friendLastSeen = new java.util.HashMap<>();

            int asyncStart = page * 45;
            int asyncEnd = Math.min(asyncStart + 45, friends.size());

            for (int i = asyncStart; i < asyncEnd; i++) {
                UUID friendId = UUID.fromString(friends.get(i));
                String name = plugin.getFriendManager().getNameByUuid(friendId);
                friendNames.put(friendId, name != null ? name : "Inconnu");
                if (!friendOnline.get(friendId)) {
                    friendLastSeen.put(friendId, plugin.getFriendManager().getLastSeen(friendId));
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                // ── Bottom bar: Pink + Purple alternating (matches Profile) ──
                MenuUtils.fillBottomRow(inventory);

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
                    String friendName = friendNames.getOrDefault(friendId, "Inconnu");

                    boolean isOnlineNetwork = friendOnline.getOrDefault(friendId, false);

                    String rank = ChatColor.GRAY + "Joueur";
                    String accountType = (friendId.version() == 4) ? ChatColor.GOLD + "Premium" : ChatColor.RED + "Crack";

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName((isOnlineNetwork ? ChatColor.GREEN : ChatColor.GRAY) + "" + ChatColor.BOLD + friendName);
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(friendId));

                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Grade : " + rank);
                        lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Compte : " + accountType);
                        if (isOnlineNetwork) {
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + ChatColor.GREEN + "En ligne");
                        } else {
                            long lastSeen = friendLastSeen.getOrDefault(friendId, 0L);
                            String lastSeenStr;
                            if (lastSeen > 0) {
                                lastSeenStr = ChatColor.YELLOW + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(lastSeen));
                            } else {
                                lastSeenStr = ChatColor.RED + "Inconnue";
                            }
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Dernière connexion : " + lastSeenStr);
                        }
                        lore.add("");
                        UUID myLeader = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
                        boolean canInvite = (myLeader == null || myLeader.equals(player.getUniqueId()));
                        UUID friendLeader = plugin.getPartyManager().getPartyLeader(friendId);
                        boolean isFriendInParty = (friendLeader != null);

                        if (canInvite && !isFriendInParty) {
                            lore.add(ChatColor.GREEN + "► Clic-Gauche " + ChatColor.GRAY + "Inviter en Party");
                        }
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
                    inventory.setItem(45, MenuUtils.getPrevPageButton());
                }

                // ── Pagination: Next Page (slot 53) ──
                if (endIndex < friends.size()) {
                    inventory.setItem(53, MenuUtils.getNextPageButton());
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
                inventory.setItem(49, MenuUtils.getBackButton());

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
            String prefix = Constants.PREFIX;
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
                    String prefix = Constants.PREFIX;
                    player.sendMessage(prefix + "Vous n'êtes plus ami avec " + ChatColor.YELLOW + friendName + ChatColor.GRAY + ".");
                    Bukkit.getScheduler().runTask(plugin, () -> new FriendsMenu(plugin, page).open(player));
                });
            } else if (clickType == ClickType.LEFT) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    UUID myLeader = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
                    if (myLeader != null && !myLeader.equals(player.getUniqueId())) {
                        String prefix = Constants.BUNGEE_PREFIX;
                        player.sendMessage(prefix + net.md_5.bungee.api.ChatColor.RED + "Seul le chef de groupe peut inviter des joueurs.");
                        return;
                    }

                    UUID targetLeader = plugin.getPartyManager().getPartyLeader(targetUuid);
                    if (targetLeader != null) {
                        String prefix = Constants.BUNGEE_PREFIX;
                        player.sendMessage(prefix + net.md_5.bungee.api.ChatColor.RED + "Ce joueur est déjà dans un groupe.");
                        return;
                    }

                    if (!plugin.getFriendManager().isOnline(targetUuid)) {
                        String prefix = Constants.BUNGEE_PREFIX;
                        player.sendMessage(prefix + net.md_5.bungee.api.ChatColor.RED + "Ce joueur est hors ligne.");
                        return;
                    }
                    
                    String prefix = Constants.BUNGEE_PREFIX;
                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour inviter " + net.md_5.bungee.api.ChatColor.YELLOW + friendName + net.md_5.bungee.api.ChatColor.GRAY + " dans votre groupe !");
                    message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party invite " + friendName));
                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.GREEN + "Cliquez pour inviter")));
                    
                    player.spigot().sendMessage(message);
                });
            }
        }
    }
}
