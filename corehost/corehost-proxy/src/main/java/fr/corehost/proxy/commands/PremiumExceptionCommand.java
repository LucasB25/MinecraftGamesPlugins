package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PremiumExceptionCommand implements SimpleCommand {

    private final CoreHostProxy plugin;

    public PremiumExceptionCommand(CoreHostProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("corehost.admin");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            String current = args.length == 0 ? "" : args[0].toLowerCase();
            return Stream.of("add", "remove")
                    .filter(cmd -> cmd.startsWith(current))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            source.sendMessage(Component.text("Le système Redis est actuellement indisponible.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(Component.text("Utilisation : /premiumexception <add|remove> <pseudo>", NamedTextColor.RED));
            return;
        }

        String action = args[0].toLowerCase();
        String username = args[1].toLowerCase();
        String redisKey = "corehost:auth:exception:" + username;

        if (action.equals("add")) {
            plugin.getRedisManager().set(redisKey, "true");
            source.sendMessage(Component.text("Exception premium ajoutée pour le joueur " + username + ".", NamedTextColor.GREEN));
        } else if (action.equals("remove")) {
            plugin.getRedisManager().del(redisKey);
            source.sendMessage(Component.text("Exception premium retirée pour le joueur " + username + ".", NamedTextColor.YELLOW));
        } else {
            source.sendMessage(Component.text("Action inconnue. Utilisation : /premiumexception <add|remove> <pseudo>", NamedTextColor.RED));
        }
    }
}
