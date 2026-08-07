package fr.corehost.sumo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SumoScoreboardManager {

    private final SumoGameInstance instance;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public SumoScoreboardManager(SumoGameInstance instance) {
        this.instance = instance;
    }

    public void setupScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        String title = ChatColor.translateAlternateColorCodes('&', instance.getPlugin().getConfig().getString("scoreboard.title", "&6&lSUMO"));
        Scoreboard board = manager.getNewScoreboard();
        Component componentTitle = LegacyComponentSerializer.legacySection().deserialize(title);
        Objective objective = board.registerNewObjective("sumoboard", "dummy", componentTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ligne 11 : Séparateur haut
        objective.getScore(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                    ").setScore(11);

        // Ligne 10 : Section Partie
        objective.getScore(" " + ChatColor.GOLD + "✦ Partie").setScore(10);

        // Ligne 9 : État (En attente, Démarrage, En jeu)
        Team stateTeam = board.registerNewTeam("state");
        stateTeam.addEntry(ChatColor.GRAY + "");
        stateTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "État: " + ChatColor.YELLOW + "En attente"));
        objective.getScore(ChatColor.GRAY + "").setScore(9);

        // Ligne 8 : Temps
        Team timeTeam = board.registerNewTeam("time");
        timeTeam.addEntry(ChatColor.DARK_GRAY + "");
        timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + "--:--"));
        objective.getScore(ChatColor.DARK_GRAY + "").setScore(8);

        // Ligne 7 : Objectif de victoire
        Team objectiveTeam = board.registerNewTeam("objective");
        objectiveTeam.addEntry(ChatColor.DARK_AQUA + "");
        objectiveTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Objectif: " + ChatColor.GREEN + instance.getTargetScore() + " victoires"));
        objective.getScore(ChatColor.DARK_AQUA + "").setScore(7);

        // Ligne 6 : Espace
        objective.getScore(" ").setScore(6);

        // Ligne 5 : Section Joueurs
        objective.getScore(" " + ChatColor.GOLD + "✦ Joueurs").setScore(5);

        // Ligne 4 : Joueur 1
        Team p1Team = board.registerNewTeam("p1");
        p1Team.addEntry(ChatColor.RED + "");
        p1Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En attente..."));
        objective.getScore(ChatColor.RED + "").setScore(4);

        // Ligne 3 : Joueur 2
        Team p2Team = board.registerNewTeam("p2");
        p2Team.addEntry(ChatColor.BLUE + "");
        p2Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En attente..."));
        objective.getScore(ChatColor.BLUE + "").setScore(3);

        // Ligne 2 : Séparateur bas (identique au haut)
        objective.getScore(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                     ").setScore(2);

        // Ligne 1 : IP
        String ip = ChatColor.translateAlternateColorCodes('&', instance.getPlugin().getConfig().getString("scoreboard.ip", "&eplay.corehost.fr"));
        objective.getScore(ip).setScore(1);

        player.setScoreboard(board);
        scoreboards.put(player.getUniqueId(), board);
        
        updateAll();
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        scoreboards.remove(player.getUniqueId());
    }

    public void updateAll() {
        for (UUID uuid : scoreboards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                updateScoreboard(player);
            }
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;

        // Mise à jour de l'état
        Team stateTeam = board.getTeam("state");
        if (stateTeam != null) {
            String stateStr = ChatColor.YELLOW + "En attente";
            switch (instance.getState()) {
                case STARTING:
                    stateStr = ChatColor.GOLD + "Démarrage";
                    break;
                case PLAYING:
                    stateStr = ChatColor.GREEN + "En jeu";
                    break;
                case ENDED:
                    stateStr = ChatColor.RED + "Terminé";
                    break;
                default:
                    break;
            }
            stateTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "État: " + stateStr));
        }

        // Mise à jour du temps
        Team timeTeam = board.getTeam("time");
        if (timeTeam != null) {
            if (instance.getState() == SumoGameInstance.GameState.PLAYING) {
                int time = instance.getRoundTime();
                String timeStr = String.format("%02d:%02d", time / 60, time % 60);
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + timeStr));
            } else {
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + "--:--"));
            }
        }

        // Mise à jour des joueurs
        Team p1Team = board.getTeam("p1");
        Team p2Team = board.getTeam("p2");

        if (p1Team != null && p2Team != null) {
            java.util.List<UUID> players = instance.getPlayers();
            if (players.size() > 0) {
                Player p1 = Bukkit.getPlayer(players.get(0));
                if (p1 != null) {
                    int p1Score = instance.getWins(p1.getUniqueId());
                    p1Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.YELLOW + p1.getName() + ": " + ChatColor.WHITE + p1Score));
                }
            } else {
                p1Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En attente..."));
            }

            if (players.size() > 1) {
                Player p2 = Bukkit.getPlayer(players.get(1));
                if (p2 != null) {
                    int p2Score = instance.getWins(p2.getUniqueId());
                    p2Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.YELLOW + p2.getName() + ": " + ChatColor.WHITE + p2Score));
                }
            } else {
                p2Team.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En attente..."));
            }
        }
    }
}
