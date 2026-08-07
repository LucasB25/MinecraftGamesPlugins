package fr.corehost.staffmod.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffListGUI {

    private final StaffModPlugin plugin;
    // Track open GUIs to update them asynchronously
    private static final Map<UUID, Inventory> openGuis = new HashMap<>();

    public StaffListGUI(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("» ", NamedTextColor.DARK_GRAY).append(Component.text("Staff en Ligne", NamedTextColor.RED, TextDecoration.BOLD)));
        StaffMenuUtils.fillBorder(inv);
        inv.setItem(45, StaffMenuUtils.getCloseButton());
        
        // Add loading item
        ItemStack loading = StaffMenuUtils.getGrayFiller();
        loading.editMeta(meta -> meta.displayName(Component.text("Chargement du staff...", NamedTextColor.GRAY)));
        
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        for (int slot : slots) {
            inv.setItem(slot, loading);
        }

        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), inv);

        // Send request to Proxy
        JsonObject request = new JsonObject();
        request.addProperty("action", "REQUEST_STAFF_LIST");
        request.addProperty("requesterUuid", player.getUniqueId().toString());
        if (plugin.getRedisManager() != null) {
            plugin.getRedisManager().publish("corehost:proxy:events", request.toString());
        }
    }

    public static void handleResponse(UUID requester, JsonArray staffList, StaffModPlugin plugin) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inv = openGuis.get(requester);
            if (inv != null) {
                inv.clear(); // Remove loading panes
                StaffMenuUtils.fillBorder(inv);
                inv.setItem(45, StaffMenuUtils.getCloseButton());
                
                int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
                int slotIndex = 0;
                
                for (JsonElement elem : staffList) {
                    if (slotIndex >= slots.length) break;
                    
                    JsonObject staffObj = elem.getAsJsonObject();
                    String name = staffObj.get("name").getAsString();
                    String serverName = staffObj.get("server").getAsString();
                    
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
                        meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));
                        
                        // Optionnel : vérifier si on le trouve en vanish localement
                        boolean isVanishedLocal = false;
                        Player localTarget = Bukkit.getPlayerExact(name);
                        if (localTarget != null) {
                            isVanishedLocal = plugin.getVanishManager().isVanished(localTarget.getUniqueId());
                        }

                        meta.lore(Arrays.asList(
                            Component.empty(),
                            Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Serveur : ", NamedTextColor.GRAY)).append(Component.text(serverName, NamedTextColor.WHITE)),
                            Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Vanish local : ", NamedTextColor.GRAY)).append(Component.text(isVanishedLocal ? "Oui" : "Non", isVanishedLocal ? NamedTextColor.GREEN : NamedTextColor.RED)),
                            Component.empty(),
                            Component.text("► Clic pour se téléporter", NamedTextColor.YELLOW)
                        ));
                        
                        head.setItemMeta(meta);
                    }
                    inv.setItem(slots[slotIndex++], head);
                }
            }
        });
    }

    public static void remove(UUID uuid) {
        openGuis.remove(uuid);
    }
}

