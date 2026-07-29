package fr.corehost.lobby.commands;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.TabCompleter;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final CoreHostLobby plugin;

    public FriendCommand(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            return Stream.of("add", "remove", "accept", "deny", "list", "notifications", "help")
                    .filter(cmd -> cmd.startsWith(current))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("accept") || sub.equals("deny")) {
                String current = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(current))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getFriendManager() == null) {
            player.sendMessage(ChatColor.RED + "Le système d'amis est actuellement indisponible.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            handleList(player);
            return true;
        }
        
        if (sub.equals("notifications")) {
            handleNotifications(player);
            return true;
        }
        
        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Veuillez spécifier un pseudo.");
            return true;
        }

        String targetName = args[1];
        
        // Exécuter les requêtes Redis en asynchrone pour ne pas laguer le serveur Bukkit
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID targetUuid = plugin.getFriendManager().getUuidByName(targetName);

            if (targetUuid == null) {
                player.sendMessage(ChatColor.RED + "Ce joueur n'a jamais été sur le serveur.");
                return;
            }

            if (targetUuid.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Vous ne pouvez pas être ami avec vous-même.");
                return;
            }

            switch (sub) {
                case "add":
                    handleAdd(player, targetUuid, targetName);
                    break;
                case "accept":
                    handleAccept(player, targetUuid, targetName);
                    break;
                case "deny":
                    handleDeny(player, targetUuid, targetName);
                    break;
                case "remove":
                    handleRemove(player, targetUuid, targetName);
                    break;
                default:
                    sendHelp(player);
                    break;
            }
        });

        return true;
    }

    private void handleAdd(Player player, UUID targetUuid, String targetName) {
        if (plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.RED + "Vous êtes déjà ami avec ce joueur.");
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(targetUuid, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vous avez déjà envoyé une demande à ce joueur.");
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.YELLOW + "Ce joueur vous a déjà envoyé une demande. Faites /friend accept " + targetName);
            return;
        }
        
        if (plugin.getFriendManager().areFriendRequestsBlocked(targetUuid)) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'accepte pas les demandes d'amis.");
            return;
        }

        plugin.getFriendManager().sendFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ChatColor.GREEN + "Demande d'ami envoyée à " + targetName + ".");

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.YELLOW + "Vous avez reçu une demande d'ami de " + player.getName() + ".");
            
            net.kyori.adventure.text.Component acceptButton = net.kyori.adventure.text.Component.text("[ACCEPTER]")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/friend accept " + player.getName()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Accepter")));
                
            net.kyori.adventure.text.Component denyButton = net.kyori.adventure.text.Component.text(" [REFUSER]")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/friend deny " + player.getName()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Refuser")));
                
            targetPlayer.sendMessage(acceptButton.append(denyButton));
        }
    }

    private void handleAccept(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas de demande d'ami de ce joueur.");
            return;
        }

        Set<String> playerFriends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (playerFriends.size() >= 50) {
            player.sendMessage(ChatColor.RED + "Vous avez atteint la limite de 50 amis.");
            return;
        }

        plugin.getFriendManager().acceptFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ChatColor.GREEN + "Vous êtes désormais ami avec " + targetName + ".");

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.GREEN + player.getName() + " a accepté votre demande d'ami !");
        }
    }

    private void handleDeny(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas de demande d'ami de ce joueur.");
            return;
        }
        
        plugin.getFriendManager().denyFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ChatColor.YELLOW + "Demande d'ami refusée pour " + targetName + ".");
    }

    private void handleRemove(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas ami avec ce joueur.");
            return;
        }
        
        plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
        player.sendMessage(ChatColor.YELLOW + "Vous n'êtes plus ami avec " + targetName + ".");
    }

    private void handleNotifications(Player player) {
        boolean enabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());
        plugin.getFriendManager().setNotificationsEnabled(player.getUniqueId(), !enabled);
        
        if (!enabled) {
            player.sendMessage(ChatColor.GREEN + "Vous avez activé les notifications de connexion de vos amis.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Vous avez désactivé les notifications de connexion de vos amis.");
        }
    }

    private void handleList(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
            if (friends.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Vous n'avez aucun ami.");
                return;
            }

            player.sendMessage(ChatColor.GOLD + "--- Vos Amis (" + friends.size() + "/50) ---");
            for (String fUuid : friends) {
                String name = plugin.getFriendManager().getNameByUuid(UUID.fromString(fUuid));
                if (name == null) name = "Inconnu";
                
                boolean online = Bukkit.getPlayer(UUID.fromString(fUuid)) != null;
                String status = online ? ChatColor.GREEN + " [En ligne]" : ChatColor.RED + " [Hors ligne]";
                
                player.sendMessage(ChatColor.GRAY + "- " + name + status);
            }
        });
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "====== " + ChatColor.GOLD + "Système d'Amis" + ChatColor.AQUA + " ======");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend add <pseudo>" + ChatColor.GRAY + " - Ajouter un ami");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend remove <pseudo>" + ChatColor.GRAY + " - Supprimer un ami");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend list" + ChatColor.GRAY + " - Voir vos amis");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend accept <pseudo>" + ChatColor.GRAY + " - Accepter une demande");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend deny <pseudo>" + ChatColor.GRAY + " - Refuser une demande");
        player.sendMessage(ChatColor.DARK_GRAY + " ► " + ChatColor.YELLOW + "/friend notifications" + ChatColor.GRAY + " - Activer/Désactiver les notifications");
        player.sendMessage(ChatColor.AQUA + "============================");
    }
}
