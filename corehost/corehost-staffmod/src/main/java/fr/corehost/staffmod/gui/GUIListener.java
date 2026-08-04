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

            if (event.getSlot() == 10) {
                plugin.getVanishManager().toggleVanish(player);
                player.closeInventory();
                new ModGUI(plugin).open(player);
            } else if (event.getSlot() == 12) {
                new StaffListGUI(plugin).open(player);
            } else if (event.getSlot() == 14) {
                new ReportGUI(plugin).open(player);
            } else if (event.getSlot() == 16) {
                new PlayerListGUI(plugin).open(player);
            }
        } 
        else if (title.equals("Staff en Ligne")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            ItemStack item = event.getCurrentItem();
            if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
                String targetName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous teleporter a vous-meme.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(Component.text("Teleporte a " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                }
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
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous teleporter a vous-meme.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(Component.text("Teleporte a " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                }
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
        else if (title.startsWith("Mod: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            String targetName = title.substring("Mod: ".length());
            
            int slot = event.getSlot();
            if (slot == 12) { // Invsee
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    player.openInventory(target.getInventory());
                } else {
                    player.sendMessage(Component.text("Le joueur n'est pas sur ce serveur. Teleportez-vous à lui d'abord.", net.kyori.adventure.text.format.NamedTextColor.RED));
                    player.closeInventory();
                }
            } else if (slot == 14) { // Freeze
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("action", "FREEZE_PLAYER");
                json.addProperty("target", targetName);
                json.addProperty("sender", player.getName());

                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().publish("corehost:staff:events", json.toString());
                }
                player.sendMessage(Component.text("Ordre de freeze/unfreeze envoyé pour " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN));
                player.closeInventory();
            } else if (slot == 30) { // TP
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous teleporter a vous-meme.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(Component.text("Teleporte a " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                }
            } else if (slot == 32 || slot == 38 || slot == 42) { // History, Mute, Ban
                player.sendMessage(Component.text("Cette fonctionnalité arrive bientôt !", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                player.closeInventory();
            }
        }
    }
}