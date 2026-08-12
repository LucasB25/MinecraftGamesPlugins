package fr.corehost.api.database.dao;

import fr.corehost.api.database.DatabaseManager;
import fr.corehost.api.profile.PlayerProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileDAO {

    private final DatabaseManager db;

    public ProfileDAO(DatabaseManager db) {
        this.db = db;
    }

    public void createTable() {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS players (" +
                     "uuid VARCHAR(36) PRIMARY KEY, " +
                     "name VARCHAR(16) NOT NULL, " +
                     "last_seen BIGINT DEFAULT 0, " +
                     "requests_blocked BOOLEAN DEFAULT FALSE, " +
                     "coins INT DEFAULT 0" +
                     ");")) {
            stmt.executeUpdate();
            
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "players", "coins")) {
                if (!rs.next()) {
                    try (PreparedStatement alter = conn.prepareStatement("ALTER TABLE players ADD COLUMN coins INT DEFAULT 0;")) {
                        alter.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT name, last_seen, requests_blocked, coins FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PlayerProfile profile = new PlayerProfile(uuid, rs.getString("name"));
                        profile.setLastSeen(rs.getLong("last_seen"));
                        profile.setRequestsBlocked(rs.getBoolean("requests_blocked"));
                        profile.setCoins(rs.getInt("coins"));
                        return profile;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO players (uuid, name, last_seen, requests_blocked, coins) " +
                         "VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE name = ?, last_seen = ?, requests_blocked = ?, coins = ?")) {
                stmt.setString(1, profile.getUuid().toString());
                stmt.setString(2, profile.getName());
                stmt.setLong(3, profile.getLastSeen());
                stmt.setBoolean(4, profile.isRequestsBlocked());
                stmt.setInt(5, profile.getCoins());
                
                stmt.setString(6, profile.getName());
                stmt.setLong(7, profile.getLastSeen());
                stmt.setBoolean(8, profile.isRequestsBlocked());
                stmt.setInt(9, profile.getCoins());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addCoins(UUID uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE players SET coins = coins + ? WHERE uuid = ?")) {
                stmt.setInt(1, amount);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
