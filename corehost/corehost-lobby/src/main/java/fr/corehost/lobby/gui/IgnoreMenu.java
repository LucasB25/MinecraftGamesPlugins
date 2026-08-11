package fr.corehost.lobby.gui;

import fr.corehost.lobby.utils.Constants;
import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import fr.corehost.lobby.utils.ItemBuilder;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IgnoreMenu implements CustomMenu {

    private final CoreHostLobby plugin;

    private Inventory inventory;
    private final List<UUID> ignoredPlayersList = new ArrayList<>();

    public IgnoreMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        this.inventory = Bukkit.createInventory(this, 54, CC.DARK_GRAY + "» " + CC.LIGHT_PURPLE + "Joueurs Ignorés");

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

                // ── Bottom bar decoration (unified Pink + Purple) ──
                LobbyMenuUtils.fillBottomRow(inventory);

                // ── Populate Ignored Players ──
                int slot = 0;
                for (UUID ignoredUuid : ignoredPlayersList) {
                    if (slot >= 45) break; // Limit to 45 players for now
                    
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ignoredUuid);
                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Joueur Inconnu";

                    ItemStack skull = new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullOwner(name)
                        .setName(CC.RED + name)
                        .setLore(
                            "",
                            CC.GRAY + "Ce joueur est actuellement",
                            CC.GRAY + "ignoré. Vous ne recevrez",
                            CC.GRAY + "plus de messages privés",
                            CC.GRAY + "de sa part.",
                            "",
                            CC.GREEN + "► Cliquez pour ne plus l'ignorer"
                        ).build();
                    inventory.setItem(slot, skull);
                    slot++;
                }

                if (ignoredPlayersList.isEmpty()) {
                    ItemStack empty = new ItemBuilder(Material.BARRIER)
                        .setName(CC.RED + "Aucun joueur ignoré")
                        .setLore(
                            "",
                            CC.GRAY + "Vous n'avez ignoré personne."
                        ).build();
                    inventory.setItem(22, empty); // Center of inventory
                }

                // ── Back to Settings ──
                inventory.setItem(49, LobbyMenuUtils.getBackToSettingsButton());

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
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Vous n'ignorez plus " + name + ".");
                    open(player); // Refresh current menu
                });
            });
        }
    }
}
