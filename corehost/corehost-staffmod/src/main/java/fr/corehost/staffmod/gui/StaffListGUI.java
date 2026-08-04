package fr.corehost.staffmod.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Staff en Ligne", NamedTextColor.DARK_RED));
        
        // Add loading item
        ItemStack loading = new ItemStack(Material.GLASS_PANE);
        loading.editMeta(meta -> meta.displayName(Component.text("Chargement du staff...", NamedTextColor.GRAY)));
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, loading);
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
                
                int slot = 0;
                for (JsonElement elem : staffList) {
                    if (slot >= 54) break;
                    
                    JsonObject staffObj = elem.getAsJsonObject();
                    String name = staffObj.get("name").getAsString();
                    String serverName = staffObj.get("server").getAsString();
                    
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
                    meta.displayName(Component.text(name, NamedTextColor.GOLD));
                    
                    // Optionnel : vérifier si on le trouve en vanish localement
                    boolean isVanishedLocal = false;
                    Player localTarget = Bukkit.getPlayerExact(name);
                    if (localTarget != null) {
                        isVanishedLocal = plugin.getVanishManager().isVanished(localTarget.getUniqueId());
                    }

                    meta.lore(Arrays.asList(
                        Component.text("Serveur: " + serverName, NamedTextColor.GRAY),
                        Component.text("Vanish local: " + (isVanishedLocal ? "Oui" : "Non"), isVanishedLocal ? NamedTextColor.GREEN : NamedTextColor.RED),
                        Component.text("Clique pour te téléporter", NamedTextColor.YELLOW)
                    ));
                    
                    head.setItemMeta(meta);
                    inv.setItem(slot++, head);
                }
            }
        });
    }

    public static void remove(UUID uuid) {
        openGuis.remove(uuid);
    }
}
