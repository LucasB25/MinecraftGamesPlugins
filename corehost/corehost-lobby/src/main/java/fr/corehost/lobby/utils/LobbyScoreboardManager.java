package fr.corehost.lobby.utils;

import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
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
import java.util.concurrent.ConcurrentHashMap;

public class LobbyScoreboardManager implements PluginMessageListener {

    private final CoreHostLobby plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Double> easyTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Double> hardTimes = new ConcurrentHashMap<>();

    private int globalPlayerCount = 0;

    public LobbyScoreboardManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        
        if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
            plugin.getRedisManager().subscribe(new redis.clients.jedis.JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    if (channel.equals("corehost:profile:update")) {
                        try {
                            UUID uuid = UUID.fromString(message);
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    updateCoins(player);
                                }, 10L); // slight delay to ensure cache is cleared by API
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }, "corehost:profile:update");
        }

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (p != null) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("PlayerCount");
                out.writeUTF("ALL");
                p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
        }, 20L, 40L);

        // Fetch Parkour Times periodically (every 10 seconds)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                try (redis.clients.jedis.Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                    for (UUID uuid : scoreboards.keySet()) {
                        Double easy = jedis.zscore("corehost:parkour:easy", uuid.toString());
                        Double hard = jedis.zscore("corehost:parkour:hard", uuid.toString());
                        if (easy != null) easyTimes.put(uuid, easy);
                        if (hard != null) hardTimes.put(uuid, hard);
                    }
                } catch (Exception ignored) {}
            }
        }, 20L, 200L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("PlayerCount")) {
            String server = in.readUTF();
            if (server.equals("ALL")) {
                int newCount = in.readInt();
                if (newCount != this.globalPlayerCount) {
                    this.globalPlayerCount = newCount;
                    updateAllPlayerCounts();
                }
            }
        }
    }

    public void updateAllPlayerCounts() {
        for (UUID uuid : scoreboards.keySet()) {
            Scoreboard board = scoreboards.get(uuid);
            Team playersTeam = board.getTeam("players_count");
            if (playersTeam != null) {
                int vanishedLocal = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasMetadata("vanished")) {
                        vanishedLocal++;
                    }
                }
                int displayCount = Math.max(globalPlayerCount, Bukkit.getOnlinePlayers().size());
                displayCount = Math.max(0, displayCount - vanishedLocal);
                playersTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Joueurs: " + CC.GREEN + displayCount);
            }
        }
    }

    public void setupScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        String title = CC.translate( plugin.getConfig().getString("scoreboard.title", "&6&lCOREHOST"));
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("lobbyboard", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ligne 15 : Séparateur haut
        objective.getScore(CC.DARK_GRAY + "" + CC.STRIKETHROUGH + "                    ").setScore(15);

        // Ligne 14 : Section Profil
        objective.getScore(" " + CC.GOLD + "✦ Profil").setScore(14);

        // Ligne 13 : Grade
        Team gradeTeam = board.registerNewTeam("grade");
        gradeTeam.addEntry(CC.LIGHT_PURPLE + "");
        gradeTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Grade: " + LuckPermsHook.getPlayerPrefix(player));
        objective.getScore(CC.LIGHT_PURPLE + "").setScore(13);

        // Ligne 12 : Pseudo
        Team pseudoTeam = board.registerNewTeam("pseudo");
        pseudoTeam.addEntry(CC.AQUA + "");
        pseudoTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Pseudo: " + CC.GREEN + player.getName());
        objective.getScore(CC.AQUA + "").setScore(12);

        // Ligne 11 : Coins
        Team coinsTeam = board.registerNewTeam("coins");
        coinsTeam.addEntry(CC.YELLOW + "");
        coinsTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Coins: " + CC.GOLD + "0 ⛃");
        objective.getScore(CC.YELLOW + "").setScore(11);

        // Ligne 10 : Espace
        objective.getScore(" ").setScore(10);

        // Ligne 9 : Section Parkour
        objective.getScore(" " + CC.GOLD + "✦ Parkour").setScore(9);

        // Ligne 8 : Record Easy
        Team recordEasyTeam = board.registerNewTeam("record_easy");
        recordEasyTeam.addEntry(CC.RED + "");
        recordEasyTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Easy: " + CC.RED + "Aucun");
        objective.getScore(CC.RED + "").setScore(8);

        // Ligne 7 : Record Hard
        Team recordHardTeam = board.registerNewTeam("record_hard");
        recordHardTeam.addEntry(CC.DARK_RED + "");
        recordHardTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Hard: " + CC.RED + "Aucun");
        objective.getScore(CC.DARK_RED + "").setScore(7);

        // Ligne 6 : Espace
        objective.getScore("  ").setScore(6);
        
        // Ligne 5 : Section Serveur
        objective.getScore(" " + CC.GOLD + "✦ Serveur").setScore(5);

        // Ligne 4 : Joueurs en ligne
        Team playersTeam = board.registerNewTeam("players_count");
        playersTeam.addEntry(CC.GREEN + "");
        playersTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Joueurs: " + CC.GREEN + "0");
        objective.getScore(CC.GREEN + "").setScore(4);

        // Ligne 3 : Têtes Progression
        Team headTeam = board.registerNewTeam("heads");
        headTeam.addEntry(CC.GOLD + "");
        headTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Têtes: " + CC.GOLD + "0/0");
        objective.getScore(CC.GOLD + "").setScore(3);

        // Ligne 2 : Séparateur bas (identique au haut)
        objective.getScore(CC.DARK_GRAY + "" + CC.STRIKETHROUGH + "                     ").setScore(2);

        // Ligne 1 : IP
        String ip = CC.translate( plugin.getConfig().getString("scoreboard.ip", "&eplay.corehost.fr"));
        objective.getScore(ip).setScore(1);

        // Nametags will be handled in updateScoreboard

        player.setScoreboard(board);
        scoreboards.put(player.getUniqueId(), board);
        
        updateScoreboard(player);
        
        // Fetch times immediately for new player
        if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (redis.clients.jedis.Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                    Double easy = jedis.zscore("corehost:parkour:easy", player.getUniqueId().toString());
                    Double hard = jedis.zscore("corehost:parkour:hard", player.getUniqueId().toString());
                    if (easy != null) easyTimes.put(player.getUniqueId(), easy);
                    if (hard != null) hardTimes.put(player.getUniqueId(), hard);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            updateParkourTimes(player);
                        }
                    });
                } catch (Exception ignored) {}
            });
        }
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        scoreboards.remove(player.getUniqueId());
        easyTimes.remove(player.getUniqueId());
        hardTimes.remove(player.getUniqueId());
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
            gradeTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Grade: " + LuckPermsHook.getPlayerPrefix(player));
        }

        // Mise à jour des joueurs
        Team playersTeam = board.getTeam("players_count");
        if (playersTeam != null) {
            int vanishedLocal = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasMetadata("vanished")) {
                    vanishedLocal++;
                }
            }
            int displayCount = Math.max(globalPlayerCount, Bukkit.getOnlinePlayers().size());
            displayCount = Math.max(0, displayCount - vanishedLocal);
            playersTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Joueurs: " + CC.GREEN + displayCount);
        }

        // Mise à jour des coins
        Team coinsTeam = board.getTeam("coins");
        if (coinsTeam != null) {
            int coins = 0;
            if (plugin.getProfileManager() != null) {
                PlayerProfile profile = plugin.getProfileManager().getCachedProfile(player.getUniqueId());
                if (profile != null) {
                    coins = profile.getCoins();
                }
            }
            coinsTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Coins: " + CC.GOLD + coins + " ⛃");
        }

        // Update Parkour Times from cache
        Team recordEasyTeam = board.getTeam("record_easy");
        if (recordEasyTeam != null) {
            String recordText = CC.RED + "Aucun";
            Double easy = easyTimes.get(player.getUniqueId());
            if (easy != null) {
                String formattedTime = String.format("%.2f", easy / 1000.0);
                recordText = CC.YELLOW + formattedTime + "s";
            }
            recordEasyTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Easy: " + recordText);
        }
        
        Team recordHardTeam = board.getTeam("record_hard");
        if (recordHardTeam != null) {
            String recordText = CC.RED + "Aucun";
            Double hard = hardTimes.get(player.getUniqueId());
            if (hard != null) {
                String formattedTime = String.format("%.2f", hard / 1000.0);
                recordText = CC.YELLOW + formattedTime + "s";
            }
            recordHardTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Hard: " + recordText);
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
            String color = (found >= total && total > 0) ? CC.GREEN : CC.GOLD;
            headTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Têtes: " + color + found + "/" + total);
        }

    }

    public void updateCoins(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;
        Team coinsTeam = board.getTeam("coins");
        if (coinsTeam != null) {
            int coins = 0;
            if (plugin.getProfileManager() != null) {
                PlayerProfile profile = plugin.getProfileManager().getCachedProfile(player.getUniqueId());
                if (profile != null) {
                    coins = profile.getCoins();
                }
            }
            coinsTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Coins: " + CC.GOLD + coins + " ⛃");
        }
    }

    public void updateParkourTimes(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) return;
        
        Team recordEasyTeam = board.getTeam("record_easy");
        if (recordEasyTeam != null) {
            String recordText = CC.RED + "Aucun";
            Double easy = easyTimes.get(player.getUniqueId());
            if (easy != null) {
                String formattedTime = String.format("%.2f", easy / 1000.0);
                recordText = CC.YELLOW + formattedTime + "s";
            }
            recordEasyTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Easy: " + recordText);
        }
        
        Team recordHardTeam = board.getTeam("record_hard");
        if (recordHardTeam != null) {
            String recordText = CC.RED + "Aucun";
            Double hard = hardTimes.get(player.getUniqueId());
            if (hard != null) {
                String formattedTime = String.format("%.2f", hard / 1000.0);
                recordText = CC.YELLOW + formattedTime + "s";
            }
            recordHardTeam.setPrefix(CC.DARK_GRAY + " ▪ " + CC.GRAY + "Hard: " + recordText);
        }
    }
}
