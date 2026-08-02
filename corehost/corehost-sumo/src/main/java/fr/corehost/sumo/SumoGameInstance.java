package fr.corehost.sumo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.game.CoreHostGame;

public class SumoGameInstance {

    public enum GameState {
        WAITING, STARTING, PLAYING, ENDED
    }

    private final CoreHostSumo plugin;
    private final String hostId;
    private final World world;
    private final SumoMapConfig mapConfig;
    private final SumoScoreboardManager scoreboardManager;
    
    private GameState state = GameState.WAITING;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> alivePlayers = new ArrayList<>();
    
    private int targetScore = 2; // Default BO3 (2 wins)
    private final Map<UUID, Integer> wins = new HashMap<>();
    
    private int roundTime = 60;
    private org.bukkit.scheduler.BukkitTask roundTimerTask;

    public SumoGameInstance(CoreHostSumo plugin, String hostId, World world, SumoMapConfig mapConfig) {
        this.plugin = plugin;
        this.hostId = hostId;
        this.world = world;
        this.mapConfig = mapConfig;
        this.scoreboardManager = new SumoScoreboardManager(this);
        
        // Fetch HostData to get Best Of setting
        CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
        if (coreGame != null && coreGame.getRedisManager() != null) {
            HostManager hostManager = new HostManager(coreGame.getRedisManager());
            try {
                HostData data = hostManager.getHost(UUID.fromString(hostId));
                if (data != null) {
                    int bestOf = data.getBestOf();
                    this.targetScore = (int) Math.ceil(bestOf / 2.0);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid hostId format for Sumo instance: " + hostId);
            }
        }
    }

    public void addPlayer(Player player) {
        if (mapConfig == null || !mapConfig.isSetup()) {
            player.sendMessage(ChatColor.RED + "Désolé, il y a un souci dans le mode de jeu : aucune carte n'est disponible ou configurée correctement.");
            return;
        }

        if (state != GameState.WAITING && state != GameState.STARTING) {
            player.sendMessage(ChatColor.RED + "La partie a déjà commencé.");
            return;
        }

        if (!players.contains(player.getUniqueId())) {
            players.add(player.getUniqueId());
            alivePlayers.add(player.getUniqueId());
            wins.put(player.getUniqueId(), 0);
            
            resetPlayerState(player);
            scoreboardManager.setupScoreboard(player);

            broadcast(ChatColor.YELLOW + player.getName() + " a rejoint la partie (" + players.size() + "/2)");

            checkStart(true);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        scoreboardManager.removeScoreboard(player);
        
        if (state == GameState.PLAYING) {
            handleDeath(player);
        } else {
            alivePlayers.remove(player.getUniqueId());
            broadcast(ChatColor.YELLOW + player.getName() + " a quitté la partie.");
            scoreboardManager.updateAll();
        }

        // Si la partie se vide complètement (ex: pendant l'attente), on l'annule
        if (players.isEmpty()) {
            plugin.getGameManager().removeInstance(hostId);
        }
    }

    private static final String[] DEATH_MESSAGES = {
        "{loser} a glissé sur une peau de banane !",
        "{loser} a été éjecté du ring par {winner} !",
        "{winner} a envoyé {loser} dans le vide !",
        "{loser} n'a pas fait le poids face à {winner} !",
        "{loser} a pris la porte de sortie !",
        "{loser} s'est envolé... mais n'a pas atterri ! Bien joué {winner}.",
        "{winner} a montré qui est le vrai maître du Sumo à {loser} !"
    };

    public void handleDeath(Player player) {
        if (!alivePlayers.contains(player.getUniqueId())) return;
        
        alivePlayers.remove(player.getUniqueId());
        
        // Visual feedback for loser
        player.sendTitle(ChatColor.RED + "TOMBÉ !", "", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
        
        Player winner = null;
        if (alivePlayers.size() == 1) {
            winner = Bukkit.getPlayer(alivePlayers.get(0));
            if (winner != null) {
                int currentWins = wins.getOrDefault(winner.getUniqueId(), 0) + 1;
                wins.put(winner.getUniqueId(), currentWins);
                
                // Visual feedback for winner
                winner.sendTitle(ChatColor.AQUA + "MANCHE GAGNÉE", ChatColor.GRAY + "Score: " + currentWins + "/" + targetScore, 5, 40, 10);
                winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                
                // Random Death Message
                String randomMsg = DEATH_MESSAGES[new java.util.Random().nextInt(DEATH_MESSAGES.length)];
                randomMsg = randomMsg.replace("{loser}", ChatColor.RED + player.getName() + ChatColor.GRAY);
                randomMsg = randomMsg.replace("{winner}", ChatColor.AQUA + winner.getName() + ChatColor.GRAY);
                
                broadcast(randomMsg);
                broadcast(ChatColor.AQUA + winner.getName() + " a gagné cette manche ! (" + currentWins + "/" + targetScore + ")");
            }
        }
        
        scoreboardManager.updateAll();
        checkWin(winner);
    }

    private void checkStart(boolean isFirstGame) {
        if (state == GameState.WAITING && players.size() >= 2) { // 2 players to start
            state = GameState.STARTING;
            scoreboardManager.updateAll();
            
            new BukkitRunnable() {
                int totalWait = isFirstGame ? 10 : 5;

                @Override
                public void run() {
                    if (players.size() < 2) {
                        state = GameState.WAITING;
                        scoreboardManager.updateAll();
                        broadcast(ChatColor.RED + "Pas assez de joueurs, annulation du démarrage.");
                        
                        // TP remaining player back to wait area
                        for (UUID uuid : players) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) p.teleport(world.getSpawnLocation());
                        }
                        
                        cancel();
                        return;
                    }

                    if (totalWait == 10) {
                        broadcast(ChatColor.YELLOW + "La partie commencera dans 10 secondes...");
                    }

                    if (totalWait == 5) {
                        teleportToFightSpawns();
                    }

                    if (totalWait <= 5 && totalWait > 0) {
                        broadcast(ChatColor.YELLOW + "Début dans " + totalWait + " secondes...");
                        if (totalWait <= 3) {
                            for (UUID uuid : players) {
                                Player p = Bukkit.getPlayer(uuid);
                                if (p != null) {
                                    p.sendTitle(ChatColor.YELLOW + "" + totalWait, "", 2, 15, 2);
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + ((4 - totalWait) * 0.2f));
                                }
                            }
                        }
                    }

                    if (totalWait <= 0) {
                        state = GameState.PLAYING;
                        roundTime = 60;
                        scoreboardManager.updateAll();

                        for (UUID uuid : players) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                p.sendTitle(ChatColor.GREEN + "C'EST PARTI !", "", 5, 20, 5);
                                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                            }
                        }

                        broadcast(ChatColor.GREEN + "La partie commence !");
                        startRoundTimer();
                        cancel();
                        return;
                    }

                    totalWait--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    private void checkWin(Player roundWinner) {
        if (alivePlayers.size() <= 1) {
            
            if (roundTimerTask != null) {
                roundTimerTask.cancel();
                roundTimerTask = null;
            }
            
            boolean matchOver = false;
            if (roundWinner != null && wins.getOrDefault(roundWinner.getUniqueId(), 0) >= targetScore) {
                matchOver = true;
            } else if (players.size() < 2) {
                matchOver = true; // forfeit
            }
            
            if (matchOver) {
                state = GameState.ENDED;
                scoreboardManager.updateAll();
                
                if (roundWinner != null) {
                    broadcast(ChatColor.GOLD + roundWinner.getName() + " a gagné la partie !");
                    for (UUID uuid : players) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            if (p.getUniqueId().equals(roundWinner.getUniqueId())) {
                                p.sendTitle(ChatColor.GOLD + "VICTOIRE", ChatColor.YELLOW + "Bien joué !", 10, 60, 20);
                                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            } else {
                                p.sendTitle(ChatColor.RED + "DÉFAITE", ChatColor.GRAY + "Meilleure chance la prochaine fois !", 10, 60, 20);
                            }
                        }
                    }
                } else {
                    broadcast(ChatColor.YELLOW + "Égalité / Forfait !");
                }
                
                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.setGameMode(GameMode.SPECTATOR);
                }
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (UUID uuid : players) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                p.kickPlayer("Partie terminée.");
                            }
                        }
                        plugin.getGameManager().removeInstance(hostId);
                    }
                }.runTaskLater(plugin, 100L); // 5 seconds
                
            } else {
                // Next Round
                state = GameState.WAITING;
                scoreboardManager.updateAll();
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        startNextRound();
                    }
                }.runTaskLater(plugin, 60L); // 3 seconds delay before teleporting back to wait
            }
        }
    }
    
    private void startRoundTimer() {
        if (roundTimerTask != null) {
            roundTimerTask.cancel();
        }
        
        roundTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                if (roundTime <= 0) {
                    broadcast(ChatColor.YELLOW + "Temps écoulé ! Égalité pour cette manche.");
                    handleDraw();
                    cancel();
                    return;
                }
                
                if (roundTime == 10 || roundTime <= 3) {
                    broadcast(ChatColor.RED + "Fin de la manche dans " + roundTime + " seconde" + (roundTime > 1 ? "s" : "") + " !");
                    for (UUID uuid : players) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                }
                
                roundTime--;
                scoreboardManager.updateAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void handleDraw() {
        if (state != GameState.PLAYING) return;
        state = GameState.WAITING;
        scoreboardManager.updateAll();
        
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendTitle(ChatColor.YELLOW + "ÉGALITÉ", ChatColor.GRAY + "Temps écoulé", 5, 40, 10);
            }
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                startNextRound();
            }
        }.runTaskLater(plugin, 60L); // 3 seconds delay before teleporting back to wait
    }

    private void startNextRound() {
        alivePlayers.clear();
        alivePlayers.addAll(players);
        
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                resetPlayerState(p);
            }
        }
        
        scoreboardManager.updateAll();
        checkStart(false);
    }
    
    private void resetPlayerState(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        
        // TP to world spawn to wait for the countdown
        player.teleport(world.getSpawnLocation());
    }

    private void teleportToFightSpawns() {
        if (players.size() > 0) {
            Player p1 = Bukkit.getPlayer(players.get(0));
            if (p1 != null) {
                p1.teleport(mapConfig.getSpawn1() != null ? mapConfig.getSpawn1() : world.getSpawnLocation());
            }
        }
        if (players.size() > 1) {
            Player p2 = Bukkit.getPlayer(players.get(1));
            if (p2 != null) {
                p2.teleport(mapConfig.getSpawn2() != null ? mapConfig.getSpawn2() : world.getSpawnLocation());
            }
        }
    }

    private static final String SUMO_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Sumo" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private void broadcast(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(SUMO_PREFIX + message);
        }
    }

    public GameState getState() {
        return state;
    }

    public World getWorld() {
        return world;
    }

    public SumoMapConfig getMapConfig() {
        return mapConfig;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public int getWins(UUID uuid) {
        return wins.getOrDefault(uuid, 0);
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public int getRoundTime() {
        return roundTime;
    }
}
