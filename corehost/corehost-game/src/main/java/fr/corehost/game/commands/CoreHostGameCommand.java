package fr.corehost.game.commands;

import fr.corehost.game.CoreHostGame;
import fr.corehost.api.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation")
public class CoreHostGameCommand implements CommandExecutor {

    private static final String GAME_PREFIX = CC.DARK_GRAY + "[" + CC.GOLD + "CoreHost" + CC.DARK_GRAY + "] " + CC.GRAY;
    private final CoreHostGame plugin;

    public CoreHostGameCommand(CoreHostGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("corehost.admin")) {
            sender.sendMessage(GAME_PREFIX + CC.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(GAME_PREFIX + CC.GREEN + "Configuration de CoreHostGame rechargée !");
            return true;
        }

        sender.sendMessage(GAME_PREFIX + CC.RED + "Usage: /corehostgame reload");
        return true;
    }
}
