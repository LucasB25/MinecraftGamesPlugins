package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;
import java.util.UUID;
import java.util.Optional;

public class FriendCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer proxy;

    public FriendCommand(CoreHostProxy plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        String[] args = invocation.arguments();

        if (plugin.getFriendManager() == null) {
            player.sendMessage(Component.text("Le système d'amis est actuellement indisponible.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            handleList(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Veuillez spécifier un pseudo.", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getFriendManager().getUuidByName(targetName);

        if (targetUuid == null) {
            player.sendMessage(Component.text("Ce joueur n'a jamais été sur le serveur.", NamedTextColor.RED));
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Vous ne pouvez pas être ami avec vous-même.", NamedTextColor.RED));
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
    }

    private void handleAdd(Player player, UUID targetUuid, String targetName) {
        if (plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Component.text("Vous êtes déjà ami avec ce joueur.", NamedTextColor.RED));
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(targetUuid, player.getUniqueId())) {
            player.sendMessage(Component.text("Vous avez déjà envoyé une demande à ce joueur.", NamedTextColor.RED));
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Component.text("Ce joueur vous a déjà envoyé une demande. Faites /friend accept " + targetName, NamedTextColor.YELLOW));
            return;
        }

        plugin.getFriendManager().sendFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(Component.text("Demande d'ami envoyée à " + targetName + ".", NamedTextColor.GREEN));

        // Notify target if online
        Optional<Player> targetPlayer = proxy.getPlayer(targetUuid);
        targetPlayer.ifPresent(p -> {
            p.sendMessage(Component.text("Vous avez reçu une demande d'ami de " + player.getUsername() + ".", NamedTextColor.YELLOW));
            p.sendMessage(Component.text("Faites /friend accept " + player.getUsername() + " pour accepter.", NamedTextColor.YELLOW));
        });
    }

    private void handleAccept(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Component.text("Vous n'avez pas de demande d'ami de ce joueur.", NamedTextColor.RED));
            return;
        }

        // Check limit for player
        Set<String> playerFriends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (playerFriends.size() >= 50) {
            player.sendMessage(Component.text("Vous avez atteint la limite de 50 amis.", NamedTextColor.RED));
            return;
        }

        plugin.getFriendManager().acceptFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(Component.text("Vous êtes désormais ami avec " + targetName + ".", NamedTextColor.GREEN));

        Optional<Player> targetPlayer = proxy.getPlayer(targetUuid);
        targetPlayer.ifPresent(p -> {
            p.sendMessage(Component.text(player.getUsername() + " a accepté votre demande d'ami !", NamedTextColor.GREEN));
        });
    }

    private void handleDeny(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Component.text("Vous n'avez pas de demande d'ami de ce joueur.", NamedTextColor.RED));
            return;
        }
        
        plugin.getFriendManager().denyFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(Component.text("Demande d'ami refusée pour " + targetName + ".", NamedTextColor.YELLOW));
    }

    private void handleRemove(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Component.text("Vous n'êtes pas ami avec ce joueur.", NamedTextColor.RED));
            return;
        }
        
        plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
        player.sendMessage(Component.text("Vous n'êtes plus ami avec " + targetName + ".", NamedTextColor.YELLOW));
    }

    private void handleList(Player player) {
        Set<String> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(Component.text("Vous n'avez aucun ami.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("--- Vos Amis (" + friends.size() + "/50) ---", NamedTextColor.GOLD));
        for (String fUuid : friends) {
            String name = plugin.getFriendManager().getNameByUuid(UUID.fromString(fUuid));
            if (name == null) name = "Inconnu";
            
            boolean online = proxy.getPlayer(UUID.fromString(fUuid)).isPresent();
            Component status = online ? Component.text(" [En ligne]", NamedTextColor.GREEN) : Component.text(" [Hors ligne]", NamedTextColor.RED);
            
            player.sendMessage(Component.text("- " + name, NamedTextColor.GRAY).append(status));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- Commandes d'Amis ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/friend add <pseudo> - Ajouter un ami", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend accept <pseudo> - Accepter une demande", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend deny <pseudo> - Refuser une demande", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend remove <pseudo> - Supprimer un ami", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend list - Voir vos amis", NamedTextColor.YELLOW));
    }
}
