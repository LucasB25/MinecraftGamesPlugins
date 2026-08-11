package fr.corehost.dac;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostManager;
import fr.corehost.game.CoreHostGame;

@SuppressWarnings("deprecation")
public class DacGameInstance {

    public enum GameState {
        WAITING, STARTING, PLAYING, ENDED
    }

    private final CoreHostDac plugin;
    private final String hostId;
    private final World world;
    private final DacMapConfig mapConfig;
    private final DacScoreboardManager scoreboardManager;
    
    private GameState state = GameState.WAITING;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> alivePlayers = new ArrayList<>();
    
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Material> playerColors = new HashMap<>();
    private final Map<UUID, String> playerChatColors = new HashMap<>();
    private final Map<UUID, Integer> earnedCoins = new HashMap<>();
    private boolean hardMode = false;
    
    private int currentTurnIndex = -1;
    private int turnTimeRemaining = 15;
    private org.bukkit.scheduler.BukkitTask turnTimerTask;
    private org.bukkit.scheduler.BukkitTask landingCheckTask;
    
    private final int maxPlayers;
    private final int coinsPerThimble;
    private final int coinsPerJump;
    private final int matchWinBonus;
    private final int matchLoseBonus;
    
    private final Material[] AVAILABLE_COLORS = {
        Material.RED_WOOL, Material.WHITE_WOOL, Material.LIME_WOOL, Material.YELLOW_WOOL,
        Material.ORANGE_WOOL, Material.PURPLE_WOOL, Material.PINK_WOOL, Material.CYAN_WOOL
    };
    
    private final String[] AVAILABLE_CHAT_COLORS = {
        "&c", "&f", "&a", "&e", "&6", "&5", "&d", "&b"
    };

    public DacGameInstance(CoreHostDac plugin, String hostId, World world, DacMapConfig mapConfig) {
        this.plugin = plugin;
        this.hostId = hostId;
        this.world = world;
        this.mapConfig = mapConfig;
        this.scoreboardManager = new DacScoreboardManager(this);
        
        if (this.world != null) {
            this.world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            this.world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            this.world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            this.world.setGameRule(org.bukkit.GameRule.DO_ENTITY_DROPS, false);
            this.world.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, false);
            this.world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            this.world.setGameRule(org.bukkit.GameRule.LOCATOR_BAR, false);
            this.world.setGameRule(org.bukkit.GameRule.SHOW_DEATH_MESSAGES, false);
            this.world.setTime(6000);
            this.world.setStorm(false);
            this.world.setThundering(false);
            this.world.setAutoSave(false);
        }

        this.maxPlayers = plugin.getConfig().getInt("gameplay.max-players", 8);
        this.coinsPerThimble = plugin.getConfig().getInt("rewards.coins-per-thimble", 5);
        this.coinsPerJump = plugin.getConfig().getInt("rewards.coins-per-jump", 1);
        this.matchWinBonus = plugin.getConfig().getInt("rewards.match-win-bonus", 20);
        this.matchLoseBonus = plugin.getConfig().getInt("rewards.match-lose-bonus", 5);
        
        resetPool();
        syncHostData();
    }

    private void syncHostData() {
        CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
        if (coreGame == null || coreGame.getRedisManager() == null) return;
        
        HostManager hostManager = new HostManager(coreGame.getRedisManager());
        HostData data = null;
        try {
            data = hostManager.getHost(UUID.fromString(hostId));
        } catch (Exception e) {
            data = hostManager.getAllHosts().stream()
                    .filter(h -> h.getWorldName().equalsIgnoreCase(hostId))
                    .findFirst().orElse(null);
        }
        
        if (data != null) {
            data.setCurrentPlayers(players.size());
            data.setMaxPlayers(maxPlayers);
            
            if (state == GameState.WAITING) data.setStatus(fr.corehost.api.host.HostStatus.WAITING);
            else if (state == GameState.STARTING) data.setStatus(fr.corehost.api.host.HostStatus.STARTING);
            else if (state == GameState.PLAYING) data.setStatus(fr.corehost.api.host.HostStatus.PLAYING);
            else if (state == GameState.ENDED) data.setStatus(fr.corehost.api.host.HostStatus.FINISHED);
            
            hostManager.saveHost(data);
        }
    }

    private void deleteHostData() {
        CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
        if (coreGame == null || coreGame.getRedisManager() == null) return;
        
        HostManager hostManager = new HostManager(coreGame.getRedisManager());
        HostData data = null;
        try {
            data = hostManager.getHost(UUID.fromString(hostId));
        } catch (Exception e) {
            data = hostManager.getAllHosts().stream()
                    .filter(h -> h.getWorldName().equalsIgnoreCase(hostId))
                    .findFirst().orElse(null);
        }
        
        if (data != null) {
            hostManager.deleteHost(data.getHostId());
        }
    }

    public void addPlayer(Player player) {
        if (mapConfig == null || !mapConfig.isSetup()) {
            player.sendMessage(DAC_PREFIX + ChatColor.RED + "Erreur : la carte DAC n'est pas configurée correctement (Plongeoir ou Piscine manquants).");
            return;
        }

        if (state == GameState.WAITING || state == GameState.STARTING) {
            if (players.size() >= maxPlayers && !players.contains(player.getUniqueId())) {
                player.kickPlayer("La partie est déjà pleine !");
                return;
            }
        } else {
            // Spectator join
            CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
            if (coreGame != null && coreGame.getSpectatorManager() != null) {
                coreGame.getSpectatorManager().setSpectator(player, true);
                Location specSpawn = mapConfig.getSpectatorSpawn();
                if (specSpawn != null) {
                    specSpawn = specSpawn.clone();
                    specSpawn.setWorld(world);
                }
                player.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
                player.sendMessage(DAC_PREFIX + ChatColor.YELLOW + "La partie est en cours. Vous êtes spectateur.");
                if (scoreboardManager != null) scoreboardManager.setupScoreboard(player);
            } else {
                player.kickPlayer("La partie a déjà commencé.");
            }
            return;
        }

        if (!players.contains(player.getUniqueId())) {
            players.add(player.getUniqueId());
            alivePlayers.add(player.getUniqueId());
            lives.put(player.getUniqueId(), 1); // 1 life to start
            
            int colorIndex = (players.size() - 1) % AVAILABLE_COLORS.length;
            playerColors.put(player.getUniqueId(), AVAILABLE_COLORS[colorIndex]);
            playerChatColors.put(player.getUniqueId(), AVAILABLE_CHAT_COLORS[colorIndex]);
            
            resetPlayerState(player, true);
            scoreboardManager.setupScoreboard(player);
            plugin.getGameManager().registerPlayer(player.getUniqueId(), this);

            broadcast(ChatColor.YELLOW + player.getName() + " a rejoint la partie (" + players.size() + "/" + maxPlayers + ")");

            syncHostData();
            checkStart();
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        scoreboardManager.removeScoreboard(player);
        plugin.getGameManager().unregisterPlayer(player.getUniqueId());
        
        syncHostData();
        
        if (state == GameState.PLAYING && alivePlayers.contains(player.getUniqueId())) {
            eliminatePlayer(player, true);
        } else {
            alivePlayers.remove(player.getUniqueId());
            broadcast(ChatColor.YELLOW + player.getName() + " a quitté la partie.");
            scoreboardManager.updateAll();
        }

        if (players.isEmpty()) {
            deleteHostData();
            plugin.getGameManager().cleanupInstance(hostId);
        }
    }

    private void checkStart() {
        if (state == GameState.WAITING && players.size() >= 2) {
            state = GameState.STARTING;
            scoreboardManager.updateAll();
            syncHostData();
            
            int countdown = plugin.getConfig().getInt("gameplay.countdown-seconds", 10);
            
            new BukkitRunnable() {
                int totalWait = countdown;

                @Override
                public void run() {
                    if (players.size() < 2) {
                        state = GameState.WAITING;
                        scoreboardManager.updateAll();
                        broadcast(ChatColor.RED + "Pas assez de joueurs, annulation du démarrage.");
                        cancel();
                        return;
                    }

                    if (totalWait % 5 == 0 || totalWait <= 5) {
                        if (totalWait > 0) {
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
                    }

                    if (totalWait <= 0) {
                        startGame();
                        cancel();
                        return;
                    }

                    totalWait--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }
    
    private void startGame() {
        state = GameState.PLAYING;
        syncHostData();
        
        // TP everyone to spectator
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                Location specSpawn = mapConfig.getSpectatorSpawn();
                if (specSpawn != null) {
                    specSpawn = specSpawn.clone();
                    specSpawn.setWorld(world);
                }
                p.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
                p.sendTitle(ChatColor.GREEN + "C'EST PARTI !", "", 5, 20, 5);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
        }

        broadcast(ChatColor.GREEN + "La partie commence ! Ne touchez pas les bords de la piscine !");
        
        // Start first turn
        currentTurnIndex = -1;
        nextTurn();
    }

    public void nextTurn() {
        if (state != GameState.PLAYING) return;
        
        if (checkWin()) return;

        if (turnTimerTask != null) turnTimerTask.cancel();
        
        // Find next alive player
        int startIndex = currentTurnIndex;
        do {
            currentTurnIndex = (currentTurnIndex + 1) % players.size();
        } while (!alivePlayers.contains(players.get(currentTurnIndex)) && currentTurnIndex != startIndex);
        
        Player current = Bukkit.getPlayer(players.get(currentTurnIndex));
        if (current == null) {
            eliminatePlayer(null, false); // Edge case if player disconnected exactly here
            return;
        }

        turnTimeRemaining = plugin.getConfig().getInt("gameplay.turn-time", 15);
        scoreboardManager.updateAll();
        
        String colorCode = ChatColor.translateAlternateColorCodes('&', playerChatColors.get(current.getUniqueId()));
        broadcast(ChatColor.YELLOW + "C'est au tour de " + colorCode + current.getName() + ChatColor.YELLOW + " !");
        
        Location diveSpawn = mapConfig.getDivingBoardSpawn().clone();
        diveSpawn.setWorld(world);
        current.teleport(diveSpawn);
        current.sendTitle(colorCode + "À TOI DE SAUTER !", ChatColor.GRAY + "Tu as " + turnTimeRemaining + "s", 5, 30, 5);
        current.playSound(current.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        
        if (hardMode) {
            spawnRandomWool();
        }
        
        // Start turn timer
        turnTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                if (turnTimeRemaining <= 0) {
                    broadcast(colorCode + current.getName() + ChatColor.RED + " a mis trop de temps à sauter !");
                    eliminatePlayer(current, false);
                    cancel();
                    return;
                }
                
                if (turnTimeRemaining <= 3) {
                    current.playSound(current.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                }
                
                turnTimeRemaining--;
                scoreboardManager.updateAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Continuous landing check task to instantly detect landing even without PlayerMoveEvent
        final UUID turnPlayerId = current.getUniqueId();
        if (landingCheckTask != null) landingCheckTask.cancel();
        landingCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING || currentTurnIndex < 0 || !players.get(currentTurnIndex).equals(turnPlayerId)) {
                    cancel();
                    return;
                }
                Player p = Bukkit.getPlayer(turnPlayerId);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }
                
                org.bukkit.block.Block feet = p.getLocation().getBlock();
                org.bukkit.block.Block below = p.getLocation().subtract(0, 0.1, 0).getBlock();
                boolean nearPool = p.getLocation().getY() <= mapConfig.getPoolYLevel() + 3.0;
                
                if (feet.getType() == Material.WATER || (nearPool && (feet.getType().name().endsWith("WOOL") || feet.getType().name().endsWith("CONCRETE")))) {
                    handleLanding(p, feet);
                } else if (below.getType() == Material.WATER || (nearPool && (below.getType().name().endsWith("WOOL") || below.getType().name().endsWith("CONCRETE")))) {
                    handleLanding(p, below);
                } else if (p.getLocation().getY() < mapConfig.getPoolYLevel() - 2) {
                    handleLanding(p, feet);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    public void handleLanding(Player player, Block block) {
        if (state != GameState.PLAYING) return;
        if (currentTurnIndex < 0 || !players.get(currentTurnIndex).equals(player.getUniqueId())) return;
        
        if (turnTimerTask != null) turnTimerTask.cancel();
        if (landingCheckTask != null) landingCheckTask.cancel();
        
        // Check if inside pool bounds
        BoundingBox pool = mapConfig.getPoolBounds();
        boolean insidePool = false;
        
        if (pool != null) {
            // Check X/Z bounds. Y can be checked against pool block
            if (block.getX() >= pool.getMinX() && block.getX() <= pool.getMaxX() &&
                block.getZ() >= pool.getMinZ() && block.getZ() <= pool.getMaxZ()) {
                insidePool = true;
            }
        }
        
        String colorCode = ChatColor.translateAlternateColorCodes('&', playerChatColors.get(player.getUniqueId()));
        
        if (!insidePool || block.getType() != Material.WATER) {
            // Landed outside pool or on a solid block (wool)
            broadcast(colorCode + player.getName() + ChatColor.RED + " s'est écrasé !");
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            // Spawn some particles
            player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, player.getLocation(), 10);
            eliminatePlayer(player, false);
            return;
        }
        
        // Valid jump in water
        Material playerWool = playerColors.get(player.getUniqueId());
        block.setType(playerWool);
        
        Location specSpawn = mapConfig.getSpectatorSpawn();
        if (specSpawn != null) {
            specSpawn = specSpawn.clone();
            specSpawn.setWorld(world);
        }
        player.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
        
        // Reward jump
        int currentEarned = earnedCoins.getOrDefault(player.getUniqueId(), 0);
        earnedCoins.put(player.getUniqueId(), currentEarned + coinsPerJump);
        
        // Check for thimble (Dé à coudre)
        if (isThimble(block)) {
            int currentLives = lives.getOrDefault(player.getUniqueId(), 0);
            lives.put(player.getUniqueId(), currentLives + 1);
            
            broadcast(ChatColor.GOLD + "⭐ " + colorCode + ChatColor.BOLD + "DÉ À COUDRE " + ChatColor.GOLD + "pour " + colorCode + player.getName() + ChatColor.GOLD + " ! (+1 Vie) ⭐");
            player.sendTitle(ChatColor.GOLD + "DÉ À COUDRE !", ChatColor.YELLOW + "+1 Vie", 5, 40, 10);
            for (Player p : world.getPlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            
            earnedCoins.put(player.getUniqueId(), earnedCoins.getOrDefault(player.getUniqueId(), 0) + coinsPerThimble);
        } else {
            broadcast(colorCode + player.getName() + ChatColor.GREEN + " a réussi son saut !");
        }
        
        if (getRemainingWaterBlocks() == 0) {
            if (!hardMode) {
                hardMode = true;
                resetPool();
                broadcast(ChatColor.LIGHT_PURPLE + "La piscine est pleine ! Activation du Mode Difficile !");
                for (Player p : world.getPlayers()) {
                    p.sendTitle(ChatColor.LIGHT_PURPLE + "MODE DIFFICILE", ChatColor.YELLOW + "La piscine va se remplir !", 10, 40, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                }
            } else {
                resetPool();
                broadcast(ChatColor.AQUA + "La piscine est pleine ! Elle a été vidée.");
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
                }
            }
        }
        
        scoreboardManager.updateAll();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                nextTurn();
            }
        }.runTaskLater(plugin, 20L); // 1 second delay before next turn
    }
    
    private boolean isThimble(Block block) {
        BoundingBox pool = mapConfig.getPoolBounds();
        if (pool == null) return false;
        
        Block[] adj = {
            block.getRelative(1, 0, 0),
            block.getRelative(-1, 0, 0),
            block.getRelative(0, 0, 1),
            block.getRelative(0, 0, -1)
        };
        
        for (Block b : adj) {
            boolean isWater = b.getType() == Material.WATER;
            boolean inBounds = (b.getX() >= pool.getMinX() && b.getX() <= pool.getMaxX() &&
                                b.getZ() >= pool.getMinZ() && b.getZ() <= pool.getMaxZ());
                                
            // If the block is water AND inside the pool, it's not a thimble.
            if (isWater && inBounds) {
                return false;
            }
        }
        
        return true;
    }

    private void eliminatePlayer(Player player, boolean isDisconnect) {
        if (turnTimerTask != null) turnTimerTask.cancel();
        if (landingCheckTask != null) landingCheckTask.cancel();
        
        if (player != null && alivePlayers.contains(player.getUniqueId())) {
            int currentLives = lives.getOrDefault(player.getUniqueId(), 1);
            currentLives--;
            lives.put(player.getUniqueId(), currentLives);
            
            String colorCode = ChatColor.translateAlternateColorCodes('&', playerChatColors.get(player.getUniqueId()));
            
            if (currentLives > 0 && !isDisconnect) {
                broadcast(colorCode + player.getName() + ChatColor.YELLOW + " a perdu une vie ! Il lui en reste " + currentLives + ".");
                player.sendTitle(ChatColor.RED + "OUCH !", ChatColor.YELLOW + "-1 Vie", 5, 30, 5);
                Location specSpawn = mapConfig.getSpectatorSpawn();
                if (specSpawn != null) {
                    specSpawn = specSpawn.clone();
                    specSpawn.setWorld(world);
                }
                player.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
                
                // Retry same turn
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Restart turn for this player
                        currentTurnIndex--; // Go back one index so nextTurn picks this player again
                        nextTurn();
                    }
                }.runTaskLater(plugin, 20L);
                return;
            }
            
            // Out of lives
            alivePlayers.remove(player.getUniqueId());
            if (!isDisconnect) {
                player.sendTitle(ChatColor.RED + "ÉLIMINÉ", "", 5, 40, 10);
                Location specSpawn = mapConfig.getSpectatorSpawn();
                if (specSpawn != null) {
                    specSpawn = specSpawn.clone();
                    specSpawn.setWorld(world);
                }
                player.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
                
                CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
                if (coreGame != null && coreGame.getSpectatorManager() != null) {
                    coreGame.getSpectatorManager().setSpectator(player, true);
                } else {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
            broadcast(colorCode + player.getName() + ChatColor.RED + " est éliminé !");
        } else if (player == null) {
            // Disconnect during their turn
            UUID currentId = players.get(currentTurnIndex);
            alivePlayers.remove(currentId);
        }
        
        scoreboardManager.updateAll();
        
        if (!checkWin()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    nextTurn();
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private boolean checkWin() {
        if (alivePlayers.size() <= 1) {
            state = GameState.ENDED;
            if (turnTimerTask != null) turnTimerTask.cancel();
            scoreboardManager.updateAll();
            syncHostData();
            
            Player winner = alivePlayers.size() == 1 ? Bukkit.getPlayer(alivePlayers.get(0)) : null;
            
            if (winner != null) {
                String colorCode = ChatColor.translateAlternateColorCodes('&', playerChatColors.get(winner.getUniqueId()));
                broadcast(ChatColor.GOLD + winner.getName() + " a gagné la partie !");
                
                int totalCoins = matchWinBonus + earnedCoins.getOrDefault(winner.getUniqueId(), 0);
                winner.sendTitle(ChatColor.GOLD + "VICTOIRE", ChatColor.YELLOW + "Bien joué ! (+" + totalCoins + " coins)", 10, 60, 20);
                winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                giveCoins(winner, totalCoins);
            } else {
                broadcast(ChatColor.YELLOW + "Match terminé (aucun vainqueur).");
            }
            
            // Rewards for losers
            for (UUID uuid : players) {
                if (winner == null || !winner.getUniqueId().equals(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        int totalCoins = matchLoseBonus + earnedCoins.getOrDefault(uuid, 0);
                        giveCoins(p, totalCoins);
                    }
                }
            }
            
            broadcast(ChatColor.GRAY + "Retour au lobby dans 10 secondes...");
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : world.getPlayers()) {
                        sendPlayerToLobby(p);
                    }
                    deleteHostData();
                    plugin.getGameManager().cleanupInstance(hostId);
                }
            }.runTaskLater(plugin, 200L); // 10 seconds
            
            return true;
        }
        return false;
    }
    
    private void giveCoins(Player player, int amount) {
        if (amount <= 0) return;
        player.sendMessage(DAC_PREFIX + ChatColor.GOLD + "Récompense : " + ChatColor.YELLOW + "+" + amount + " Coins");
        
        CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(CoreHostGame.class);
        if (coreGame != null && coreGame.getRedisManager() != null) {
            coreGame.getRedisManager().publish("corehost:proxy:events", "{\"action\":\"ADD_COINS\", \"uuid\":\"" + player.getUniqueId().toString() + "\", \"amount\": " + amount + "}");
        }
    }

    private void resetPlayerState(Player player, boolean tpToSpawn) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        if (state == GameState.WAITING || state == GameState.STARTING) {
            org.bukkit.inventory.ItemStack bed = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_BED);
            org.bukkit.inventory.meta.ItemMeta meta = bed.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Retour au Lobby");
                meta.setLore(java.util.Arrays.asList(
                    "",
                    ChatColor.GRAY + "Quitter la partie et",
                    ChatColor.GRAY + "retourner au hub principal.",
                    "",
                    ChatColor.YELLOW + "► Clic droit pour quitter"
                ));
                bed.setItemMeta(meta);
            }
            player.getInventory().setItem(8, bed);
        }
        
        if (tpToSpawn) {
            Location specSpawn = mapConfig.getSpectatorSpawn();
            if (specSpawn != null) {
                specSpawn = specSpawn.clone();
                specSpawn.setWorld(world);
            }
            player.teleport(specSpawn != null ? specSpawn : world.getSpawnLocation());
        }
    }

    public static final String DAC_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "DAC" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private void sendPlayerToLobby(Player player) {
        if (player == null || !player.isOnline()) return;

        try {
            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("lobby");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getWorld().equals(world)) {
                        resetPlayerState(player, false);
                        if (Bukkit.getWorlds().size() > 0) {
                            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
            
        } catch (Exception e) {
            resetPlayerState(player, false);
            if (Bukkit.getWorlds().size() > 0) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }
    }

    private void broadcast(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(DAC_PREFIX + message);
        }
    }

    public GameState getState() { return state; }
    public World getWorld() { return world; }
    public DacMapConfig getMapConfig() { return mapConfig; }
    public List<UUID> getPlayers() { return players; }
    public List<UUID> getAlivePlayers() { return alivePlayers; }
    public int getLives(UUID uuid) { return lives.getOrDefault(uuid, 0); }
    public CoreHostDac getPlugin() { return plugin; }
    public int getTurnTimeRemaining() { return turnTimeRemaining; }
    
    public Player getCurrentPlayer() {
        if (currentTurnIndex >= 0 && currentTurnIndex < players.size()) {
            return Bukkit.getPlayer(players.get(currentTurnIndex));
        }
        return null;
    }
    
    public String getPlayerColorChat(UUID uuid) {
        return ChatColor.translateAlternateColorCodes('&', playerChatColors.getOrDefault(uuid, "&7"));
    }

    private int getRemainingWaterBlocks() {
        if (mapConfig == null || world == null) return 0;
        BoundingBox pool = mapConfig.getPoolBounds();
        if (pool == null) return 0;
        
        int count = 0;
        for (int x = (int) pool.getMinX(); x <= pool.getMaxX(); x++) {
            for (int y = (int) pool.getMinY(); y <= pool.getMaxY(); y++) {
                for (int z = (int) pool.getMinZ(); z <= pool.getMaxZ(); z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.WATER) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void spawnRandomWool() {
        if (mapConfig == null || world == null) return;
        BoundingBox pool = mapConfig.getPoolBounds();
        if (pool == null) return;
        
        List<Block> waterBlocks = new ArrayList<>();
        for (int x = (int) pool.getMinX(); x <= pool.getMaxX(); x++) {
            for (int y = (int) pool.getMinY(); y <= pool.getMaxY(); y++) {
                for (int z = (int) pool.getMinZ(); z <= pool.getMaxZ(); z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() == Material.WATER) {
                        waterBlocks.add(b);
                    }
                }
            }
        }
        
        if (!waterBlocks.isEmpty()) {
            java.util.Random rand = new java.util.Random();
            int blocksToSpawn = Math.min(waterBlocks.size(), rand.nextInt(8) + 3); // 3 to 10 blocks (3+0 to 3+7)
            java.util.Collections.shuffle(waterBlocks);
            
            for (int i = 0; i < blocksToSpawn; i++) {
                Block b = waterBlocks.get(i);
                Material randomColor = AVAILABLE_COLORS[rand.nextInt(AVAILABLE_COLORS.length)];
                b.setType(randomColor);
            }
            
            for (Player p : world.getPlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
            }
            
            if (getRemainingWaterBlocks() == 0) {
                resetPool();
                broadcast(ChatColor.AQUA + "La piscine est pleine ! Elle a été vidée.");
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
                }
            }
        }
    }

    private void resetPool() {
        if (mapConfig == null || world == null) return;
        BoundingBox pool = mapConfig.getPoolBounds();
        if (pool != null) {
            for (int x = (int) pool.getMinX(); x <= pool.getMaxX(); x++) {
                for (int y = (int) pool.getMinY(); y <= pool.getMaxY(); y++) {
                    for (int z = (int) pool.getMinZ(); z <= pool.getMaxZ(); z++) {
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType().name().endsWith("WOOL")) {
                            b.setType(Material.WATER);
                        }
                    }
                }
            }
        }
    }
}
