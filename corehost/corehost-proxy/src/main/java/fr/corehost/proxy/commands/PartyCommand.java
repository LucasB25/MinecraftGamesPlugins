package fr.corehost.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.corehost.proxy.CoreHostProxy;
import fr.corehost.api.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.corehost.proxy.utils.ProxyPrefix;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartyCommand implements SimpleCommand {

    private final CoreHostProxy plugin;
    private final ProxyServer server;
    private final PartyManager partyManager;

    public PartyCommand(CoreHostProxy plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("corehost.command.party");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            String current = args.length == 0 ? "" : args[0].toLowerCase();
            return Stream.of("invite", "accept", "deny", "leave", "kick", "disband", "list", "chat", "c")
                    .filter(cmd -> cmd.startsWith(current))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("accept") || sub.equals("deny") || sub.equals("kick")) {
                String current = args[1].toLowerCase();
                return server.getAllPlayers().stream()
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
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();
        UUID playerUuid = player.getUniqueId();
        UUID leaderUuid = partyManager.getPartyLeader(playerUuid);

        switch (subCommand) {
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "deny":
                handleDeny(player, args);
                break;
            case "leave":
                handleLeave(player, leaderUuid);
                break;
            case "kick":
                handleKick(player, leaderUuid, args);
                break;
            case "disband":
                handleDisband(player, leaderUuid);
                break;
            case "list":
                handleList(player, leaderUuid);
                break;
            case "chat":
            case "c":
                handleChat(player, leaderUuid, args);
                break;
            default:
                sendHelp(player);
                break;
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Usage: /party invite <joueur>", NamedTextColor.RED));
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID leaderUuid = partyManager.getPartyLeader(playerUuid);
        
        if (leaderUuid != null && !leaderUuid.equals(playerUuid)) {
            player.sendMessage(ProxyPrefix.message("Seul le chef de groupe peut inviter des joueurs.", NamedTextColor.RED));
            return;
        }
        
        String targetName = args[1];
        Optional<Player> targetOpt = server.getPlayer(targetName);
        
        if (targetOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Joueur introuvable ou hors ligne.", NamedTextColor.RED));
            return;
        }
        
        Player target = targetOpt.get();
        UUID targetUuid = target.getUniqueId();
        
        if (playerUuid.equals(targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous ne pouvez pas vous inviter vous-même.", NamedTextColor.RED));
            return;
        }
        
        if (partyManager.getPartyLeader(targetUuid) != null) {
            player.sendMessage(ProxyPrefix.message("Ce joueur est déjà dans un groupe.", NamedTextColor.RED));
            return;
        }
        
        if (partyManager.arePartyInvitesBlocked(targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Ce joueur n'accepte pas les invitations de groupe.", NamedTextColor.RED));
            return;
        }
        
        partyManager.sendInvite(playerUuid, targetUuid);
        player.sendMessage(ProxyPrefix.message("Invitation envoyée à " + target.getUsername() + ".", NamedTextColor.GREEN));
        
        target.sendMessage(ProxyPrefix.get().append(Component.text("Vous avez reçu une invitation de groupe de " + player.getUsername() + ".", NamedTextColor.YELLOW)));
        
        Component acceptButton = Component.text("[ACCEPTER]")
            .color(NamedTextColor.GREEN)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party accept " + player.getUsername()))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Accepter")));
            
        Component denyButton = Component.text(" [REFUSER]")
            .color(NamedTextColor.RED)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party deny " + player.getUsername()))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Refuser")));
            
        target.sendMessage(acceptButton.append(denyButton));
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Usage: /party accept <joueur>", NamedTextColor.RED));
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        if (partyManager.getPartyLeader(playerUuid) != null) {
            player.sendMessage(ProxyPrefix.message("Vous êtes déjà dans un groupe.", NamedTextColor.RED));
            return;
        }
        
        String senderName = args[1];
        Optional<Player> senderOpt = server.getPlayer(senderName);
        
        if (senderOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Le joueur qui vous a invité est hors ligne.", NamedTextColor.RED));
            return;
        }
        
        Player sender = senderOpt.get();
        UUID senderUuid = sender.getUniqueId();
        
        if (!partyManager.hasInvite(playerUuid, senderUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez pas d'invitation de ce joueur ou elle a expiré.", NamedTextColor.RED));
            return;
        }
        
        partyManager.removeInvite(playerUuid, senderUuid);
        
        UUID senderLeader = partyManager.getPartyLeader(senderUuid);
        if (senderLeader == null) {
            partyManager.createParty(senderUuid);
            senderLeader = senderUuid;
        }
        
        partyManager.addMember(senderLeader, playerUuid);
        
        Component joinMessage = Component.text(player.getUsername() + " a rejoint le groupe !", NamedTextColor.GREEN);
        sendMessageToParty(senderLeader, joinMessage);
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Usage: /party deny <joueur>", NamedTextColor.RED));
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        String senderName = args[1];
        Optional<Player> senderOpt = server.getPlayer(senderName);
        
        if (senderOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Le joueur est hors ligne.", NamedTextColor.RED));
            return;
        }
        
        Player sender = senderOpt.get();
        UUID senderUuid = sender.getUniqueId();
        
        if (!partyManager.hasInvite(playerUuid, senderUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous n'avez pas d'invitation de ce joueur ou elle a expiré.", NamedTextColor.RED));
            return;
        }
        
        partyManager.removeInvite(playerUuid, senderUuid);
        player.sendMessage(ProxyPrefix.message("Vous avez refusé l'invitation.", NamedTextColor.YELLOW));
        sender.sendMessage(ProxyPrefix.message(player.getUsername() + " a refusé votre invitation.", NamedTextColor.RED));
    }

    private void handleLeave(Player player, UUID leaderUuid) {
        if (leaderUuid == null) {
            player.sendMessage(ProxyPrefix.message("Vous n'êtes pas dans un groupe.", NamedTextColor.RED));
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        if (leaderUuid.equals(playerUuid)) {
            handleDisband(player, leaderUuid);
        } else {
            partyManager.removeMember(playerUuid);
            player.sendMessage(ProxyPrefix.message("Vous avez quitté le groupe.", NamedTextColor.YELLOW));
            sendMessageToParty(leaderUuid, ProxyPrefix.message(player.getUsername() + " a quitté le groupe.", NamedTextColor.YELLOW));
        }
    }

    private void handleKick(Player player, UUID leaderUuid, String[] args) {
        if (leaderUuid == null || !leaderUuid.equals(player.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous devez être le chef de groupe pour expulser un joueur.", NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Usage: /party kick <joueur>", NamedTextColor.RED));
            return;
        }
        
        String targetName = args[1];
        Optional<Player> targetOpt = server.getPlayer(targetName);
        
        if (targetOpt.isEmpty()) {
            player.sendMessage(ProxyPrefix.message("Joueur introuvable.", NamedTextColor.RED));
            return;
        }
        
        Player target = targetOpt.get();
        UUID targetUuid = target.getUniqueId();
        
        if (leaderUuid.equals(targetUuid)) {
            player.sendMessage(ProxyPrefix.message("Vous ne pouvez pas vous expulser vous-même.", NamedTextColor.RED));
            return;
        }
        
        UUID targetLeader = partyManager.getPartyLeader(targetUuid);
        if (targetLeader == null || !targetLeader.equals(leaderUuid)) {
            player.sendMessage(ProxyPrefix.message("Ce joueur n'est pas dans votre groupe.", NamedTextColor.RED));
            return;
        }
        
        partyManager.removeMember(targetUuid);
        target.sendMessage(ProxyPrefix.message("Vous avez été expulsé du groupe.", NamedTextColor.RED));
        sendMessageToParty(leaderUuid, ProxyPrefix.message(target.getUsername() + " a été expulsé du groupe.", NamedTextColor.YELLOW));
    }

    private void handleDisband(Player player, UUID leaderUuid) {
        if (leaderUuid == null || !leaderUuid.equals(player.getUniqueId())) {
            player.sendMessage(ProxyPrefix.message("Vous devez être le chef de groupe pour dissoudre le groupe.", NamedTextColor.RED));
            return;
        }
        
        sendMessageToParty(leaderUuid, ProxyPrefix.message("Le groupe a été dissous.", NamedTextColor.RED));
        partyManager.disbandParty(leaderUuid);
    }

    private void handleList(Player player, UUID leaderUuid) {
        if (leaderUuid == null) {
            player.sendMessage(ProxyPrefix.message("Vous n'êtes pas dans un groupe.", NamedTextColor.RED));
            return;
        }
        
        Set<UUID> members = partyManager.getPartyMembers(leaderUuid);
        player.sendMessage(Component.text("")
                .append(Component.text("====== ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Membres du Groupe (" + members.size() + ")").color(NamedTextColor.GOLD))
                .append(Component.text(" ======").color(NamedTextColor.DARK_GRAY)));
        for (UUID memberUuid : members) {
            Optional<Player> memberOpt = server.getPlayer(memberUuid);
            if (memberOpt.isPresent()) {
                Player member = memberOpt.get();
                Component role = memberUuid.equals(leaderUuid) 
                    ? Component.text(" [Chef]", NamedTextColor.GOLD) 
                    : Component.text(" [Membre]", NamedTextColor.GREEN);
                player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(member.getUsername()).color(NamedTextColor.WHITE))
                    .append(role));
            } else {
                player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(memberUuid.toString().substring(0, 8) + "...").color(NamedTextColor.GRAY))
                    .append(Component.text(" [Hors ligne]", NamedTextColor.RED)));
            }
        }
        player.sendMessage(Component.text("============================").color(NamedTextColor.DARK_GRAY));
    }

    private void handleChat(Player player, UUID leaderUuid, String[] args) {
        if (leaderUuid == null) {
            player.sendMessage(ProxyPrefix.message("Vous n'êtes pas dans un groupe.", NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ProxyPrefix.message("Usage: /party chat <message>", NamedTextColor.RED));
            return;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Component chatMessage = Component.text("[Party] ", NamedTextColor.BLUE)
            .append(Component.text(player.getUsername() + ": ", NamedTextColor.WHITE))
            .append(Component.text(message, NamedTextColor.GRAY));
            
        sendMessageToParty(leaderUuid, chatMessage);
    }

    private void sendMessageToParty(UUID leaderUuid, Component message) {
        Set<UUID> members = partyManager.getPartyMembers(leaderUuid);
        for (UUID memberUuid : members) {
            server.getPlayer(memberUuid).ifPresent(member -> member.sendMessage(message));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("====== ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Système de Groupe").color(NamedTextColor.GOLD))
                .append(Component.text(" ======").color(NamedTextColor.DARK_GRAY)));
        
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party invite <joueur>").color(NamedTextColor.YELLOW)).append(Component.text(" - Inviter un joueur").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party accept <joueur>").color(NamedTextColor.YELLOW)).append(Component.text(" - Accepter une invitation").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party deny <joueur>").color(NamedTextColor.YELLOW)).append(Component.text(" - Refuser une invitation").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party leave").color(NamedTextColor.YELLOW)).append(Component.text(" - Quitter le groupe").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party kick <joueur>").color(NamedTextColor.YELLOW)).append(Component.text(" - Expulser un joueur").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party disband").color(NamedTextColor.YELLOW)).append(Component.text(" - Dissoudre le groupe").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party list").color(NamedTextColor.YELLOW)).append(Component.text(" - Voir les membres").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ► ").color(NamedTextColor.DARK_GRAY).append(Component.text("/party chat <message>").color(NamedTextColor.YELLOW)).append(Component.text(" - Parler au groupe").color(NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text("============================").color(NamedTextColor.DARK_GRAY));
    }
}
