package fr.corehost.lobby.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import fr.corehost.lobby.CoreHostLobby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PremiumExceptionCommand implements TabExecutor {

    private final CoreHostLobby plugin;

    public PremiumExceptionCommand(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'exécuter cette commande.");
            return true;
        }

        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            sender.sendMessage(ChatColor.RED + "Le système Redis est actuellement indisponible.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Utilisation : /premiumexception <add|remove> <pseudo>");
            return true;
        }

        String action = args[0].toLowerCase();
        String username = args[1].toLowerCase();
        String redisKey = "corehost:auth:exception:" + username;

        if (action.equals("add")) {
            plugin.getRedisManager().set(redisKey, "true");
            sender.sendMessage(ChatColor.GREEN + "Exception premium ajoutée pour le joueur " + username + ".");
        } else if (action.equals("remove")) {
            plugin.getRedisManager().del(redisKey);
            sender.sendMessage(ChatColor.YELLOW + "Exception premium retirée pour le joueur " + username + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Action inconnue. Utilisation : /premiumexception <add|remove> <pseudo>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
