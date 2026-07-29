package fr.corehost.lobby.cloudnet;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import eu.cloudnetservice.driver.provider.CloudServiceProvider;
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
    public void createHost(Player player, String gameType) {
        String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
        
        if (!isCloudNetEnabled || plugin.getHostManager() == null) {
            player.sendMessage(prefix + ChatColor.RED + "Le système de Host est actuellement en maintenance ou en mise à jour. Veuillez réessayer plus tard.");
            return;
        }

        player.sendMessage(prefix + "Préparation de votre serveur " + ChatColor.YELLOW + gameType + ChatColor.GRAY + " en cours...");

        // Start async to avoid blocking main thread during API calls
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ServiceTaskProvider taskProvider = InjectionLayer.ext().instance(ServiceTaskProvider.class);
                ServiceTask task = taskProvider.serviceTask(gameType); // "Sumo" or "CTF"

                if (task == null) {
                    player.sendMessage(ChatColor.RED + "Erreur: La tâche CloudNet '" + gameType + "' n'existe pas !");
                    return;
                }
                
                CloudServiceProvider serviceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
                Collection<ServiceInfoSnapshot> runningServices = serviceProvider.servicesByTask(gameType);
                ServiceInfoSnapshot warmService = null;
                
                List<HostData> allHosts = plugin.getHostManager().getAllHosts();
                
                for (ServiceInfoSnapshot service : runningServices) {
                    if (service.lifeCycle() == ServiceLifeCycle.RUNNING) {
                        boolean isClaimed = allHosts.stream().anyMatch(h -> h.getServerName().equalsIgnoreCase(service.name()));
                        if (!isClaimed) {
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
                    
                    HostData hostData = new HostData(
                            hostId,
                            player.getUniqueId(),
                            player.getName(),
                            gameType,
                            serverName,
                            maxPlayers
                    );
                    
                    // We set it directly to WAITING so the Proxy can teleport them if needed, or they can be teleported now
                    hostData.setStatus(fr.corehost.api.host.HostStatus.WAITING);
                    plugin.getHostManager().saveHost(hostData);
                    
                    if (player.isOnline()) {
                        player.sendMessage(prefix + ChatColor.GREEN + "Serveur " + ChatColor.GOLD + serverName + ChatColor.GREEN + " trouvé (Warm Pool) ! Téléportation immédiate...");
                    }
                    // Velocity will handle the teleportation via its own logic if they use a command, or they need to connect.
                    // To force connect them from Lobby, we use BungeeCord channel
                    sendPlayerToServer(player, serverName);
                    
                } else {
                    // Fallback: Create new service
                    ServiceConfiguration configuration = ServiceConfiguration.builder(task).build();
                    
                    CloudServiceFactory serviceFactory = InjectionLayer.ext().instance(CloudServiceFactory.class);
                    ServiceCreateResult createResult = serviceFactory.createCloudService(configuration);
                    
                    if (createResult.state() == ServiceCreateResult.State.CREATED || createResult.state() == ServiceCreateResult.State.DEFERRED) {
                        ServiceInfoSnapshot serviceInfo = createResult.serviceInfo();
                        
                        // Host created, register in Redis
                        UUID hostId = UUID.randomUUID();
                        String serverName = serviceInfo.name();
                        int maxPlayers = plugin.getConfig().getInt("games." + gameType + ".max-players", 20);
                        
                        HostData hostData = new HostData(
                                hostId,
                                player.getUniqueId(),
                                player.getName(),
                                gameType,
                                serverName,
                                maxPlayers
                        );

                        plugin.getHostManager().saveHost(hostData);

                        if (player.isOnline()) {
                            player.sendMessage(prefix + ChatColor.GREEN + "Le serveur " + ChatColor.GOLD + serverName + ChatColor.GREEN + " est prêt et démarre !");
                        }
                        
                        // Actually start the process
                        serviceInfo.provider().start();
                    } else {
                        if (player.isOnline()) {
                            player.sendMessage(prefix + ChatColor.RED + "Erreur: Impossible de créer l'instance de serveur. (" + createResult.state().name() + ")");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (player.isOnline()) {
                    player.sendMessage(prefix + ChatColor.RED + "Le système de Host est actuellement en maintenance ou en mise à jour. Veuillez réessayer plus tard.");
                }
            }
        });
    }
    
    private void sendPlayerToServer(Player player, String serverName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                java.io.DataOutputStream out = new java.io.DataOutputStream(b);
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
