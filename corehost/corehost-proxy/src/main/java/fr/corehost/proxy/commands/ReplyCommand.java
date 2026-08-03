package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.Optional;
import java.util.UUID;

public class ReplyCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer proxy;

    public ReplyCommand(CoreHostProxy plugin, ProxyServer proxy) {
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
            source.sendMessage(ProxyPrefix.message("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("corehost.command.reply")) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez pas la permission.", NamedTextColor.RED));
            return;
        }
        String[] args = invocation.arguments();

        if (args.length < 1) {
            player.sendMessage(ProxyPrefix.message("Utilisation : /r <message>", NamedTextColor.RED));
            return;
        }

        UUID targetUuid = plugin.getMessageManager().getLastMessaged(player.getUniqueId());

        if (targetUuid == null) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez personne à qui répondre.", NamedTextColor.RED));
            return;
        }

        Optional<Player> targetOpt = proxy.getPlayer(targetUuid);

        if (targetOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Le joueur n'est plus connecté.", NamedTextColor.RED));
            plugin.getMessageManager().removeLastMessaged(player.getUniqueId());
            return;
        }

        Player target = targetOpt.get();

        if (plugin.getMessageManager().isMessagesBlocked(target.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Ce joueur n'accepte pas les messages privés.", NamedTextColor.RED));
            return;
        }

        if (plugin.getMessageManager().isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous ne pouvez pas envoyer de message à ce joueur.", NamedTextColor.RED));
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
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
}
