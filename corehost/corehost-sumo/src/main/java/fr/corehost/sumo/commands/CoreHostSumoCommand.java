package fr.corehost.sumo.commands;

import fr.corehost.sumo.CoreHostSumo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoreHostSumoCommand implements CommandExecutor {

    private final CoreHostSumo plugin;

    public CoreHostSumoCommand(CoreHostSumo plugin) {
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
            plugin.getMapManager().reloadMaps();
            sender.sendMessage(ChatColor.GREEN + "Configuration de CoreHostSumo rechargée !");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /corehostsumo reload");
        return true;
    }
}
