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

        if (title.contains("Staff en Ligne")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            ItemStack item = event.getCurrentItem();
            if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String targetName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous téléporter à vous-même.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté à " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(plugin.getPrefix().append(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                }
            } else if (event.getSlot() == 45) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
        else if (title.contains("Signalements Actifs")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            ItemStack item = event.getCurrentItem();
            if (event.getSlot() == 45) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }
            if (item.getItemMeta() == null || item.getItemMeta().lore() == null) return;
            
            java.util.List<Component> lore = item.getItemMeta().lore();
            if (lore.size() < 6) return;
            
            String uuidStr = PlainTextComponentSerializer.plainText().serialize(lore.get(5));
            UUID reportId;
            try {
                reportId = UUID.fromString(uuidStr);
            } catch (Exception e) { return; }

            if (event.isRightClick()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getReportManager().deleteReport(reportId);
                new ReportGUI(plugin).open(player);
            } else if (event.isLeftClick()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String targetName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous téléporter à vous-même.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté à " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(plugin.getPrefix().append(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                }
            }
        }
        else if (title.contains("Joueurs (Freeze)") || title.contains("Modération - Joueurs")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String targetName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    plugin.getFreezeManager().toggleFreeze(target);
                    new PlayerListGUI(plugin).open(player);
                }
            } else if (event.getSlot() == 45) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
        else if (title.startsWith("Mod: ") || title.contains("Modération : ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            String targetName;
            if (title.contains("Modération : ")) {
                targetName = title.substring(title.indexOf("Modération : ") + "Modération : ".length());
            } else {
                targetName = title.substring("Mod: ".length());
            }
            
            int slot = event.getSlot();
            if (slot == 36) { // Back button
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            } else if (slot == 19) { // Invsee
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    player.openInventory(target.getInventory());
                } else {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le joueur n'est pas sur ce serveur.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    player.closeInventory();
                }
            } else if (slot == 20) { // Enderchest
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    player.openInventory(target.getEnderChest());
                } else {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le joueur n'est pas sur ce serveur.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    player.closeInventory();
                }

            } else if (slot == 23) { // Freeze
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                boolean isFrozen = false;
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    isFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
                }

                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("action", "FREEZE_PLAYER");
                json.addProperty("target", targetName);
                json.addProperty("sender", player.getName());

                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().publish("corehost:staff:events", json.toString());
                }
                if (isFrozen) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous avez dégelé le joueur ", net.kyori.adventure.text.format.NamedTextColor.GREEN))
                        .append(Component.text(targetName, net.kyori.adventure.text.format.NamedTextColor.YELLOW)).append(Component.text(".", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                } else {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous avez gelé le joueur ", net.kyori.adventure.text.format.NamedTextColor.RED))
                        .append(Component.text(targetName, net.kyori.adventure.text.format.NamedTextColor.YELLOW)).append(Component.text(".", net.kyori.adventure.text.format.NamedTextColor.RED)));
                }
                player.closeInventory();
            } else if (slot == 21) { // TP
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas vous téléporter à vous-même.", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                player.closeInventory();
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté à " + targetName, net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                } else {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("action", "TELEPORT_STAFF");
                    json.addProperty("staffUuid", player.getUniqueId().toString());
                    json.addProperty("targetName", targetName);
                    
                    if (plugin.getRedisManager() != null) {
                        plugin.getRedisManager().setEx("corehost:pending_tp:" + player.getUniqueId().toString(), targetName, 30);
                        plugin.getRedisManager().publish("corehost:proxy:events", json.toString());
                    }
                    player.sendMessage(plugin.getPrefix().append(Component.text("Connexion au serveur de " + targetName + "...", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                }
            } else if (slot == 29 || slot == 31 || slot == 33) { // History, Mute, Ban
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(plugin.getPrefix().append(Component.text("Cette fonctionnalité arrive bientôt !", net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
                player.closeInventory();
            }
        }
    }
}