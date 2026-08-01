package fr.corehost.sumo;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SumoSetupCommand implements CommandExecutor, TabCompleter {

    private final CoreHostSumo plugin;
    private final List<String> actions = Arrays.asList("setspawn1", "setspawn2", "setdeathheight", "save", "tp", "tp1", "tp2");

    public SumoSetupCommand(CoreHostSumo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("corehost.admin")) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /sumosetup <mapName> <action>");
            return true;
        }

        String mapName = args[0];
        String action = args[1].toLowerCase();

        SumoMapConfig mapConfig = plugin.getMapManager().getOrCreateMap(mapName);

        switch (action) {
            case "setspawn1":
                mapConfig.setSpawn1(player.getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(ChatColor.GREEN + "Spawn 1 défini et sauvegardé pour la map " + mapName);
                break;
            case "setspawn2":
                mapConfig.setSpawn2(player.getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(ChatColor.GREEN + "Spawn 2 défini et sauvegardé pour la map " + mapName);
                break;
            case "setdeathheight":
                mapConfig.setDeathHeight(player.getLocation().getBlockY());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(ChatColor.GREEN + "Death height défini à " + player.getLocation().getBlockY() + " et sauvegardé pour la map " + mapName);
                break;
            case "save":
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(ChatColor.GREEN + "Configuration de la map " + mapName + " sauvegardée avec succès!");
                break;
            case "tp":
            case "tp1":
                if (mapConfig.getSpawn1() != null) {
                    player.teleport(mapConfig.getSpawn1());
                    player.sendMessage(ChatColor.GREEN + "Téléporté au Spawn 1 de " + mapName);
                } else if (org.bukkit.Bukkit.getWorld(mapName) != null) {
                    player.teleport(org.bukkit.Bukkit.getWorld(mapName).getSpawnLocation());
                    player.sendMessage(ChatColor.GREEN + "Téléporté au spawn du monde " + mapName);
                } else {
                    player.sendMessage(ChatColor.RED + "Aucun spawn défini pour " + mapName + " et le monde n'est pas chargé.");
                }
                break;
            case "tp2":
                if (mapConfig.getSpawn2() != null) {
                    player.teleport(mapConfig.getSpawn2());
                    player.sendMessage(ChatColor.GREEN + "Téléporté au Spawn 2 de " + mapName);
                } else {
                    player.sendMessage(ChatColor.RED + "Aucun Spawn 2 défini pour la map " + mapName);
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "Action inconnue. Actions valides: " + String.join(", ", actions));
                break;
        }

        // We update the map config in memory, but don't save to file unless they run "save"
        // Except actually the manager caches it, so that's fine.
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("corehost.admin")) return new ArrayList<>();

        if (args.length == 1) {
            String partialMap = args[0].toLowerCase();
            java.util.Set<String> maps = new java.util.HashSet<>(plugin.getMapManager().getLoadedMaps().keySet());
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                maps.add(world.getName().toLowerCase());
            }
            return maps.stream()
                    .filter(name -> name.startsWith(partialMap))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String partialAction = args[1].toLowerCase();
            return actions.stream()
                    .filter(a -> a.startsWith(partialAction))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

}
