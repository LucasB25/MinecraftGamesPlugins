package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IgnoreMenu implements CustomMenu {

    private final CoreHostLobby plugin;
    private final Player targetPlayer;
    private Inventory inventory;
    private final List<UUID> ignoredPlayersList = new ArrayList<>();

    public IgnoreMenu(CoreHostLobby plugin, Player player) {
        this.plugin = plugin;
        this.targetPlayer = player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "» " + ChatColor.RED + "Joueurs Ignorés");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ignoredPlayersList.clear();

            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                    Set<String> members = jedis.smembers("corehost:messages:ignored:" + player.getUniqueId().toString());
                    for (String m : members) {
                        try {
                            ignoredPlayersList.add(UUID.fromString(m));
                        } catch (IllegalArgumentException ignored) {}
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error fetching ignored players for " + player.getName());
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                // ── Border decoration ──
                ItemStack filler1 = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta1 = filler1.getItemMeta();
                if (meta1 != null) { meta1.setDisplayName(" "); filler1.setItemMeta(meta1); }

                ItemStack filler2 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta meta2 = filler2.getItemMeta();
                if (meta2 != null) { meta2.setDisplayName(" "); filler2.setItemMeta(meta2); }

                for (int i = 45; i < 54; i++) {
                    inventory.setItem(i, (i % 2 == 0) ? filler1 : filler2);
                }

                // ── Populate Ignored Players ──
                int slot = 0;
                for (UUID ignoredUuid : ignoredPlayersList) {
                    if (slot >= 45) break; // Limit to 45 players for now
                    
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ignoredUuid);
                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Joueur Inconnu";

                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    if (skullMeta != null) {
                        skullMeta.setOwningPlayer(offlinePlayer);
                        skullMeta.setDisplayName(ChatColor.RED + name);
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Ce joueur est actuellement");
                        lore.add(ChatColor.GRAY + "ignoré. Vous ne recevrez");
                        lore.add(ChatColor.GRAY + "plus de messages privés");
                        lore.add(ChatColor.GRAY + "de sa part.");
                        lore.add("");
                        lore.add(ChatColor.GREEN + "► Cliquez pour ne plus l'ignorer");
                        skullMeta.setLore(lore);
                        skull.setItemMeta(skullMeta);
                    }
                    inventory.setItem(slot, skull);
                    slot++;
                }

                if (ignoredPlayersList.isEmpty()) {
                    ItemStack empty = new ItemStack(Material.BARRIER);
                    ItemMeta emptyMeta = empty.getItemMeta();
                    if (emptyMeta != null) {
                        emptyMeta.setDisplayName(ChatColor.RED + "Aucun joueur ignoré");
                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.GRAY + "Vous n'avez ignoré personne.");
                        emptyMeta.setLore(lore);
                        empty.setItemMeta(emptyMeta);
                    }
                    inventory.setItem(22, empty); // Center of inventory
                }

                // ── Back to Settings ──
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.RED + "◄ Retour aux Paramètres");
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);

                player.openInventory(inventory);
            });
        });
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType().name().contains("GLASS_PANE") || clickedItem.getType() == Material.BARRIER) return;

        int slot = event.getSlot();

        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new SettingsMenu(plugin).open(player, false);
            return;
        }

        if (slot < 45 && slot < ignoredPlayersList.size()) {
            UUID targetUuid = ignoredPlayersList.get(slot);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "ce joueur";

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                        jedis.srem("corehost:messages:ignored:" + player.getUniqueId().toString(), targetUuid.toString());
                    } catch (Exception ignored) {}
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GREEN + "Vous n'ignorez plus " + name + ".");
                    open(player); // Refresh current menu
                });
            });
        }
    }
}
