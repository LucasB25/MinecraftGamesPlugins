package fr.corehost.api.database.dao;

import fr.corehost.api.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StatsDAO {
    private final DatabaseManager db;

    public StatsDAO(DatabaseManager db) {
        this.db = db;
    }

    public void createTable() {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS player_stats (" +
                     "uuid VARCHAR(36) NOT NULL, " +
                     "game VARCHAR(32) NOT NULL, " +
                     "stat_key VARCHAR(32) NOT NULL, " +
                     "stat_value INT DEFAULT 0, " +
                     "PRIMARY KEY (uuid, game, stat_key)" +
                     ");")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Map<String, Map<String, Integer>>> loadStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Map<String, Integer>> stats = new ConcurrentHashMap<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT game, stat_key, stat_value FROM player_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String game = rs.getString("game");
                        String statKey = rs.getString("stat_key");
                        int statValue = rs.getInt("stat_value");
                        stats.computeIfAbsent(game, k -> new ConcurrentHashMap<>()).put(statKey, statValue);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return stats;
        });
    }

    public CompletableFuture<Void> saveStat(UUID uuid, String game, String statKey, int statValue) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO player_stats (uuid, game, stat_key, stat_value) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE stat_value = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, game);
                stmt.setString(3, statKey);
                stmt.setInt(4, statValue);
                stmt.setInt(5, statValue);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addStat(UUID uuid, String game, String statKey, int amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO player_stats (uuid, game, stat_key, stat_value) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE stat_value = stat_value + ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, game);
                stmt.setString(3, statKey);
                stmt.setInt(4, amount);
                stmt.setInt(5, amount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
