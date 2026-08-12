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
import fr.corehost.proxy.utils.ProxyPrefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import redis.clients.jedis.JedisPubSub;

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

                HostData targetHost = null;
                for (HostData h : hostManager.getAllHosts().join()) {
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
                    if (!serverName.equals("Unknown-1") && !serverName.equals("local")) {
                        plugin.getLogger().warn("Le serveur Host " + serverName + " est introuvable par Velocity !");
                    }
                    return;
                }

                if (ownerOpt.isPresent()) {
                    Player owner = ownerOpt.get();
                    owner.sendMessage(ProxyPrefix.get().append(Component.text("Votre serveur Host ", NamedTextColor.GRAY)
                            .append(Component.text(targetHost.getGameType(), NamedTextColor.GOLD))
                            .append(Component.text(" est prêt ! Téléportation en cours...", NamedTextColor.GRAY))));

                    owner.createConnectionRequest(targetServer.get()).connect();

                    PartyManager partyManager = plugin.getPartyManager();
                    if (partyManager != null) {
                        UUID partyLeader = partyManager.getPartyLeader(ownerId);
                        if (partyLeader != null && partyLeader.equals(ownerId)) {
                            java.util.Set<UUID> members = partyManager.getPartyMembers(partyLeader);
                            for (UUID memberId : members) {
                                if (!memberId.equals(ownerId)) {
                                    server.getPlayer(memberId).ifPresent(member -> {
                                        member.sendMessage(ProxyPrefix.message("Le Host de la party est prêt ! Téléportation en cours...", NamedTextColor.GRAY));
                                        member.createConnectionRequest(targetServer.get()).connect();
                                    });
                                }
                            }
                        }
                    }
                }
            } else if ("ADD_COINS".equals(action)) {
                String uuidStr = json.get("uuid").getAsString();
                int amount = json.get("amount").getAsInt();
                
                if (plugin.getProfileManager() != null) {
                    plugin.getProfileManager().addCoins(UUID.fromString(uuidStr), amount);
                }
            } else if ("ADD_STAT".equals(action)) {
                String uuidStr = json.get("uuid").getAsString();
                String game = json.get("game").getAsString();
                String statKey = json.get("statKey").getAsString();
                int amount = json.get("amount").getAsInt();
                
                if (plugin.getDatabaseManager() != null) {
                    plugin.getDatabaseManager().getStatsDAO().addStat(UUID.fromString(uuidStr), game, statKey, amount);
                    
                    // Force update cache on all servers
                    if (plugin.getProfileManager() != null) {
                        plugin.getProfileManager().publishProfileUpdate(UUID.fromString(uuidStr));
                    }
                }
            } else if ("TELEPORT_STAFF".equals(action)) {
                String staffUuidStr = json.get("staffUuid").getAsString();
                String targetName = json.get("targetName").getAsString();
                
                Optional<Player> staffOpt = server.getPlayer(UUID.fromString(staffUuidStr));
                Optional<Player> targetOpt = server.getPlayer(targetName);
                
                if (staffOpt.isPresent() && targetOpt.isPresent()) {
                    Player staff = staffOpt.get();
                    Player target = targetOpt.get();
                    
                    if (staff.getCurrentServer().isPresent() && target.getCurrentServer().isPresent()) {
                        RegisteredServer targetServer = target.getCurrentServer().get().getServer();
                        
                        if (!staff.getCurrentServer().get().getServer().getServerInfo().getName().equals(targetServer.getServerInfo().getName())) {
                            staff.createConnectionRequest(targetServer).connect();
                        }
                    }
                }
            } else if ("REQUEST_STAFF_LIST".equals(action)) {
                String requesterUuid = json.get("requesterUuid").getAsString();
                com.google.gson.JsonArray staffArray = new com.google.gson.JsonArray();
                
                for (Player p : server.getAllPlayers()) {
                    if (p.hasPermission("staffmod.mod")) {
                        JsonObject staffObj = new JsonObject();
                        staffObj.addProperty("name", p.getUsername());
                        staffObj.addProperty("server", p.getCurrentServer().isPresent() ? p.getCurrentServer().get().getServer().getServerInfo().getName() : "Inconnu");
                        staffArray.add(staffObj);
                    }
                }
                
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("action", "STAFF_LIST_RESPONSE");
                responseJson.addProperty("requesterUuid", requesterUuid);
                responseJson.add("staffList", staffArray);
                
                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().publish("corehost:staff:events", responseJson.toString());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erreur PubSub Proxy: ", e);
        }
    }
}
