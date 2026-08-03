package fr.corehost.lobby.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.api.profile.PlayerProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.plugin.messaging.PluginMessageListener;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.collect.Iterables;

public class LobbyScoreboardManager implements PluginMessageListener {

    private final CoreHostLobby plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    private int globalPlayerCount = 0;

    public LobbyScoreboardManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (p != null) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("PlayerCount");
                out.writeUTF("ALL");
                p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
        }, 20L, 40L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("PlayerCount")) {
            String server = in.readUTF();
            if (server.equals("ALL")) {
                this.globalPlayerCount = in.readInt();
            }
        }
    }

    public void setupScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title", "&6&lCOREHOST"));
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("lobbyboard", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ligne 15 : Séparateur haut
        objective.getScore(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------------").setScore(15);

        // Ligne 14 : Profil
        objective.getScore(ChatColor.WHITE + " 👤 Profil :").setScore(14);

        // Ligne 13 : Grade
        Team gradeTeam = board.registerNewTeam("grade");
        gradeTeam.addEntry(ChatColor.LIGHT_PURPLE + "");
        gradeTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Grade: " + LuckPermsHook.getPlayerPrefix(player));
        objective.getScore(ChatColor.LIGHT_PURPLE + "").setScore(13);

        // Ligne 12 : Pseudo
        Team pseudoTeam = board.registerNewTeam("pseudo");
        pseudoTeam.addEntry(ChatColor.AQUA + "");
        pseudoTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Pseudo: " + ChatColor.GREEN + player.getName());
        objective.getScore(ChatColor.AQUA + "").setScore(12);

        // Ligne 11 : Coins
        Team coinsTeam = board.registerNewTeam("coins");
        coinsTeam.addEntry(ChatColor.YELLOW + "");
        coinsTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Coins: " + ChatColor.YELLOW + "0 ⛃");
        objective.getScore(ChatColor.YELLOW + "").setScore(11);

        // Ligne 10 : Espace
        objective.getScore(" ").setScore(10);

        // Ligne 9 : Parkour
        objective.getScore(ChatColor.WHITE + " 🏃 Parkour :").setScore(9);

        // Ligne 8 : Record Easy
        Team recordEasyTeam = board.registerNewTeam("record_easy");
        recordEasyTeam.addEntry(ChatColor.RED + "");
        recordEasyTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Easy: " + ChatColor.RED + "Aucun");
        objective.getScore(ChatColor.RED + "").setScore(8);

        // Ligne 7 : Record Hard
        Team recordHardTeam = board.registerNewTeam("record_hard");
        recordHardTeam.addEntry(ChatColor.DARK_RED + "");
        recordHardTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Hard: " + ChatColor.RED + "Aucun");
        objective.getScore(ChatColor.DARK_RED + "").setScore(7);

        // Ligne 6 : Espace
        objective.getScore("  ").setScore(6);
        
        // Ligne 5 : Serveur
        objective.getScore(ChatColor.WHITE + " 🌍 Serveur :").setScore(5);

        // Ligne 4 : Joueurs en ligne
        Team playersTeam = board.registerNewTeam("players_count");
        playersTeam.addEntry(ChatColor.GREEN + "");
        playersTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Joueurs: " + ChatColor.GREEN + "0");
        objective.getScore(ChatColor.GREEN + "").setScore(4);

        // Ligne 3 : Têtes Progression
        Team headTeam = board.registerNewTeam("heads");
        headTeam.addEntry(ChatColor.GOLD + "");
        headTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Têtes: " + ChatColor.GOLD + "0/0");
        objective.getScore(ChatColor.GOLD + "").setScore(3);

        // Ligne 2 : Séparateur bas
        objective.getScore(ChatColor.GRAY + "" + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "------------------------").setScore(2);

        // Ligne 1 : IP
        String ip = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.ip", "&eplay.corehost.fr"));
        objective.getScore(ip).setScore(1);

        // Nametags will be handled in updateScoreboard

        player.setScoreboard(board);
        scoreboards.put(player.getUniqueId(), board);
        
        updateScoreboard(player);
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

    public void updateScoreboard(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;

        // Mise à jour du grade
        Team gradeTeam = board.getTeam("grade");
        if (gradeTeam != null) {
            gradeTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Grade: " + LuckPermsHook.getPlayerPrefix(player));
        }

        // Mise à jour des joueurs
        Team playersTeam = board.getTeam("players_count");
        if (playersTeam != null) {
            // Using globalPlayerCount updated via BungeeCord plugin messaging
            // If proxy is not reachable, this will just display 0, or fallback to Bukkit size if wanted.
            // A good fallback is Math.max(globalPlayerCount, Bukkit.getOnlinePlayers().size())
            int displayCount = Math.max(globalPlayerCount, Bukkit.getOnlinePlayers().size());
            playersTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Joueurs: " + ChatColor.GREEN + displayCount);
        }

        // Mise à jour des coins
        Team coinsTeam = board.getTeam("coins");
        if (coinsTeam != null) {
            int coins = 0;
            if (plugin.getProfileManager() != null) {
                PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                if (profile != null) {
                    coins = profile.getCoins();
                }
            }
            coinsTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Coins: " + ChatColor.YELLOW + coins + " ⛃");
        }

        // Fetch Parkour Times Asynchronously
        if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Double easyTime = null;
                Double hardTime = null;
                try (redis.clients.jedis.Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                    easyTime = jedis.zscore("corehost:parkour:easy", player.getUniqueId().toString());
                    hardTime = jedis.zscore("corehost:parkour:hard", player.getUniqueId().toString());
                } catch (Exception ignored) {}
                
                final Double finalEasy = easyTime;
                final Double finalHard = hardTime;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Team recordEasyTeam = board.getTeam("record_easy");
                    if (recordEasyTeam != null) {
                        String recordText = ChatColor.RED + "Aucun";
                        if (finalEasy != null) {
                            String formattedTime = String.format("%.2f", finalEasy / 1000.0);
                            recordText = ChatColor.YELLOW + formattedTime + "s";
                        }
                        recordEasyTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Easy: " + recordText);
                    }
                    
                    Team recordHardTeam = board.getTeam("record_hard");
                    if (recordHardTeam != null) {
                        String recordText = ChatColor.RED + "Aucun";
                        if (finalHard != null) {
                            String formattedTime = String.format("%.2f", finalHard / 1000.0);
                            recordText = ChatColor.YELLOW + formattedTime + "s";
                        }
                        recordHardTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Hard: " + recordText);
                    }
                });
            });
        }

        // Mise à jour des têtes
        Team headTeam = board.getTeam("heads");
        if (headTeam != null) {
            int total = 0;
            int found = 0;
            if (plugin.getHeadHuntManager() != null) {
                total = plugin.getHeadHuntManager().getTotalHeads();
                found = plugin.getHeadHuntManager().getFoundHeads(player.getUniqueId());
            }
            ChatColor color = (found >= total && total > 0) ? ChatColor.GREEN : ChatColor.GOLD;
            headTeam.setPrefix(ChatColor.GRAY + " ▪ " + ChatColor.WHITE + "Têtes: " + color + found + "/" + total);
        }

        // Mise à jour des nametags
        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = "nt_" + target.getName();
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);
            
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
            
            String prefix = LuckPermsHook.getPlayerPrefix(target);
            if (prefix.length() > 63) prefix = prefix.substring(0, 63);
            team.setPrefix(prefix + " ");
            
            String lastColors = ChatColor.getLastColors(prefix);
            if (!lastColors.isEmpty()) {
                team.setColor(ChatColor.getByChar(lastColors.charAt(lastColors.length() - 1)));
            }
            
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }
    }
}
