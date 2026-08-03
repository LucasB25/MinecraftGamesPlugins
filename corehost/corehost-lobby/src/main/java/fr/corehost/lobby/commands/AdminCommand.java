package fr.corehost.lobby.commands;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.UUID;

import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.Constants;

public class AdminCommand implements TabExecutor {

    public static final Set<UUID> buildModePlayers = new HashSet<>();
    private final CoreHostLobby plugin;

    public AdminCommand(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Constants.PREFIX + ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("corehost.admin")) {
            player.sendMessage(Constants.PREFIX + ChatColor.RED + "Vous n'avez pas la permission d'exécuter cette commande.");
            return true;
        }

        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            switch (arg) {
                case "sethologram":
                    plugin.getParkourManager().setHologramLocation(player.getLocation());
                    player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Position de l'hologramme du parkour définie à votre position.");
                    return true;
                case "setstart":
                    plugin.getParkourManager().setStartPlate(player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Plaque de départ définie.");
                    return true;
                case "setend":
                    plugin.getParkourManager().setEndPlate(player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Plaque de fin définie.");
                    return true;
                case "addcheckpoint":
                    plugin.getParkourManager().addCheckpoint(player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Checkpoint ajouté à votre position.");
                    return true;
                case "clearcheckpoints":
                    plugin.getParkourManager().clearCheckpoints();
                    player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Tous les checkpoints ont été supprimés.");
                    return true;
            }
        }

        if (buildModePlayers.contains(player.getUniqueId())) {
            buildModePlayers.remove(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Constants.PREFIX + ChatColor.RED + "Mode Admin (Build) désactivé.");
        } else {
            buildModePlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Mode Admin (Build) activé. Vous êtes en mode créatif.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> commands = Arrays.asList("sethologram", "setstart", "setend", "addcheckpoint", "clearcheckpoints");
            
            for (String c : commands) {
                if (c.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
