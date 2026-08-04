package fr.corehost.staffmod.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

public class StaffPubSubListener extends JedisPubSub {

    private final Gson gson = new Gson();

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals("corehost:staff:events")) return;

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.get("action").getAsString();

            if ("STAFF_CHAT".equals(action)) {
                String sender = json.get("sender").getAsString();
                String content = json.get("message").getAsString();
                
                Component scMessage = Component.text("[SC] ", NamedTextColor.BLUE)
                        .append(Component.text(sender + " » ", NamedTextColor.AQUA))
                        .append(Component.text(content, NamedTextColor.WHITE));
                        
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("staffmod.staffchat")) {
                        online.sendMessage(scMessage);
                    }
                }
                Bukkit.getConsoleSender().sendMessage(scMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}