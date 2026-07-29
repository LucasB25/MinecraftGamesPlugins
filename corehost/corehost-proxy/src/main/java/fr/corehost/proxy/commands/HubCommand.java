package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class HubCommand implements SimpleCommand {

    private final ProxyServer server;

    public HubCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("corehost.command.hub");
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Seuls les joueurs peuvent utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();

        // Check if the player is already on the Lobby server
        Optional<RegisteredServer> currentServer = player.getCurrentServer().map(serverConnection -> serverConnection.getServer());
        if (currentServer.isPresent() && currentServer.get().getServerInfo().getName().toLowerCase().contains("lobby")) {
            player.sendMessage(Component.text("Vous êtes déjà sur le Lobby.", NamedTextColor.RED));
            return;
        }

        // Try to find a Lobby server
        Optional<RegisteredServer> lobbyServer = server.getAllServers().stream()
                .filter(s -> s.getServerInfo().getName().toLowerCase().contains("lobby"))
                .findFirst();

        if (lobbyServer.isPresent()) {
            player.sendMessage(Component.text("Redirection vers le Hub...", NamedTextColor.GREEN));
            player.createConnectionRequest(lobbyServer.get()).fireAndForget();
        } else {
            player.sendMessage(Component.text("Aucun Hub n'est disponible pour le moment.", NamedTextColor.RED));
        }
    }
}
