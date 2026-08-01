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

public class PartyMenu implements CustomMenu {

    private final CoreHostLobby plugin;
    private Inventory inventory;

    public PartyMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        if (plugin.getPartyManager() == null) {
            player.sendMessage(ChatColor.RED + "Système de groupe indisponible.");
            return;
        }

        this.inventory = Bukkit.createInventory(this, 54,
                ChatColor.DARK_GRAY + "» " + ChatColor.LIGHT_PURPLE + "Groupe (Party)");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID playerUuid = player.getUniqueId();
            UUID leaderUuid = plugin.getPartyManager().getPartyLeader(playerUuid);
            
            List<String> members = new ArrayList<>();
            java.util.Map<UUID, String> memberNames = new java.util.HashMap<>();

            if (leaderUuid != null) {
                Set<UUID> memberUuids = plugin.getPartyManager().getPartyMembers(leaderUuid);
                
                if (memberUuids.contains(leaderUuid)) {
                    members.add(leaderUuid.toString());
                }
                for (UUID uuid : memberUuids) {
                    if (!uuid.equals(leaderUuid)) {
                        members.add(uuid.toString());
                    }
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) {
                        name = plugin.getFriendManager().getNameByUuid(uuid);
                    }
                    memberNames.put(uuid, name != null ? name : "Inconnu");
                }
            }

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

                boolean isLeader = leaderUuid != null && leaderUuid.equals(playerUuid);

                if (leaderUuid == null) {
                    ItemStack noParty = new ItemStack(Material.COBWEB);
                    ItemMeta noMeta = noParty.getItemMeta();
                    if (noMeta != null) {
                        noMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Aucun Groupe");
                        noMeta.setLore(Arrays.asList(
                            "",
                            ChatColor.GRAY + "Vous n'êtes dans aucun groupe.",
                            ChatColor.GRAY + "Invitez un joueur avec",
                            ChatColor.YELLOW + "/party invite <pseudo>",
                            ""
                        ));
                        noParty.setItemMeta(noMeta);
                    }
                    inventory.setItem(22, noParty);
                } else {
                    int slot = 0;

                    for (String mUuidStr : members) {
                        UUID memberId = UUID.fromString(mUuidStr);
                        String memberName = memberNames.getOrDefault(memberId, "Inconnu");

                        boolean isMemberLeader = memberId.equals(leaderUuid);

                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) head.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName((isMemberLeader ? ChatColor.GOLD : ChatColor.GREEN) + "" + ChatColor.BOLD + memberName);
                            meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));

                            List<String> lore = new ArrayList<>();
                            lore.add("");
                            lore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Rôle : " + (isMemberLeader ? ChatColor.GOLD + "Chef" : ChatColor.GRAY + "Membre"));
                            lore.add("");
                            
                            if (isLeader && !isMemberLeader) {
                                lore.add(ChatColor.RED + "► Clic-Droit " + ChatColor.GRAY + "Expulser du groupe");
                            }
                            
                            meta.setLore(lore);
                            
                            org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "party_uuid");
                            meta.getPersistentDataContainer().set(uuidKey, org.bukkit.persistence.PersistentDataType.STRING, memberId.toString());
                            
                            head.setItemMeta(meta);
                        }
                        inventory.setItem(slot++, head);
                    }
                }

                // ── Info: Party Info (slot 48) — matches FriendsMenu ──
                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = info.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Informations");
                    List<String> infoLore = new ArrayList<>();
                    infoLore.add("");
                    if (leaderUuid == null) {
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Groupe : " + ChatColor.RED + "Aucun");
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Rôle : " + ChatColor.GRAY + "—");
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Membres : " + ChatColor.WHITE + "0");
                    } else {
                        String leaderName = memberNames.getOrDefault(leaderUuid, "Inconnu");
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Chef : " + ChatColor.GOLD + leaderName);
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Rôle : " + (isLeader ? ChatColor.GOLD + "Chef" : ChatColor.GREEN + "Membre"));
                        infoLore.add(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Membres : " + ChatColor.WHITE + members.size());
                    }
                    infoLore.add("");
                    infoMeta.setLore(infoLore);
                    info.setItemMeta(infoMeta);
                }
                inventory.setItem(48, info);

                // ── Back to Profile (slot 49) — matches FriendsMenu ──
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "◄ Retour au Profil");
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);

                // ── Invite Player (slot 50) — matches FriendsMenu "Ajouter un ami" ──
                if (isLeader) {
                    ItemStack inviteItem = new ItemStack(Material.EMERALD);
                    ItemMeta inviteMeta = inviteItem.getItemMeta();
                    if (inviteMeta != null) {
                        inviteMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Inviter un joueur");
                        inviteMeta.setLore(Arrays.asList(
                            "",
                            ChatColor.GRAY + "Cliquez pour inviter un",
                            ChatColor.GRAY + "joueur dans votre groupe.",
                            ""
                        ));
                        inviteItem.setItemMeta(inviteMeta);
                    }
                    inventory.setItem(50, inviteItem);
                }

                // ── Leave / Disband Party (slot 51) ──
                if (leaderUuid != null) {
                    ItemStack leaveItem = new ItemStack(Material.BARRIER);
                    ItemMeta leaveMeta = leaveItem.getItemMeta();
                    if (leaveMeta != null) {
                        if (isLeader) {
                            leaveMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Dissoudre le groupe");
                            leaveMeta.setLore(Arrays.asList("", ChatColor.GRAY + "Cliquez pour dissoudre", ChatColor.GRAY + "votre groupe.", ""));
                        } else {
                            leaveMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Quitter le groupe");
                            leaveMeta.setLore(Arrays.asList("", ChatColor.GRAY + "Cliquez pour quitter", ChatColor.GRAY + "le groupe actuel.", ""));
                        }
                        leaveItem.setItemMeta(leaveMeta);
                    }
                    inventory.setItem(51, leaveItem);
                }

                player.openInventory(inventory);
            });
        });
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getSlot();

        // ── Back to Profile ──
        if (slot == 49 && clickedItem.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new PlayerProfileMenu(plugin, player).open(player);
            return;
        }

        // ── Invite Player ──
        if (slot == 50 && clickedItem.getType() == Material.EMERALD) {
            UUID leaderUuid = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
            if (leaderUuid != null && !leaderUuid.equals(player.getUniqueId())) {
                String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
                player.sendMessage(prefix + ChatColor.RED + "Seul le chef de groupe peut inviter des joueurs.");
                player.closeInventory();
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player.closeInventory();
            // Add player to pending party invite list so LobbyListener catches their next chat message
            fr.corehost.lobby.listeners.LobbyListener.pendingPartyInvite.add(player.getUniqueId());
            String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
            player.sendMessage(prefix + "Tapez le " + ChatColor.GREEN + "pseudo" + ChatColor.GRAY + " du joueur à inviter dans le chat.");
            player.sendMessage(prefix + "Tapez " + ChatColor.YELLOW + "'annuler'" + ChatColor.GRAY + " pour annuler.");
            return;
        }

        // ── Leave / Disband ──
        if (slot == 51 && clickedItem.getType() == Material.BARRIER) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            String prefix = net.md_5.bungee.api.ChatColor.DARK_GRAY + "[" + net.md_5.bungee.api.ChatColor.GOLD + "CoreHost" + net.md_5.bungee.api.ChatColor.DARK_GRAY + "] " + net.md_5.bungee.api.ChatColor.GRAY;
            UUID leaderUuid = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
            if (leaderUuid != null && leaderUuid.equals(player.getUniqueId())) {
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour " + net.md_5.bungee.api.ChatColor.RED + "dissoudre le groupe" + net.md_5.bungee.api.ChatColor.GRAY + " !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party disband"));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.RED + "Cliquez pour dissoudre")));
                player.spigot().sendMessage(msg);
            } else {
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour " + net.md_5.bungee.api.ChatColor.RED + "quitter le groupe" + net.md_5.bungee.api.ChatColor.GRAY + " !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party leave"));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.RED + "Cliquez pour quitter")));
                player.spigot().sendMessage(msg);
            }
            return;
        }

        // ── Click on a Member Head ──
        if (clickedItem.getType() == Material.PLAYER_HEAD && slot < 45) {
            ClickType clickType = event.getClick();
            
            org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "party_uuid");
            String uuidStr = clickedItem.getItemMeta().getPersistentDataContainer().get(uuidKey, org.bukkit.persistence.PersistentDataType.STRING);
            
            if (uuidStr == null) return;
            UUID targetUuid = UUID.fromString(uuidStr);
            String targetName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (clickType == ClickType.RIGHT) {
                // Kick from party via proxy command
                UUID leaderUuid = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
                if (leaderUuid != null && leaderUuid.equals(player.getUniqueId()) && !player.getUniqueId().equals(targetUuid)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.closeInventory();
                    String prefix = net.md_5.bungee.api.ChatColor.DARK_GRAY + "[" + net.md_5.bungee.api.ChatColor.GOLD + "CoreHost" + net.md_5.bungee.api.ChatColor.DARK_GRAY + "] " + net.md_5.bungee.api.ChatColor.GRAY;
                    net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour expulser " + net.md_5.bungee.api.ChatColor.YELLOW + targetName + net.md_5.bungee.api.ChatColor.GRAY + " du groupe !");
                    msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party kick " + targetName));
                    msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.RED + "Cliquez pour expulser")));
                    player.spigot().sendMessage(msg);
                }
            }
        }
    }
}
