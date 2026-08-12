package fr.corehost.api.database.dao;

import fr.corehost.api.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FriendDAO {
    private final DatabaseManager db;

    public FriendDAO(DatabaseManager db) {
        this.db = db;
    }

    public void createTable() {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS friends (" +
                     "player1_uuid VARCHAR(36) NOT NULL, " +
                     "player2_uuid VARCHAR(36) NOT NULL, " +
                     "PRIMARY KEY (player1_uuid, player2_uuid)" +
                     ");")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Set<String>> loadFriends(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> friends = new HashSet<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT player2_uuid FROM friends WHERE player1_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        friends.add(rs.getString("player2_uuid"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return friends;
        });
    }
}
