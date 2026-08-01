package fr.corehost.sumo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
    
    private GameState state = GameState.WAITING;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> alivePlayers = new ArrayList<>();
    
    private int targetScore = 2; // Default BO3 (2 wins)
    private final Map<UUID, Integer> wins = new HashMap<>();

    public SumoGameInstance(CoreHostSumo plugin, String hostId, World world, SumoMapConfig mapConfig) {
        this.plugin = plugin;
        this.hostId = hostId;
        this.world = world;
        this.mapConfig = mapConfig;
        
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
        if (state != GameState.WAITING && state != GameState.STARTING) {
            player.sendMessage(ChatColor.RED + "La partie a déjà commencé.");
            return;
        }

        if (!players.contains(player.getUniqueId())) {
            players.add(player.getUniqueId());
            alivePlayers.add(player.getUniqueId());
            wins.put(player.getUniqueId(), 0);
            
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);

            // Basic spawn logic
            resetPlayerState(player, players.size() == 1);

            broadcast(ChatColor.YELLOW + player.getName() + " a rejoint la partie (" + players.size() + "/2)");

            checkStart();
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        
        if (state == GameState.PLAYING) {
            handleDeath(player);
        } else {
            alivePlayers.remove(player.getUniqueId());
            broadcast(ChatColor.YELLOW + player.getName() + " a quitté la partie.");
        }
    }

    public void handleDeath(Player player) {
        if (!alivePlayers.contains(player.getUniqueId())) return;
        
        alivePlayers.remove(player.getUniqueId());
        
        Player winner = null;
        if (alivePlayers.size() == 1) {
            winner = Bukkit.getPlayer(alivePlayers.get(0));
            if (winner != null) {
                int currentWins = wins.getOrDefault(winner.getUniqueId(), 0) + 1;
                wins.put(winner.getUniqueId(), currentWins);
                broadcast(ChatColor.AQUA + winner.getName() + " a gagné cette manche ! (" + currentWins + "/" + targetScore + ")");
            }
        }
        
        checkWin(winner);
    }

    private void checkStart() {
        if (state == GameState.WAITING && players.size() >= 2) { // 2 players to start
            state = GameState.STARTING;
            
            new BukkitRunnable() {
                int countdown = plugin.getConfig().getInt("gameplay.countdown-seconds", 5);

                @Override
                public void run() {
                    if (players.size() < 2) {
                        state = GameState.WAITING;
                        broadcast(ChatColor.RED + "Pas assez de joueurs, annulation du démarrage.");
                        cancel();
                        return;
                    }

                    if (countdown <= 0) {
                        state = GameState.PLAYING;
                        broadcast(ChatColor.GREEN + "La partie commence !");
                        cancel();
                        return;
                    }

                    broadcast(ChatColor.YELLOW + "Début dans " + countdown + " secondes...");
                    countdown--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    private void checkWin(Player roundWinner) {
        if (alivePlayers.size() <= 1) {
            
            boolean matchOver = false;
            if (roundWinner != null && wins.getOrDefault(roundWinner.getUniqueId(), 0) >= targetScore) {
                matchOver = true;
            } else if (players.size() < 2) {
                matchOver = true; // forfeit
            }
            
            if (matchOver) {
                state = GameState.ENDED;
                if (roundWinner != null) {
                    broadcast(ChatColor.GOLD + roundWinner.getName() + " a gagné la partie !");
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
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        startNextRound();
                    }
                }.runTaskLater(plugin, 40L); // 2 seconds delay before next round
            }
        }
    }
    
    private void startNextRound() {
        alivePlayers.clear();
        alivePlayers.addAll(players);
        
        boolean isFirst = true;
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                resetPlayerState(p, isFirst);
                isFirst = false;
            }
        }
        
        checkStart();
    }
    
    private void resetPlayerState(Player player, boolean isFirstSpawn) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        
        if (isFirstSpawn) {
            player.teleport(mapConfig.getSpawn1() != null ? mapConfig.getSpawn1() : world.getSpawnLocation());
        } else {
            player.teleport(mapConfig.getSpawn2() != null ? mapConfig.getSpawn2() : world.getSpawnLocation());
        }
    }

    private void broadcast(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
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

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }
}
