package fr.corehost.lobby.cloudnet;

import com.google.gson.JsonObject;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import fr.corehost.api.host.HostData;
import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.entity.Player;

import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import fr.corehost.lobby.utils.Constants;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CloudNetServiceManager {

    private final CoreHostLobby plugin;
    private final boolean isCloudNetEnabled;

    public CloudNetServiceManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.isCloudNetEnabled = Bukkit.getPluginManager().isPluginEnabled("CloudNet-Bridge");
        if (!isCloudNetEnabled) {
            plugin.getLogger().warning("CloudNet-Bridge n'est pas actif ! La création de hosts est en mode 'simulation'.");
        }
    }

    @SuppressWarnings("deprecation")
    public void createHost(Player player, String gameType, int bestOf, boolean doubleJumpEnabled, boolean customKBEnabled) {
        String prefix = Constants.PREFIX;
        
        if (player.hasMetadata("modmode")) {
            player.sendMessage(prefix + CC.RED + "Vous ne pouvez pas créer un host en mode Modération !");
            return;
        }

        if (!isCloudNetEnabled || plugin.getHostManager() == null) {
            // Local Test Mode Bypass
            player.sendMessage(prefix + CC.YELLOW + "[Mode Test Local] " + CC.GRAY + "Génération du monde en cours...");
            
            UUID hostId = UUID.randomUUID();
            String worldName = gameType.toLowerCase() + "-" + hostId.toString().substring(0, 8);
            String localServerName = gameType.substring(0, 1).toUpperCase() + gameType.substring(1).toLowerCase() + "-1";
            
            if (plugin.getHostManager() != null) {
                HostData hostData = new HostData(
                        hostId,
                        player.getUniqueId(),
                        player.getName(),
                        gameType,
                        localServerName, 
                        worldName, 
                        2
                );
                hostData.setBestOf(bestOf);
                hostData.setDoubleJumpEnabled(doubleJumpEnabled);
                hostData.setCustomKB(customKBEnabled);
                hostData.setStatus(fr.corehost.api.host.HostStatus.STARTING);
                plugin.getHostManager().saveHost(hostData);
            }
            
            if (plugin.getRedisManager() != null) {
                JsonObject request = new JsonObject();
                request.addProperty("action", "create_slime_instance");
                request.addProperty("hostId", worldName);
                request.addProperty("gameType", gameType);
                
                plugin.getRedisManager().publish("corehost:game:" + localServerName, request.toString());
            }
            
            return;
        }

        player.sendMessage(prefix + "Préparation de votre serveur " + CC.YELLOW + gameType + CC.GRAY + " en cours...");

        // Start async to avoid blocking main thread during API calls
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ServiceTaskProvider taskProvider = InjectionLayer.ext().instance(ServiceTaskProvider.class);
                ServiceTask task = taskProvider.serviceTask(gameType); // "Sumo" or "CTF"

                if (task == null) {
                    player.sendMessage(CC.RED + "Erreur: La tâche CloudNet '" + gameType + "' n'existe pas !");
                    return;
                }
                
                CloudServiceProvider serviceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
                Collection<ServiceInfoSnapshot> runningServices = serviceProvider.servicesByTask(gameType);
                ServiceInfoSnapshot warmService = null;
                
                List<HostData> allHosts = plugin.getHostManager().getAllHosts();
                int maxInstances = plugin.getConfig().getInt("games." + gameType + ".max-slime-instances", 5);
                
                for (ServiceInfoSnapshot service : runningServices) {
                    if (service.lifeCycle() == ServiceLifeCycle.RUNNING) {
                        long instances = allHosts.stream().filter(h -> h.getServerName().equalsIgnoreCase(service.name())).count();
                        if (instances < maxInstances) {
                            warmService = service;
                            break;
                        }
                    }
                }
                
                if (warmService != null) {
                    // Warm pool service found!
                    UUID hostId = UUID.randomUUID();
                    String serverName = warmService.name();
                    int maxPlayers = plugin.getConfig().getInt("games." + gameType + ".max-players", 20);
                    
                    String worldName = gameType.toLowerCase() + "-" + hostId.toString().substring(0, 8);
                    
                    HostData hostData = new HostData(
                            hostId,
                            player.getUniqueId(),
                            player.getName(),
                            gameType,
                            serverName,
                            worldName,
                            maxPlayers
                    );
                    hostData.setBestOf(bestOf);
                    hostData.setDoubleJumpEnabled(doubleJumpEnabled);
                    hostData.setCustomKB(customKBEnabled);
                    
                    // The slime world needs to be generated, so status is STARTING
                    hostData.setStatus(fr.corehost.api.host.HostStatus.STARTING);
                    plugin.getHostManager().saveHost(hostData);
                    
                    if (player.isOnline()) {
                        player.sendMessage(prefix + CC.GREEN + "Serveur CloudNet trouvé ! Génération du monde Slime en cours...");
                    }
                    
                    // Send PubSub message to the game server to create the slime instance
                    JsonObject request = new JsonObject();
                    request.addProperty("action", "create_slime_instance");
                    request.addProperty("hostId", worldName);
                    request.addProperty("gameType", gameType);
                    
                    plugin.getRedisManager().publish("corehost:game:" + serverName, request.toString());
                    
                } else {
                    // Fallback: Create new CloudNet service
                    ServiceConfiguration configuration = ServiceConfiguration.builder(task).build();
                    
                    CloudServiceFactory serviceFactory = InjectionLayer.ext().instance(CloudServiceFactory.class);
                    ServiceCreateResult createResult = serviceFactory.createCloudService(configuration);
                    
                    if (createResult.state() == ServiceCreateResult.State.CREATED || createResult.state() == ServiceCreateResult.State.DEFERRED) {
                        ServiceInfoSnapshot serviceInfo = createResult.serviceInfo();
                        
                        // Host created, register in Redis
                        UUID hostId = UUID.randomUUID();
                        String serverName = serviceInfo.name();
                        int maxPlayers = plugin.getConfig().getInt("games." + gameType + ".max-players", 20);
                        
                        String worldName = gameType.toLowerCase() + "-" + hostId.toString().substring(0, 8);
                        
                        HostData hostData = new HostData(
                                hostId,
                                player.getUniqueId(),
                                player.getName(),
                                gameType,
                                serverName,
                                worldName,
                                maxPlayers
                        );
                        hostData.setBestOf(bestOf);

                        plugin.getHostManager().saveHost(hostData);

                        if (player.isOnline()) {
                            player.sendMessage(prefix + CC.GREEN + "Aucun serveur prêt. Démarrage complet du serveur " + CC.GOLD + serverName + CC.GREEN + " en cours...");
                        }
                        
                        // Start the CloudNet service, proxy will catch it when RUNNING
                        // But wait! When it starts, it won't load the Slime World automatically!
                        // The proxy will need to send a create_slime_instance message to it, or it will auto-load?
                        // This logic needs to be handled: we publish the message right now, and when the server starts and connects to Redis, it will MISS the message.
                        // We will need the Game Plugin to check its missing instances on startup, or we just let it be STARTING and let Proxy resend the pubsub when it's RUNNING.
                        
                        serviceInfo.provider().start();
                    } else {
                        if (player.isOnline()) {
                            player.sendMessage(prefix + CC.RED + "Erreur: Impossible de créer l'instance de serveur. (" + createResult.state().name() + ")");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (player.isOnline()) {
                    player.sendMessage(prefix + CC.RED + "Le système de Host est actuellement en maintenance ou en mise à jour. Veuillez réessayer plus tard.");
                }
            }
        });
    }
    
}
