package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FriendCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer proxy;

    public FriendCommand(CoreHostProxy plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            String current = args.length == 0 ? "" : args[0].toLowerCase();
            return Stream.of("add", "remove", "accept", "deny", "list", "notifications", "help")
                    .filter(cmd -> cmd.startsWith(current))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("accept") || sub.equals("deny")) {
                String current = args[1].toLowerCase();
                return proxy.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(name -> name.toLowerCase().startsWith(current))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("corehost.command.friend")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED));
            return;
        }
        String[] args = invocation.arguments();

        if (plugin.getFriendManager() == null) {
            player.sendMessage(ProxyPrefix.message("Le système d'amis est actuellement indisponible.", NamedTextColor.RED));
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
        
        if (sub.equals("notifications")) {
            handleNotifications(player);
            return;
        }
        
        if (sub.equals("help")) {
            sendHelp(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Veuillez spécifier un pseudo.", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getFriendManager().getUuidByName(targetName);

        if (targetUuid == null) {
            player.sendMessage(ProxyPrefix.message("Ce joueur n'a jamais été sur le serveur.", NamedTextColor.RED));
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous ne pouvez pas être ami avec vous-même.", NamedTextColor.RED));
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
            player.sendMessage(ProxyPrefix.message("Vous êtes déjà ami avec ce joueur.", NamedTextColor.RED));
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(targetUuid, player.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous avez déjà envoyé une demande à ce joueur.", NamedTextColor.RED));
            return;
        }
        if (plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ProxyPrefix.get().append(Component.text("Ce joueur vous a déjà envoyé une demande. Faites /friend accept " + targetName).color(NamedTextColor.YELLOW)));
            return;
        }

        if (plugin.getFriendManager().areFriendRequestsBlocked(targetUuid)) {
            player.sendMessage(ProxyPrefix.get().append(Component.text("Ce joueur n'accepte pas les demandes d'amis.").color(NamedTextColor.RED)));
            return;
        }

        plugin.getFriendManager().sendFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ProxyPrefix.get().append(Component.text("Demande d'ami envoyée à " + targetName + ".").color(NamedTextColor.GREEN)));

        // Notify target if online
        Optional<Player> targetPlayer = proxy.getPlayer(targetUuid);
        if (targetPlayer.isPresent()) {
            targetPlayer.get().sendMessage(ProxyPrefix.get().append(Component.text("Vous avez reçu une demande d'ami de " + player.getUsername() + ".").color(NamedTextColor.YELLOW)));
            
            Component acceptButton = Component.text("[ACCEPTER]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/friend accept " + player.getUsername()))
                .hoverEvent(HoverEvent.showText(Component.text("Accepter")));
                
            Component denyButton = Component.text(" [REFUSER]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/friend deny " + player.getUsername()))
                .hoverEvent(HoverEvent.showText(Component.text("Refuser")));
                
            targetPlayer.get().sendMessage(acceptButton.append(denyButton));
        }
    }

    private void handleAccept(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez pas de demande d'ami de ce joueur.", NamedTextColor.RED));
            return;
        }

        // Check limit for player
        Set<String> playerFriends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (playerFriends.size() >= 50) {
            player.sendMessage(ProxyPrefix.message("Vous avez atteint la limite de 50 amis.", NamedTextColor.RED));
            return;
        }

        plugin.getFriendManager().acceptFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ProxyPrefix.message("Vous êtes désormais ami avec " + targetName + ".", NamedTextColor.GREEN));

        Optional<Player> targetPlayer = proxy.getPlayer(targetUuid);
        targetPlayer.ifPresent(p -> {
            p.sendMessage(ProxyPrefix.message(player.getUsername() + " a accepté votre demande d'ami !", NamedTextColor.GREEN));
        });
    }

    private void handleDeny(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().hasFriendRequest(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez pas de demande d'ami de ce joueur.", NamedTextColor.RED));
            return;
        }
        
        plugin.getFriendManager().denyFriendRequest(player.getUniqueId(), targetUuid);
        player.sendMessage(ProxyPrefix.message("Demande d'ami refusée pour " + targetName + ".", NamedTextColor.YELLOW));
    }

    private void handleRemove(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getFriendManager().areFriends(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous n'êtes pas ami avec ce joueur.", NamedTextColor.RED));
            return;
        }
        
        plugin.getFriendManager().removeFriend(player.getUniqueId(), targetUuid);
        player.sendMessage(ProxyPrefix.message("Vous n'êtes plus ami avec " + targetName + ".", NamedTextColor.YELLOW));
    }

    private void handleNotifications(Player player) {
        boolean enabled = plugin.getFriendManager().areNotificationsEnabled(player.getUniqueId());
        plugin.getFriendManager().setNotificationsEnabled(player.getUniqueId(), !enabled);
        
        if (!enabled) {
            player.sendMessage(ProxyPrefix.message("Vous avez activé les notifications de connexion de vos amis.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(ProxyPrefix.message("Vous avez désactivé les notifications de connexion de vos amis.", NamedTextColor.YELLOW));
        }
    }

    private void handleList(Player player) {
        Set<String> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez aucun ami.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(ProxyPrefix.get().append(Component.text("Vos Amis (" + friends.size() + "/50)", NamedTextColor.GOLD)));
        for (String fUuid : friends) {
            String name = plugin.getFriendManager().getNameByUuid(UUID.fromString(fUuid));
            if (name == null) name = "Inconnu";
            
            boolean online = proxy.getPlayer(UUID.fromString(fUuid)).isPresent();
            Component status = online ? Component.text(" [En ligne]", NamedTextColor.GREEN) : Component.text(" [Hors ligne]", NamedTextColor.RED);
            
            player.sendMessage(Component.text("- " + name, NamedTextColor.GRAY).append(status));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("====== ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Système d'Amis").color(NamedTextColor.GOLD))
                .append(Component.text(" ======").color(NamedTextColor.DARK_GRAY)));
        
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend add <pseudo>").color(NamedTextColor.YELLOW)).append(Component.text(" - Ajouter un ami").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend remove <pseudo>").color(NamedTextColor.YELLOW)).append(Component.text(" - Supprimer un ami").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend list").color(NamedTextColor.YELLOW)).append(Component.text(" - Voir vos amis").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend accept <pseudo>").color(NamedTextColor.YELLOW)).append(Component.text(" - Accepter une demande").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend deny <pseudo>").color(NamedTextColor.YELLOW)).append(Component.text(" - Refuser une demande").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/friend notifications").color(NamedTextColor.YELLOW)).append(Component.text(" - Activer/Désactiver les notifications").color(NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text("============================").color(NamedTextColor.DARK_GRAY));
    }
}
