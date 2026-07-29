package fr.corehost.lobby.commands;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.TabCompleter;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final CoreHostLobby plugin;

    public FriendCommand(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            return Stream.of("add", "remove", "accept", "deny", "list", "notifications", "help")
                    .filter(cmd -> cmd.startsWith(current))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("accept") || sub.equals("deny")) {
                String current = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(current))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est réservée aux joueurs.");
            return true;
        }

        // Le code d'exécution de la commande /friend est géré entièrement
        // par le proxy Velocity (CoreHostProxy).
        // Cette classe sert uniquement à fournir l'autocomplétion (TabCompleter)
        // au client Minecraft afin que les arguments s'affichent correctement.

        return true;
    }
}
