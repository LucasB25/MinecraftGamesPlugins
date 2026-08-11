package fr.corehost.lobby.utils;

import fr.corehost.api.utils.CC;

public class Constants {
    
    public static String PREFIX;
    
    public static String BUNGEE_PREFIX;

    public static void load(org.bukkit.configuration.file.FileConfiguration config) {
        String rawPrefix = config.getString("settings.prefix", "&8[&6CoreHost&8] &7");
        PREFIX = CC.translate( rawPrefix);
        BUNGEE_PREFIX = CC.translate( rawPrefix);
    }

}
