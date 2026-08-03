package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IgnoreCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer proxy;

    public IgnoreCommand(CoreHostProxy plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("corehost.command.ignore")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED));
            return;
        }
        String[] args = invocation.arguments();

        if (args.length != 1) {
            player.sendMessage(ProxyPrefix.message("Utilisation : /ignore <joueur>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = proxy.getPlayer(targetName);

        // For simplicity we require the player to be online to ignore them by name, 
        // because we don't have a reliable UUID fetcher by name in proxy without FriendManager/DB
        if (targetOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Joueur introuvable ou hors ligne.", NamedTextColor.RED));
            return;
        }

        Player target = targetOpt.get();

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous ne pouvez pas vous ignorer vous-même.", NamedTextColor.RED));
            return;
        }

        boolean isIgnoring = plugin.getMessageManager().isIgnoring(player.getUniqueId(), target.getUniqueId());

        if (isIgnoring) {
            plugin.getMessageManager().removeIgnore(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(ProxyPrefix.message("Vous n'ignorez plus " + target.getUsername() + ".", NamedTextColor.GREEN));
        } else {
            plugin.getMessageManager().addIgnore(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(ProxyPrefix.message("Vous ignorez désormais " + target.getUsername() + ".", NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String current = args.length == 0 ? "" : args[0].toLowerCase();
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(current))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
