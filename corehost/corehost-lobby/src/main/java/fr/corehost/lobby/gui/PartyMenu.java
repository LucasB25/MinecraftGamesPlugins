package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import fr.corehost.lobby.utils.Constants;
import fr.corehost.lobby.utils.ItemBuilder;

import java.util.ArrayList;
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
            player.sendMessage(CC.RED + "Système de groupe indisponible.");
            return;
        }

        this.inventory = Bukkit.createInventory(this, 54,
                CC.DARK_GRAY + "» " + CC.LIGHT_PURPLE + "Groupe (Party)");

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
                LobbyMenuUtils.fillBottomRow(inventory);

                boolean isLeader = leaderUuid != null && leaderUuid.equals(playerUuid);

                if (leaderUuid == null) {
                    ItemStack noParty = new ItemBuilder(Material.COBWEB)
                        .setName(CC.YELLOW + "" + CC.BOLD + "Aucun Groupe")
                        .setLore(
                            "",
                            CC.GRAY + "Vous n'êtes dans aucun groupe.",
                            CC.GRAY + "Invitez un joueur avec",
                            CC.YELLOW + "/party invite <pseudo>",
                            ""
                        ).build();
                    inventory.setItem(22, noParty);
                } else {
                    int slot = 0;

                    for (String mUuidStr : members) {
                        UUID memberId = UUID.fromString(mUuidStr);
                        String memberName = memberNames.getOrDefault(memberId, "Inconnu");

                        boolean isMemberLeader = memberId.equals(leaderUuid);

                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Rôle : " + (isMemberLeader ? CC.GOLD + "Chef" : CC.GRAY + "Membre"));
                        lore.add("");
                        
                        if (isLeader && !isMemberLeader) {
                            lore.add(CC.RED + "► Clic-Droit " + CC.GRAY + "Expulser du groupe");
                        }
                        
                        org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(plugin, "party_uuid");
                        
                        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                            .setName((isMemberLeader ? CC.GOLD : CC.GREEN) + "" + CC.BOLD + memberName)
                            .setSkullOwner(Bukkit.getOfflinePlayer(memberId).getName())
                            .setLore(lore)
                            .addPersistentData(uuidKey, org.bukkit.persistence.PersistentDataType.STRING, memberId.toString())
                            .build();

                        inventory.setItem(slot++, head);
                    }
                }

                // ── Info: Party Info (slot 48) — matches FriendsMenu ──
                List<String> infoLore = new ArrayList<>();
                infoLore.add("");
                if (leaderUuid == null) {
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Groupe : " + CC.RED + "Aucun");
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Rôle : " + CC.GRAY + "—");
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Membres : " + CC.WHITE + "0");
                } else {
                    String leaderName = memberNames.getOrDefault(leaderUuid, "Inconnu");
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Chef : " + CC.GOLD + leaderName);
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Rôle : " + (isLeader ? CC.GOLD + "Chef" : CC.GREEN + "Membre"));
                    infoLore.add(CC.DARK_GRAY + "▪ " + CC.GRAY + "Membres : " + CC.WHITE + members.size());
                }
                infoLore.add("");
                
                ItemStack info = new ItemBuilder(Material.BOOK)
                    .setName(CC.AQUA + "" + CC.BOLD + "Informations")
                    .setLore(infoLore).build();
                inventory.setItem(48, info);

                // ── Back to Profile (slot 49) — matches FriendsMenu ──
                inventory.setItem(49, LobbyMenuUtils.getBackButton());

                // ── Invite Player (slot 50) — matches FriendsMenu "Ajouter un ami" ──
                if (isLeader) {
                    ItemStack inviteItem = new ItemBuilder(Material.EMERALD)
                        .setName(CC.GREEN + "" + CC.BOLD + "Inviter un joueur")
                        .setLore(
                            "",
                            CC.GRAY + "Cliquez pour inviter un",
                            CC.GRAY + "joueur dans votre groupe.",
                            ""
                        ).build();
                    inventory.setItem(50, inviteItem);
                }

                // ── Leave / Disband Party (slot 51) ──
                if (leaderUuid != null) {
                    ItemStack leaveItem = new ItemBuilder(Material.BARRIER)
                        .setName(isLeader ? CC.RED + "" + CC.BOLD + "Dissoudre le groupe" : CC.RED + "" + CC.BOLD + "Quitter le groupe")
                        .setLore(
                            "",
                            CC.GRAY + (isLeader ? "Cliquez pour dissoudre" : "Cliquez pour quitter"),
                            CC.GRAY + (isLeader ? "votre groupe." : "le groupe actuel."),
                            ""
                        ).build();
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
                String prefix = Constants.PREFIX;
                player.sendMessage(prefix + CC.RED + "Seul le chef de groupe peut inviter des joueurs.");
                player.closeInventory();
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player.closeInventory();
            // Add player to pending party invite list so LobbyListener catches their next chat message
            fr.corehost.lobby.listeners.LobbyListener.pendingPartyInvite.add(player.getUniqueId());
            String prefix = Constants.PREFIX;
            player.sendMessage(prefix + "Tapez le " + CC.GREEN + "pseudo" + CC.GRAY + " du joueur à inviter dans le chat.");
            player.sendMessage(prefix + "Tapez " + CC.YELLOW + "'annuler'" + CC.GRAY + " pour annuler.");
            return;
        }

        // ── Leave / Disband ──
        if (slot == 51 && clickedItem.getType() == Material.BARRIER) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            String prefix = Constants.BUNGEE_PREFIX;
            UUID leaderUuid = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
            if (leaderUuid != null && leaderUuid.equals(player.getUniqueId())) {
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour " + CC.RED + "dissoudre le groupe" + CC.GRAY + " !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party disband"));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(CC.RED + "Cliquez pour dissoudre")));
                player.spigot().sendMessage(msg);
            } else {
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour " + CC.RED + "quitter le groupe" + CC.GRAY + " !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party leave"));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(CC.RED + "Cliquez pour quitter")));
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
            String targetName = org.bukkit.ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (clickType == ClickType.RIGHT) {
                // Kick from party via proxy command
                UUID leaderUuid = plugin.getPartyManager().getPartyLeader(player.getUniqueId());
                if (leaderUuid != null && leaderUuid.equals(player.getUniqueId()) && !player.getUniqueId().equals(targetUuid)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.closeInventory();
                    String prefix = Constants.BUNGEE_PREFIX;
                    net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour expulser " + CC.YELLOW + targetName + CC.GRAY + " du groupe !");
                    msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party kick " + targetName));
                    msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(CC.RED + "Cliquez pour expulser")));
                    player.spigot().sendMessage(msg);
                }
            }
        }
    }
}
