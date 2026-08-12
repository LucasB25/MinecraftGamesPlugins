package fr.corehost.sumo.commands;

import fr.corehost.sumo.CoreHostSumo;
import fr.corehost.sumo.SumoGameInstance;
import fr.corehost.api.host.HostData;
import fr.corehost.api.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.google.gson.JsonObject;
import java.util.UUID;

public class SumoCommand implements CommandExecutor {

    private final CoreHostSumo plugin;

    public SumoCommand(CoreHostSumo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("corehost.admin")) {
            player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Usage: /sumo <create|join|list|leave>");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create":
                int bestOf = 3;
                if (args.length > 1) {
                    try {
                        bestOf = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Le format BO doit être un nombre.");
                        return true;
                    }
                }
                createLocalSumo(player, bestOf);
                break;
            case "join":
                if (args.length < 2) {
                    player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Usage: /sumo join <hostId>");
                    return true;
                }
                String hostId = args[1];
                SumoGameInstance instance = plugin.getGameManager().getInstance(hostId);
                if (instance != null) {
                    player.teleport(instance.getWorld().getSpawnLocation());
                    instance.addPlayer(player);
                } else {
                    player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Instance introuvable.");
                }
                break;
            case "list":
                player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.YELLOW + "Instances actives :");
                player.sendMessage(CC.GRAY + "- (Commande en cours de développement)");
                break;
            case "leave":
                plugin.getGameManager().getInstanceForPlayer(player).ifPresent(inst -> {
                    inst.removePlayer(player);
                    player.teleport(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
                    player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.GREEN + "Vous avez quitté la partie.");
                });
                break;
            default:
                player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Usage: /sumo <create|join|list|leave>");
                break;
        }

        return true;
    }

    private void createLocalSumo(Player player, int bestOf) {
        player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.YELLOW + "Création d'une instance Sumo locale...");
        
        UUID hostId = UUID.randomUUID();
        String worldName = "sumo-" + hostId.toString().substring(0, 8);
        String localServerName = "Unknown-1";
        
        fr.corehost.game.CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(fr.corehost.game.CoreHostGame.class);
        
        fr.corehost.sumo.SumoMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
        if (mapConfig == null) {
            player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "Aucune map n'est configurée pour héberger une partie.");
            return;
        }

        if (coreGame != null && coreGame.getRedisManager() != null) {
            plugin.getGameManager().setPendingMap(worldName, mapConfig.getName());
            
            fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(coreGame.getRedisManager());
            HostData hostData = new HostData(
                    hostId,
                    player.getUniqueId(),
                    player.getName(),
                    "sumo",
                    localServerName, 
                    worldName, 
                    2
            );
            hostData.setBestOf(bestOf);
            hostData.setStatus(fr.corehost.api.host.HostStatus.STARTING);
            hostManager.saveHost(hostData).join();
            
            JsonObject request = new JsonObject();
            request.addProperty("action", "create_slime_instance");
            request.addProperty("hostId", worldName);
            request.addProperty("gameType", "sumo");
            request.addProperty("templateName", mapConfig.getTemplateName());
            
            coreGame.getRedisManager().publish("corehost:game:" + localServerName, request.toString());
        } else {
            player.sendMessage(SumoGameInstance.SUMO_PREFIX + CC.RED + "CoreHostGame ou Redis n'est pas disponible.");
        }
    }
}
