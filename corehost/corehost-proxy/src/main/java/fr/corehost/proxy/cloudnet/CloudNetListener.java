package fr.corehost.proxy.cloudnet;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.api.party.PartyManager;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CloudNetListener {

    private final CoreHostProxy plugin;
    private final ProxyServer server;

    public CloudNetListener(CoreHostProxy plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @EventListener
    public void onServiceLifecycleChange(CloudServiceLifecycleChangeEvent event) {
        if (event.newLifeCycle() == ServiceLifeCycle.RUNNING) {
            ServiceInfoSnapshot serviceInfo = event.serviceInfo();
            String serverName = serviceInfo.name();

            HostManager hostManager = plugin.getHostManager();
            if (hostManager == null) return;

            // Wait a small moment to let Velocity register the server internally from CloudNet-Bridge
            server.getScheduler().buildTask(plugin, () -> {
                List<HostData> hosts = hostManager.getAllHosts();
                
                for (HostData host : hosts) {
                        // Si ce serveur correspond a un Host en cours de demarrage
                    if (serverName.equalsIgnoreCase(host.getServerName()) && fr.corehost.api.host.HostStatus.STARTING == host.getStatus()) {
                        
                        // Mettre a jour l'etat dans Redis
                        host.setStatus(fr.corehost.api.host.HostStatus.WAITING);
                        hostManager.saveHost(host);
                        
                        // Teleporter le proprietaire
                        UUID ownerId = host.getOwnerUuid();
                        Optional<Player> ownerOpt = server.getPlayer(ownerId);
                        
                        Optional<RegisteredServer> targetServer = server.getServer(serverName);
                        if (targetServer.isEmpty()) {
                            // plugin.getLogger() is not publicly available directly, maybe just use System.out or server.getConsoleCommandSource()
                            return;
                        }
                        
                        if (ownerOpt.isPresent()) {
                            Player owner = ownerOpt.get();
                            owner.sendMessage(Component.text("Votre serveur Host ", NamedTextColor.GRAY)
                                    .append(Component.text(serverName, NamedTextColor.GOLD))
                                    .append(Component.text(" est prêt ! Téléportation en cours...", NamedTextColor.GRAY)));
                                    
                            owner.createConnectionRequest(targetServer.get()).connect();
                            
                            // Teleporter la Party du proprietaire s'il y en a une
                            PartyManager partyManager = plugin.getPartyManager();
                            if (partyManager != null) {
                                UUID partyLeader = partyManager.getPartyLeader(ownerId);
                                if (partyLeader != null && partyLeader.equals(ownerId)) {
                                    java.util.Set<UUID> members = partyManager.getPartyMembers(partyLeader);
                                    for (UUID memberId : members) {
                                        if (!memberId.equals(ownerId)) {
                                            server.getPlayer(memberId).ifPresent(member -> {
                                                member.sendMessage(Component.text("Le Host de la party est prêt ! Téléportation en cours...", NamedTextColor.GRAY));
                                                member.createConnectionRequest(targetServer.get()).connect();
                                            });
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }).delay(2, TimeUnit.SECONDS).schedule(); // Delai de securite
        }
    }
}
