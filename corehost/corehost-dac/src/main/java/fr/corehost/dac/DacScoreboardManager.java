package fr.corehost.dac;

import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
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

public class DacScoreboardManager {

    private final DacGameInstance instance;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public DacScoreboardManager(DacGameInstance instance) {
        this.instance = instance;
    }

    public void setupScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        String title = CC.translate( instance.getPlugin().getConfig().getString("scoreboard.title", "&b&lDÉ À COUDRE"));
        Scoreboard board = manager.getNewScoreboard();
        Component componentTitle = LegacyComponentSerializer.legacySection().deserialize(title);
        Objective objective = board.registerNewObjective("dacboard", "dummy", componentTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ligne 11 : Séparateur haut
        objective.getScore(CC.DARK_GRAY + "" + CC.STRIKETHROUGH + "                    ").setScore(11);

        // Ligne 10 : Section Partie
        objective.getScore(" " + CC.GOLD + "✦ Partie").setScore(10);

        // Ligne 9 : État
        Team stateTeam = board.registerNewTeam("state");
        stateTeam.addEntry(CC.GRAY + "");
        stateTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "État: " + CC.YELLOW + "En attente"));
        objective.getScore(CC.GRAY + "").setScore(9);

        // Ligne 8 : Joueur actuel
        Team turnTeam = board.registerNewTeam("turn");
        turnTeam.addEntry(CC.DARK_GRAY + "");
        turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tour: " + CC.AQUA + "Aucun"));
        objective.getScore(CC.DARK_GRAY + "").setScore(8);

        // Ligne 7 : Temps restant
        Team timeTeam = board.registerNewTeam("time");
        timeTeam.addEntry(CC.DARK_AQUA + "");
        timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Temps: " + CC.YELLOW + "--"));
        objective.getScore(CC.DARK_AQUA + "").setScore(7);

        // Ligne 6 : Espace
        objective.getScore(" ").setScore(6);

        // Ligne 5 : Section Stats du Joueur (Lives)
        objective.getScore(" " + CC.GOLD + "✦ Vies").setScore(5);

        // Ligne 4 : Vies du joueur courant (qui regarde)
        Team livesTeam = board.registerNewTeam("lives");
        livesTeam.addEntry(CC.RED + "");
        livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tu as: " + CC.RED + "❤ 1"));
        objective.getScore(CC.RED + "").setScore(4);

        // Ligne 3 : Joueurs vivants
        Team aliveTeam = board.registerNewTeam("alive");
        aliveTeam.addEntry(CC.BLUE + "");
        aliveTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "En vie: " + CC.GREEN + "0"));
        objective.getScore(CC.BLUE + "").setScore(3);

        // Ligne 2 : Séparateur bas
        objective.getScore(CC.DARK_GRAY + "" + CC.STRIKETHROUGH + "                     ").setScore(2);

        // Ligne 1 : IP
        String ip = CC.translate( instance.getPlugin().getConfig().getString("scoreboard.ip", "&eplay.corehost.fr"));
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
            String stateStr = CC.YELLOW + "En attente";
            switch (instance.getState()) {
                case STARTING:
                    stateStr = CC.GOLD + "Démarrage";
                    break;
                case PLAYING:
                    stateStr = CC.GREEN + "En jeu";
                    break;
                case ENDED:
                    stateStr = CC.RED + "Terminé";
                    break;
                default:
                    break;
            }
            stateTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "État: " + stateStr));
        }

        // Tour actuel
        Team turnTeam = board.getTeam("turn");
        if (turnTeam != null) {
            if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                Player current = instance.getCurrentPlayer();
                String name = (current != null) ? instance.getPlayerColorChat(current.getUniqueId()) + current.getName() : CC.GRAY + "Aucun";
                turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tour: " + name));
            } else {
                turnTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tour: " + CC.AQUA + "Aucun"));
            }
        }

        // Temps
        Team timeTeam = board.getTeam("time");
        if (timeTeam != null) {
            if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                int time = instance.getTurnTimeRemaining();
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Temps: " + CC.YELLOW + time + "s"));
            } else {
                timeTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Temps: " + CC.YELLOW + "--"));
            }
        }

        // Vies du joueur
        Team livesTeam = board.getTeam("lives");
        if (livesTeam != null) {
            int lives = instance.getLives(player.getUniqueId());
            if (!instance.getPlayers().contains(player.getUniqueId())) {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tu es: " + CC.GRAY + "Spectateur"));
            } else if (lives > 0) {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tu as: " + CC.RED + "❤ " + lives));
            } else {
                livesTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Tu es: " + CC.RED + "Éliminé"));
            }
        }

        // Joueurs vivants
        Team aliveTeam = board.getTeam("alive");
        if (aliveTeam != null) {
            int alive = instance.getAlivePlayers().size();
            int total = instance.getPlayers().size();
            aliveTeam.prefix(LegacyComponentSerializer.legacySection().deserialize(CC.DARK_GRAY + " ▪ " + CC.GRAY + "En vie: " + CC.GREEN + alive + "/" + total));
        }
    }
}
