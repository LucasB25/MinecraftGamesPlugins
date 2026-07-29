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

    public void createHost(Player player, String gameType) {
        if (!isCloudNetEnabled) {
            player.sendMessage(ChatColor.RED + "CloudNet n'est pas disponible sur ce serveur de test. Impossible de démarrer un vrai sous-serveur.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Préparation de votre serveur " + gameType + " en cours...");

        // Start async to avoid blocking main thread during API calls
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ServiceTaskProvider taskProvider = InjectionLayer.ext().instance(ServiceTaskProvider.class);
                ServiceTask task = taskProvider.serviceTask(gameType); // "Sumo" or "CTF"

                if (task == null) {
                    player.sendMessage(ChatColor.RED + "Erreur: La tâche CloudNet '" + gameType + "' n'existe pas !");
                    return;
                }

                ServiceConfiguration configuration = ServiceConfiguration.builder(task).build();
                
                CloudServiceFactory serviceFactory = InjectionLayer.ext().instance(CloudServiceFactory.class);
                ServiceCreateResult createResult = serviceFactory.createCloudService(configuration);
                
                if (createResult.state() == ServiceCreateResult.State.CREATED || createResult.state() == ServiceCreateResult.State.DEFERRED) {
                    ServiceInfoSnapshot serviceInfo = createResult.serviceInfo();
                    
                    // Host created, register in Redis
                    UUID hostId = UUID.randomUUID(); // Optional: use serviceInfo.serviceId().uniqueId()
                    String serverName = serviceInfo.name();
                    
                    HostData hostData = new HostData(
                            hostId,
                            player.getUniqueId(),
                            player.getName(),
                            gameType,
                            serverName,
                            20 // Default max players
                    );

                    plugin.getHostManager().saveHost(hostData);

                    player.sendMessage(ChatColor.GREEN + "Votre serveur " + ChatColor.GOLD + serverName + ChatColor.GREEN + " a été créé ! Il est en cours de démarrage...");
                    
                    // Actually start the process
                    serviceInfo.provider().start();
                } else {
                    player.sendMessage(ChatColor.RED + "Erreur: Impossible de créer l'instance de serveur. (" + createResult.state().name() + ")");
                }

            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "Une erreur inattendue est survenue lors de la communication avec CloudNet.");
            }
        });
    }
}
