package fr.corehost.staffmod.redis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.corehost.staffmod.StaffModPlugin;
import fr.corehost.staffmod.gui.StaffListGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class StaffPubSubListener extends JedisPubSub {

    private final Gson gson = new Gson();
    private final StaffModPlugin plugin;

    public StaffPubSubListener(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals("corehost:staff:events")) return;

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.get("action").getAsString();

            if ("STAFF_CHAT".equals(action)) {
                String sender = json.get("sender").getAsString();
                String content = json.get("message").getAsString();
                String rank = json.has("rank") ? json.get("rank").getAsString() : "";
                
                Component prefixComponent = Component.empty();
                if (!rank.isEmpty()) {
                    prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rank + " ");
                }
                
                Component scTag = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&6SC&8] &7");
                
                Component scMessage = scTag
                        .append(prefixComponent)
                        .append(Component.text(sender, NamedTextColor.RED))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(content, NamedTextColor.WHITE));
                        
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.hasPermission("staffmod.staffchat")) {
                            online.sendMessage(scMessage);
                        }
                    }
                    Bukkit.getConsoleSender().sendMessage(scMessage);
                });
            } else if ("CHAT_FILTER".equals(action)) {
                String sender = json.get("sender").getAsString();
                String reason = json.get("reason").getAsString();
                String content = json.get("message").getAsString();
                
                Component filterTag = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&6Filtre&8] &7");
                
                Component filterMessage = filterTag
                        .append(Component.text(sender, NamedTextColor.YELLOW))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(content, NamedTextColor.GRAY))
                        .append(Component.text(" (" + reason + ")", NamedTextColor.RED));
                        
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.hasPermission("staffmod.use")) {
                            online.sendMessage(filterMessage);
                        }
                    }
                    Bukkit.getConsoleSender().sendMessage(filterMessage);
                });
            } else if ("FREEZE_PLAYER".equals(action)) {
                String targetName = json.get("target").getAsString();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player target = Bukkit.getPlayerExact(targetName);
                    if (target != null && target.isOnline()) {
                        plugin.getFreezeManager().toggleFreeze(target);
                    }
                });
            } else if ("STAFF_LIST_RESPONSE".equals(action)) {
                String requesterStr = json.get("requesterUuid").getAsString();
                UUID requesterUuid = UUID.fromString(requesterStr);
                JsonArray staffList = json.getAsJsonArray("staffList");
                
                StaffListGUI.handleResponse(requesterUuid, staffList, plugin);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur dans StaffPubSubListener: " + e.getMessage());
        }
    }
}