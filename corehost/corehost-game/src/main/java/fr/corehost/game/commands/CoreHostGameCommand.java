package fr.corehost.game.commands;

import fr.corehost.game.CoreHostGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoreHostGameCommand implements CommandExecutor {

    private static final String GAME_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CoreHost" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    private final CoreHostGame plugin;

    public CoreHostGameCommand(CoreHostGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            sender.sendMessage(GAME_PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(GAME_PREFIX + ChatColor.GREEN + "Configuration de CoreHostGame rechargée !");
            return true;
        }

        sender.sendMessage(GAME_PREFIX + ChatColor.RED + "Usage: /corehostgame reload");
        return true;
    }
}
