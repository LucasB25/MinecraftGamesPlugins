package fr.corehost.game.slime;

import fr.corehost.game.CoreHostGame;
import fr.corehost.api.redis.RedisManager;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SlimeManager {

    private final CoreHostGame plugin;
    private final RedisManager redisManager;
    private final String serverName;

    public SlimeManager(CoreHostGame plugin, RedisManager redisManager, String serverName) {
        this.plugin = plugin;
        this.redisManager = redisManager;
        this.serverName = serverName;
    }

    public void loadWorld(String templateName, String worldName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Loading world " + worldName + " from template " + templateName + "...");
                
                // Check if it's a SlimeWorld file (.slime) in the slime_worlds folder
                File slimeWorldsDir = new File("slime_worlds");
                File templateSlimeFile = new File(slimeWorldsDir, templateName + ".slime");
                File targetSlimeFile = new File(slimeWorldsDir, worldName + ".slime");
                
                if (templateSlimeFile.exists() && templateSlimeFile.isFile()) {
                    Files.copy(templateSlimeFile.toPath(), targetSlimeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    try {
                        com.infernalsuite.asp.api.AdvancedSlimePaperAPI asp = com.infernalsuite.asp.api.AdvancedSlimePaperAPI.instance();
                        
                        // Instantiate the FileLoader directly as required by ASP v4+
                        com.infernalsuite.asp.api.loaders.SlimeLoader loader = new com.infernalsuite.asp.loaders.file.FileLoader(slimeWorldsDir);
                        
                        com.infernalsuite.asp.api.world.SlimeWorld slimeWorld = asp.readWorld(loader, worldName, false, new com.infernalsuite.asp.api.world.properties.SlimePropertyMap());

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            asp.loadWorld(slimeWorld, true);
                            plugin.getLogger().info("SlimeWorld " + worldName + " loaded from template " + templateName + "!");
                            finishLoading(worldName, serverName);
                        });
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Failed to load slime world with ASP: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    return;
                }
                
                // Fallback: copy world folder
                File serverFolder = Bukkit.getWorldContainer();
                File templateFolder = new File(serverFolder, templateName.toLowerCase());
                File targetFolder = new File(serverFolder, worldName);
                
                if (templateFolder.exists() && templateFolder.isDirectory()) {
                    copyDirectory(templateFolder, targetFolder);
                    File uidFile = new File(targetFolder, "uid.dat");
                    if (uidFile.exists()) uidFile.delete();
                } else {
                    plugin.getLogger().warning("Template folder " + templateName + " does not exist! An empty world will be created.");
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.createWorld(new WorldCreator(worldName));
                    plugin.getLogger().info("World " + worldName + " loaded from template " + templateName + "!");
                    finishLoading(worldName, serverName);
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load world " + worldName + ": " + e.getMessage());
            }
        });
    }
    
    private void finishLoading(String worldName, String serverName) {
        if (redisManager != null) {
            // Local teleport fallback
            try {
                fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(redisManager);
                for (fr.corehost.api.host.HostData h : hostManager.getAllHosts()) {
                    if (h.getWorldName().equals(worldName) && h.getServerName().equalsIgnoreCase(serverName)) {
                        org.bukkit.entity.Player owner = Bukkit.getPlayer(h.getOwnerUuid());
                        if (owner != null && owner.isOnline()) {
                            owner.teleport(Bukkit.getWorld(worldName).getSpawnLocation());
                            owner.sendMessage("§7Votre serveur Host §6" + h.getGameType() + " §7est prêt ! Téléportation en cours...");
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not teleport locally: " + e.getMessage());
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("action", "HOST_READY");
            response.addProperty("hostId", worldName);
            response.addProperty("serverName", serverName);
            redisManager.publish("corehost:proxy:events", response.toString());
        }
    }
    
    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdir();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
