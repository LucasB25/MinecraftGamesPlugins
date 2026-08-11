package fr.corehost.dac.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import fr.corehost.dac.CoreHostDac;
import fr.corehost.dac.DacGameInstance;
import fr.corehost.dac.DacMapConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DacSetupCommand implements CommandExecutor, TabCompleter {

    private final CoreHostDac plugin;
    private final List<String> actions = Arrays.asList("setdivingboard", "setspectator", "setpoolmin", "setpoolmax", "save", "tp");

    public DacSetupCommand(CoreHostDac plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("corehost.admin")) {
            player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Usage: /dacsetup <mapName> <action>");
            return true;
        }

        String mapName = args[0];
        String action = args[1].toLowerCase();

        DacMapConfig mapConfig = plugin.getMapManager().getOrCreateMap(mapName);

        switch (action) {
            case "setdivingboard":
                mapConfig.setDivingBoardSpawn(player.getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Plongeoir (Diving Board) défini pour la map " + mapName);
                break;
            case "setspectator":
                mapConfig.setSpectatorSpawn(player.getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Spawn spectateur défini pour la map " + mapName);
                break;
            case "setpoolmin":
                mapConfig.setPoolMin(player.getLocation().getBlock().getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Point Min de la piscine défini pour la map " + mapName);
                break;
            case "setpoolmax":
                mapConfig.setPoolMax(player.getLocation().getBlock().getLocation());
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Point Max de la piscine défini pour la map " + mapName);
                break;
            case "save":
                plugin.getMapManager().saveMap(mapConfig);
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Configuration de la map " + mapName + " sauvegardée avec succès!");
                break;
            case "tp":
                org.bukkit.Location target = null;
                if (mapConfig.getSpectatorSpawn() != null) target = mapConfig.getSpectatorSpawn();
                else if (mapConfig.getDivingBoardSpawn() != null) target = mapConfig.getDivingBoardSpawn();
                
                String targetWorldName = (target != null && target.getWorld() != null) ? target.getWorld().getName() : mapName;
                if (target != null && target.getWorld() == null) {
                    targetWorldName = mapConfig.getTemplateName();
                }

                org.bukkit.World w = org.bukkit.Bukkit.getWorld(targetWorldName);
                if (w == null) {
                    java.io.File slimeFile = new java.io.File("slime_worlds", targetWorldName + ".slime");
                    java.io.File worldFolder = new java.io.File(org.bukkit.Bukkit.getWorldContainer(), targetWorldName);
                    
                    if (slimeFile.exists()) {
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "sw load " + targetWorldName);
                        player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.YELLOW + "Chargement du SlimeWorld " + targetWorldName + " en cours...");
                        
                        org.bukkit.Location finalTarget = target;
                        String finalWorldName = targetWorldName;
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            org.bukkit.World loaded = org.bukkit.Bukkit.getWorld(finalWorldName);
                            if (loaded != null) {
                                if (finalTarget != null) {
                                    finalTarget.setWorld(loaded);
                                    player.teleport(finalTarget);
                                } else {
                                    player.teleport(loaded.getSpawnLocation());
                                }
                                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Monde chargé ! Téléportation effectuée.");
                            } else {
                                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Le monde " + finalWorldName + " n'a pas pu être chargé.");
                            }
                        }, 40L);
                    } else if (worldFolder.exists() && worldFolder.isDirectory()) {
                        w = org.bukkit.Bukkit.createWorld(new org.bukkit.WorldCreator(targetWorldName));
                        if (w != null) {
                            if (target != null) {
                                target.setWorld(w);
                                player.teleport(target);
                            } else {
                                player.teleport(w.getSpawnLocation());
                            }
                            player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Monde chargé et téléporté.");
                        } else {
                            player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Erreur de chargement du monde.");
                        }
                    } else {
                        player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Monde " + targetWorldName + " introuvable sur le disque.");
                    }
                } else {
                    if (target != null) {
                        if (target.getWorld() == null) target.setWorld(w);
                        player.teleport(target);
                        player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Téléporté au point défini.");
                    } else {
                        player.teleport(w.getSpawnLocation());
                        player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.GREEN + "Téléporté au spawn du monde " + targetWorldName);
                    }
                }
                break;
            default:
                player.sendMessage(DacGameInstance.DAC_PREFIX + ChatColor.RED + "Action inconnue. Actions valides: " + String.join(", ", actions));
                break;
        }

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
