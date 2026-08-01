package fr.corehost.proxy.cloudnet;

import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.proxy.CoreHostProxy;
import com.google.gson.JsonObject;

import java.util.List;
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
                    // Si ce serveur correspond a un Host en cours de demarrage (attendant un demarrage CloudNet complet)
                    if (serverName.equalsIgnoreCase(host.getServerName()) && fr.corehost.api.host.HostStatus.STARTING == host.getStatus()) {
                        
                        plugin.getLogger().info("CloudNet Server " + serverName + " is now RUNNING. Requesting Slime world creation for Host " + host.getWorldName());
                        
                        JsonObject request = new JsonObject();
                        request.addProperty("action", "create_slime_instance");
                        request.addProperty("hostId", host.getWorldName());
                        request.addProperty("gameType", host.getGameType());
                        
                        plugin.getRedisManager().publish("corehost:game:" + serverName, request.toString());
                    }
                }
            }).delay(2, TimeUnit.SECONDS).schedule(); // Delai de securite
        }
    }
}
