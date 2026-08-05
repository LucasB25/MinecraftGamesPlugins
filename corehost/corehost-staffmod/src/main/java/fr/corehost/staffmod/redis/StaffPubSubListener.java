package fr.corehost.staffmod.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;
import java.util.UUID;

public class StaffPubSubListener extends JedisPubSub {

    private final Gson gson = new Gson();
    private final fr.corehost.staffmod.StaffModPlugin plugin;

    public StaffPubSubListener(fr.corehost.staffmod.StaffModPlugin plugin) {
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
                    prefixComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(rank + " ");
                }
                
                Component scTag = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&cStaffChat&8] &7");
                
                Component scMessage = scTag
                        .append(prefixComponent)
                        .append(Component.text(sender, NamedTextColor.RED))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(content, NamedTextColor.WHITE));
                        
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("staffmod.staffchat")) {
                        online.sendMessage(scMessage);
                    }
                }
                Bukkit.getConsoleSender().sendMessage(scMessage);
            } else if ("FREEZE_PLAYER".equals(action)) {
                String targetName = json.get("target").getAsString();
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    // Update freeze state locally on the correct server
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getFreezeManager().toggleFreeze(target);
                    });
                }
            } else if ("STAFF_LIST_RESPONSE".equals(action)) {
                String requesterStr = json.get("requesterUuid").getAsString();
                UUID requesterUuid = UUID.fromString(requesterStr);
                com.google.gson.JsonArray staffList = json.getAsJsonArray("staffList");
                
                fr.corehost.staffmod.gui.StaffListGUI.handleResponse(requesterUuid, staffList, plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}