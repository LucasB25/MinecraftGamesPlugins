package fr.corehost.lobby.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LuckPermsHook {

    public static String getPlayerPrefix(Player player) {
        return getPlayerPrefix(player.getUniqueId());
    }

    public static String getPlayerPrefix(java.util.UUID uuid) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(uuid);
            if (user == null) {
                // If not loaded (e.g. offline friend), we must load them synchronously
                // This method should be called async if the user is offline!
                user = api.getUserManager().loadUser(uuid).join();
            }
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null) {
                    return ChatColor.translateAlternateColorCodes('&', prefix);
                }
                
                String group = user.getPrimaryGroup();
                if (group != null) {
                    if (group.equalsIgnoreCase("default")) {
                        return ChatColor.GRAY + "Joueurs";
                    } else if (group.equalsIgnoreCase("admin") || group.equalsIgnoreCase("administrateur")) {
                        return ChatColor.RED + group.substring(0, 1).toUpperCase() + group.substring(1);
                    } else if (group.equalsIgnoreCase("modo") || group.equalsIgnoreCase("moderateur")) {
                        return ChatColor.DARK_GREEN + group.substring(0, 1).toUpperCase() + group.substring(1);
                    } else {
                        return ChatColor.AQUA + group.substring(0, 1).toUpperCase() + group.substring(1);
                    }
                }
            }
        } catch (Exception ignored) {
            // LuckPerms not loaded or error
        }
        return ChatColor.GRAY + "Joueurs";
    }
}
