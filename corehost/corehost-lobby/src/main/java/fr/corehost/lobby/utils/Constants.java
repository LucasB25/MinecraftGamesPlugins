package fr.corehost.lobby.utils;

import org.bukkit.ChatColor;

public class Constants {
    
    public static String PREFIX;
    
    public static String BUNGEE_PREFIX;

    public static void load(org.bukkit.configuration.file.FileConfiguration config) {
        String rawPrefix = config.getString("settings.prefix", "&8[&6CoreHost&8] &7");
        PREFIX = ChatColor.translateAlternateColorCodes('&', rawPrefix);
        BUNGEE_PREFIX = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', rawPrefix);
    }

}
