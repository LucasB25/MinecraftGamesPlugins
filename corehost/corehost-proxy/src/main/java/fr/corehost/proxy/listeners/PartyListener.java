package fr.corehost.proxy.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.corehost.proxy.CoreHostProxy;
import fr.corehost.api.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PartyListener {

    private final CoreHostProxy plugin;
    private final PartyManager partyManager;

    public PartyListener(CoreHostProxy plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        UUID playerUuid = player.getUniqueId();
        
        UUID leaderUuid = partyManager.getPartyLeader(playerUuid);
        if (leaderUuid != null && leaderUuid.equals(playerUuid)) {
            // Le joueur est chef de groupe. On emmène les membres avec lui.
            Set<UUID> members = partyManager.getPartyMembers(leaderUuid);
            
            for (UUID memberUuid : members) {
                if (!memberUuid.equals(leaderUuid)) {
                    Optional<Player> memberOpt = plugin.getServer().getPlayer(memberUuid);
                    if (memberOpt.isPresent()) {
                        Player member = memberOpt.get();
                        
                        // Ne pas téléporter s'ils sont déjà sur le serveur en train d'y aller
                        if (!member.getCurrentServer().isPresent() || !member.getCurrentServer().get().getServer().equals(server)) {
                            member.createConnectionRequest(server).fireAndForget();
                            member.sendMessage(ProxyPrefix.get().append(Component.text("Le chef de groupe a rejoint le serveur ", NamedTextColor.YELLOW)
                                    .append(Component.text(server.getServerInfo().getName(), NamedTextColor.GOLD))
                                    .append(Component.text(". Vous le suivez.", NamedTextColor.YELLOW))));
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        UUID leaderUuid = partyManager.getPartyLeader(playerUuid);
        if (leaderUuid != null) {
            if (leaderUuid.equals(playerUuid)) {
                // Le chef se déconnecte, on dissout le groupe.
                Set<UUID> members = partyManager.getPartyMembers(leaderUuid);
                partyManager.disbandParty(leaderUuid);
                
                for (UUID memberUuid : members) {
                    if (!memberUuid.equals(leaderUuid)) {
                        plugin.getServer().getPlayer(memberUuid).ifPresent(member -> {
                            member.sendMessage(ProxyPrefix.message("Le chef de groupe s'est déconnecté. Le groupe a été dissous.", NamedTextColor.RED));
                        });
                    }
                }
            } else {
                // Un membre se déconnecte, il quitte le groupe.
                partyManager.removeMember(playerUuid);
                plugin.getServer().getPlayer(leaderUuid).ifPresent(leader -> {
                    leader.sendMessage(ProxyPrefix.message(player.getUsername() + " s'est déconnecté et a quitté le groupe.", NamedTextColor.YELLOW));
                });
            }
        }
    }
}
