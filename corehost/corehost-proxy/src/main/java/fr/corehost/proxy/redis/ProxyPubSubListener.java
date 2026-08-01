package fr.corehost.proxy.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.api.party.PartyManager;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProxyPubSubListener extends JedisPubSub {

    private final CoreHostProxy plugin;
    private final ProxyServer server;
    private final Gson gson = new Gson();

    public ProxyPubSubListener(CoreHostProxy plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals("corehost:proxy:events")) return;

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.get("action").getAsString();

            if ("HOST_READY".equals(action)) {
                String worldName = json.get("hostId").getAsString();
                String serverName = json.get("serverName").getAsString();

                HostManager hostManager = plugin.getHostManager();
                if (hostManager == null) return;

                // Trouver le host correspondant
                HostData targetHost = null;
                for (HostData h : hostManager.getAllHosts()) {
                    if (h.getWorldName().equals(worldName) && h.getServerName().equalsIgnoreCase(serverName)) {
                        targetHost = h;
                        break;
                    }
                }

                if (targetHost == null) return;

                targetHost.setStatus(fr.corehost.api.host.HostStatus.WAITING);
                hostManager.saveHost(targetHost);

                UUID ownerId = targetHost.getOwnerUuid();
                Optional<Player> ownerOpt = server.getPlayer(ownerId);

                Optional<RegisteredServer> targetServer = server.getServer(serverName);
                if (targetServer.isEmpty()) {
                    plugin.getLogger().warn("Le serveur Host " + serverName + " est introuvable par Velocity !");
                    return;
                }

                if (ownerOpt.isPresent()) {
                    Player owner = ownerOpt.get();
                    owner.sendMessage(Component.text("Votre serveur Host ", NamedTextColor.GRAY)
                            .append(Component.text(targetHost.getGameType(), NamedTextColor.GOLD))
                            .append(Component.text(" est prêt ! Téléportation en cours...", NamedTextColor.GRAY)));

                    owner.createConnectionRequest(targetServer.get()).connect();

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
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erreur PubSub Proxy: ", e);
        }
    }
}
