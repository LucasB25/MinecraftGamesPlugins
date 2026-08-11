package fr.corehost.dac;

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
import java.util.List;

public class DacScoreboardManager {

    private final DacGameInstance instance;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public DacScoreboardManager(DacGameInstance instance) {
        this.instance = instance;
    }

    public void setupScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        String title = ChatColor.translateAlternateColorCodes('&', instance.getPlugin().getConfig().getString("scoreboard.title", "&b&lDÉ À COUDRE"));
        Scoreboard board = manager.getNewScoreboard();
        Component componentTitle = LegacyComponentSerializer.legacySection().deserialize(title);
        Objective objective = board.registerNewObjective("dacboard", "dummy", componentTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ligne 11 : Séparateur haut
        objective.getScore(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                    ").setScore(11);

        // Ligne 10 : Section Partie
        objective.getScore(" " + ChatColor.GOLD + "✦ Partie").setScore(10);

        // Ligne 9 : État
        Team stateTeam = board.registerNewTeam("state");
        stateTeam.addEntry(ChatColor.GRAY + "");
        stateTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "État: " + ChatColor.YELLOW + "En attente"));
        objective.getScore(ChatColor.GRAY + "").setScore(9);

        // Ligne 8 : Joueur actuel
        Team turnTeam = board.registerNewTeam("turn");
        turnTeam.addEntry(ChatColor.DARK_GRAY + "");
        turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tour: " + ChatColor.AQUA + "Aucun"));
        objective.getScore(ChatColor.DARK_GRAY + "").setScore(8);

        // Ligne 7 : Temps restant
        Team timeTeam = board.registerNewTeam("time");
        timeTeam.addEntry(ChatColor.DARK_AQUA + "");
        timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + "--"));
        objective.getScore(ChatColor.DARK_AQUA + "").setScore(7);

        // Ligne 6 : Espace
        objective.getScore(" ").setScore(6);

        // Ligne 5 : Section Stats du Joueur (Lives)
        objective.getScore(" " + ChatColor.GOLD + "✦ Vies").setScore(5);

        // Ligne 4 : Vies du joueur courant (qui regarde)
        Team livesTeam = board.registerNewTeam("lives");
        livesTeam.addEntry(ChatColor.RED + "");
        livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tu as: " + ChatColor.RED + "❤ 1"));
        objective.getScore(ChatColor.RED + "").setScore(4);

        // Ligne 3 : Joueurs vivants
        Team aliveTeam = board.registerNewTeam("alive");
        aliveTeam.addEntry(ChatColor.BLUE + "");
        aliveTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En vie: " + ChatColor.GREEN + "0"));
        objective.getScore(ChatColor.BLUE + "").setScore(3);

        // Ligne 2 : Séparateur bas
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

        // Tour actuel
        Team turnTeam = board.getTeam("turn");
        if (turnTeam != null) {
            if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                Player current = instance.getCurrentPlayer();
                String name = (current != null) ? instance.getPlayerColorChat(current.getUniqueId()) + current.getName() : ChatColor.GRAY + "Aucun";
                turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tour: " + name));
            } else {
                turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tour: " + ChatColor.AQUA + "Aucun"));
            }
        }

        // Temps
        Team timeTeam = board.getTeam("time");
        if (timeTeam != null) {
            if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                int time = instance.getTurnTimeRemaining();
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + time + "s"));
            } else {
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Temps: " + ChatColor.YELLOW + "--"));
            }
        }

        // Vies du joueur
        Team livesTeam = board.getTeam("lives");
        if (livesTeam != null) {
            int lives = instance.getLives(player.getUniqueId());
            if (!instance.getPlayers().contains(player.getUniqueId())) {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tu es: " + ChatColor.GRAY + "Spectateur"));
            } else if (lives > 0) {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tu as: " + ChatColor.RED + "❤ " + lives));
            } else {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "Tu es: " + ChatColor.RED + "Éliminé"));
            }
        }

        // Joueurs vivants
        Team aliveTeam = board.getTeam("alive");
        if (aliveTeam != null) {
            int alive = instance.getAlivePlayers().size();
            int total = instance.getPlayers().size();
            aliveTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(ChatColor.DARK_GRAY + " ▪ " + ChatColor.GRAY + "En vie: " + ChatColor.GREEN + alive + "/" + total));
        }
    }
}
