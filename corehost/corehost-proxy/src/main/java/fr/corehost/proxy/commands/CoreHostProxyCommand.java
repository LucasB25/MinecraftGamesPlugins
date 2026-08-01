package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CoreHostProxyCommand implements SimpleCommand {

    private final CoreHostProxy plugin;

    public CoreHostProxyCommand(CoreHostProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("corehost.admin")) {
            invocation.source().sendMessage(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            invocation.source().sendMessage(Component.text("Configuration de CoreHostProxy rechargée !", NamedTextColor.GREEN));
            return;
        }

        invocation.source().sendMessage(Component.text("Usage: /corehostproxy reload", NamedTextColor.RED));
    }
}
