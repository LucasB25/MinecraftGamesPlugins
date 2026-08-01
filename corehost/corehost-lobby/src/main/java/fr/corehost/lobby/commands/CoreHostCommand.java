package fr.corehost.lobby.commands;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoreHostCommand implements CommandExecutor {

    private final CoreHostLobby plugin;

    public CoreHostCommand(CoreHostLobby plugin) {
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
            fr.corehost.lobby.utils.Constants.load(plugin.getConfig());
            sender.sendMessage(ChatColor.GREEN + "Configuration de CoreHostLobby rechargée !");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /corehost reload");
        return true;
    }
}
