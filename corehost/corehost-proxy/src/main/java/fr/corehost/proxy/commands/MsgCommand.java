package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MsgCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer proxy;

    public MsgCommand(CoreHostProxy plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        String[] args = invocation.arguments();

        if (args.length < 2) {
            player.sendMessage(Component.text("Utilisation : /msg <joueur> <message>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = proxy.getPlayer(targetName);

        if (targetOpt.isEmpty()) {
            player.sendMessage(Component.text("Joueur introuvable ou hors ligne.", NamedTextColor.RED));
            return;
        }

        Player target = targetOpt.get();

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Component.text("Vous ne pouvez pas vous envoyer un message à vous-même.", NamedTextColor.RED));
            return;
        }

        if (plugin.getMessageManager().isMessagesBlocked(target.getUniqueId())) {
            player.sendMessage(Component.text("Ce joueur n'accepte pas les messages privés.", NamedTextColor.RED));
            return;
        }

        if (plugin.getMessageManager().isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(Component.text("Vous ne pouvez pas envoyer de message à ce joueur.", NamedTextColor.RED));
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Format: [Moi -> Joueur] message
        Component senderFormat = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Moi", NamedTextColor.AQUA))
                .append(Component.text(" -> ", NamedTextColor.GRAY))
                .append(Component.text(target.getUsername(), NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        // Format: [Joueur -> Moi] message
        Component targetFormat = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(player.getUsername(), NamedTextColor.AQUA))
                .append(Component.text(" -> ", NamedTextColor.GRAY))
                .append(Component.text("Moi", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        player.sendMessage(senderFormat);
        target.sendMessage(targetFormat);

        plugin.getMessageManager().setLastMessaged(player.getUniqueId(), target.getUniqueId());
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
