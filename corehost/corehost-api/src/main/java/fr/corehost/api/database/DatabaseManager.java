package fr.corehost.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public DatabaseManager(String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(user);
        config.setPassword(password);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10);
        
        this.dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            
            // Table for name <-> UUID cache
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "last_seen BIGINT DEFAULT 0, " +
                    "requests_blocked BOOLEAN DEFAULT FALSE, " +
                    "coins INT DEFAULT 0" +
                    ");")) {
                stmt.executeUpdate();
            }
            
            // Alter table just in case the table already exists without coins column
            try (PreparedStatement stmt = conn.prepareStatement(
                    "ALTER TABLE players ADD COLUMN coins INT DEFAULT 0;")) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
                // Column probably already exists
            }
            
            // Table for friends
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS friends (" +
                    "player1_uuid VARCHAR(36) NOT NULL, " +
                    "player2_uuid VARCHAR(36) NOT NULL, " +
                    "PRIMARY KEY (player1_uuid, player2_uuid)" +
                    ");")) {
                stmt.executeUpdate();
            }

            // Table for discord links
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS discord_links (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "discord_id VARCHAR(32) NOT NULL" +
                    ");")) {
                stmt.executeUpdate();
            }

            // Table for parkour records
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS parkour_records (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "course_id VARCHAR(32) NOT NULL, " +
                    "best_time BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, course_id)" +
                    ");")) {
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
