package fr.corehost.game.commands;

import fr.corehost.game.CoreHostGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoreHostGameCommand implements CommandExecutor {

    private final CoreHostGame plugin;

    public CoreHostGameCommand(CoreHostGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration de CoreHostGame rechargée !");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /corehostgame reload");
        return true;
    }
}
