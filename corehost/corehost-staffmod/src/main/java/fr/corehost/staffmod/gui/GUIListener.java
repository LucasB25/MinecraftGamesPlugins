package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GUIListener implements Listener {

    private final StaffModPlugin plugin;

    public GUIListener(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.equals("Menu de Moderation")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            if (event.getSlot() == 11) {
                plugin.getVanishManager().toggleVanish(player);
                player.closeInventory();
                new ModGUI(plugin).open(player);
            } else if (event.getSlot() == 13) {
                new ReportGUI(plugin).open(player);
            } else if (event.getSlot() == 15) {
                new PlayerListGUI(plugin).open(player);
            }
        } 
        else if (title.equals("Signalements Actifs")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            ItemStack item = event.getCurrentItem();
            if (item.getItemMeta() == null || item.getItemMeta().lore() == null) return;
            
            java.util.List<Component> lore = item.getItemMeta().lore();
            if (lore.size() < 6) return;
            
            String uuidStr = PlainTextComponentSerializer.plainText().serialize(lore.get(5));
            UUID reportId;
            try {
                reportId = UUID.fromString(uuidStr);
            } catch (Exception e) { return; }

            if (event.isRightClick()) {
                plugin.getReportManager().deleteReport(reportId);
                new ReportGUI(plugin).open(player);
            } else if (event.isLeftClick()) {
                String targetName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("action", "TELEPORT_STAFF");
                json.addProperty("staffUuid", player.getUniqueId().toString());
                json.addProperty("targetName", targetName);
                
                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                }
                player.closeInventory();
                player.sendMessage("Teleportation vers " + targetName + " demandee...");
            }
        }
        else if (title.equals("Joueurs (Freeze)")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            String targetName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                plugin.getFreezeManager().toggleFreeze(target);
                new PlayerListGUI(plugin).open(player);
            }
        }
    }
}