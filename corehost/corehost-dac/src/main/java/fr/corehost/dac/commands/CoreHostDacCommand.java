package fr.corehost.dac.commands;

import fr.corehost.dac.CoreHostDac;
import fr.corehost.dac.DacGameInstance;
import fr.corehost.api.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoreHostDacCommand implements CommandExecutor {

    private final CoreHostDac plugin;

    public CoreHostDacCommand(CoreHostDac plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            sender.sendMessage(DacGameInstance.DAC_PREFIX + CC.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getMapManager().reloadMaps();
            sender.sendMessage(DacGameInstance.DAC_PREFIX + CC.GREEN + "Configuration de CoreHostDac rechargée !");
            return true;
        }

        sender.sendMessage(DacGameInstance.DAC_PREFIX + CC.RED + "Usage: /corehostdac reload");
        return true;
    }
}
