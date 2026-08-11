package fr.corehost.lobby.commands;

import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.HashSet;
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
            sender.sendMessage(Constants.PREFIX + CC.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("corehost.admin")) {
            player.sendMessage(Constants.PREFIX + CC.RED + "Vous n'avez pas la permission d'exécuter cette commande.");
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("parkour")) {
            String courseId = args[1].toLowerCase();
            String sub = args[2].toLowerCase();
            
            if (!courseId.equals("easy") && !courseId.equals("hard")) {
                player.sendMessage(Constants.PREFIX + CC.RED + "Parcours invalide. Utilisez 'easy' ou 'hard'.");
                return true;
            }
            
            switch (sub) {
                case "sethologram":
                    plugin.getParkourManager().setHologramLocation(courseId, player.getLocation());
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Hologramme du parkour " + courseId + " défini.");
                    return true;
                case "setstart":
                    plugin.getParkourManager().setStartPlate(courseId, player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Plaque de départ du parkour " + courseId + " définie.");
                    return true;
                case "setend":
                    plugin.getParkourManager().setEndPlate(courseId, player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Plaque de fin du parkour " + courseId + " définie.");
                    return true;
                case "addcheckpoint":
                    plugin.getParkourManager().addCheckpoint(courseId, player.getLocation().getBlock().getLocation());
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Checkpoint ajouté pour le parkour " + courseId + ".");
                    return true;
                case "clearcheckpoints":
                    plugin.getParkourManager().clearCheckpoints(courseId);
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "Checkpoints effacés pour le parkour " + courseId + ".");
                    return true;
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("headhunt")) {
            String sub = args[1].toLowerCase();
            switch (sub) {
                case "add":
                    org.bukkit.block.Block targetBlockAdd = player.getTargetBlockExact(5);
                    if (targetBlockAdd != null && (targetBlockAdd.getType() == org.bukkit.Material.PLAYER_HEAD || targetBlockAdd.getType() == org.bukkit.Material.PLAYER_WALL_HEAD)) {
                        boolean added = plugin.getHeadHuntManager().addHead(targetBlockAdd.getLocation());
                        if (added) {
                            player.sendMessage(Constants.PREFIX + CC.GREEN + "Tête ajoutée à la chasse aux trésors !");
                        } else {
                            player.sendMessage(Constants.PREFIX + CC.RED + "Cette tête fait déjà partie de la chasse.");
                        }
                    } else {
                        player.sendMessage(Constants.PREFIX + CC.RED + "Vous devez regarder une tête de joueur (à moins de 5 blocs).");
                    }
                    return true;
                case "remove":
                    org.bukkit.block.Block targetBlockRemove = player.getTargetBlockExact(5);
                    if (targetBlockRemove != null && (targetBlockRemove.getType() == org.bukkit.Material.PLAYER_HEAD || targetBlockRemove.getType() == org.bukkit.Material.PLAYER_WALL_HEAD)) {
                        boolean removed = plugin.getHeadHuntManager().removeHead(targetBlockRemove.getLocation());
                        if (removed) {
                            player.sendMessage(Constants.PREFIX + CC.GREEN + "Tête retirée de la chasse aux trésors !");
                        } else {
                            player.sendMessage(Constants.PREFIX + CC.RED + "Cette tête ne faisait pas partie de la chasse.");
                        }
                    } else {
                        player.sendMessage(Constants.PREFIX + CC.RED + "Vous devez regarder une tête de joueur (à moins de 5 blocs).");
                    }
                    return true;
                case "list":
                    player.sendMessage(Constants.PREFIX + CC.YELLOW + "Têtes enregistrées : " + plugin.getHeadHuntManager().getTotalHeads());
                    java.util.List<org.bukkit.Location> heads = plugin.getHeadHuntManager().getHeads();
                    for (int i = 0; i < heads.size(); i++) {
                        org.bukkit.Location loc = heads.get(i);
                        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(" - Tête #" + i + " : " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                        msg.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                        msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/admin headhunt tp " + i));
                        msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(CC.GREEN + "Clique pour te téléporter !")));
                        player.spigot().sendMessage(msg);
                    }
                    return true;
                case "tp":
                    if (args.length == 3) {
                        try {
                            int index = Integer.parseInt(args[2]);
                            java.util.List<org.bukkit.Location> headList = plugin.getHeadHuntManager().getHeads();
                            if (index >= 0 && index < headList.size()) {
                                org.bukkit.Location target = headList.get(index).clone();
                                // Add 0.5 to X/Z and some Y to spawn on top safely
                                target.add(0.5, 1, 0.5);
                                player.teleport(target);
                                player.sendMessage(Constants.PREFIX + CC.GREEN + "Téléporté à la tête #" + index);
                            } else {
                                player.sendMessage(Constants.PREFIX + CC.RED + "Index invalide.");
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(Constants.PREFIX + CC.RED + "Index invalide.");
                        }
                    }
                    return true;
                case "reset":
                    if (args.length == 3) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target != null) {
                            plugin.getHeadHuntManager().resetPlayer(target.getUniqueId());
                            player.sendMessage(Constants.PREFIX + CC.GREEN + "Progression HeadHunt de " + target.getName() + " réinitialisée !");
                            target.sendMessage(Constants.PREFIX + CC.YELLOW + "Votre progression des têtes cachées a été réinitialisée.");
                            if (plugin.getScoreboardManager() != null) {
                                Bukkit.getScheduler().runTask(plugin, () -> plugin.getScoreboardManager().updateScoreboard(target));
                            }
                        } else {
                            player.sendMessage(Constants.PREFIX + CC.RED + "Joueur introuvable.");
                        }
                    } else {
                        player.sendMessage(Constants.PREFIX + CC.RED + "Usage: /admin headhunt reset <joueur>");
                    }
                    return true;
            }
        }

        if (buildModePlayers.contains(player.getUniqueId())) {
            buildModePlayers.remove(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Constants.PREFIX + CC.RED + "Mode Admin (Build) désactivé.");
        } else {
            buildModePlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(Constants.PREFIX + CC.GREEN + "Mode Admin (Build) activé. Vous êtes en mode créatif.");
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            java.util.List<String> commands = java.util.Arrays.asList("parkour", "headhunt");
            
            for (String c : commands) {
                if (c.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("parkour")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            java.util.List<String> courses = java.util.Arrays.asList("easy", "hard");
            for (String c : courses) {
                if (c.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("parkour")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            java.util.List<String> subCommands = java.util.Arrays.asList("sethologram", "setstart", "setend", "addcheckpoint", "clearcheckpoints");
            for (String c : subCommands) {
                if (c.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("headhunt")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            java.util.List<String> subCommands = java.util.Arrays.asList("add", "remove", "list", "tp", "reset");
            
            for (String c : subCommands) {
                if (c.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(c);
                }
            }
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("headhunt") && args[1].equalsIgnoreCase("tp")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            int total = plugin.getHeadHuntManager().getTotalHeads();
            for (int i = 0; i < total; i++) {
                if (String.valueOf(i).startsWith(args[2])) {
                    completions.add(String.valueOf(i));
                }
            }
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("headhunt") && args[1].equalsIgnoreCase("reset")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        return new java.util.ArrayList<>();
    }
}
