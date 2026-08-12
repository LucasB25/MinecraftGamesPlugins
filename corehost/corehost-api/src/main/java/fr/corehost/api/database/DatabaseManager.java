package fr.corehost.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import fr.corehost.api.database.dao.FriendDAO;
import fr.corehost.api.database.dao.ProfileDAO;
import fr.corehost.api.database.dao.StatsDAO;

public class DatabaseManager {

    private final HikariDataSource dataSource;
    private final ProfileDAO profileDAO;
    private final FriendDAO friendDAO;
    private final StatsDAO statsDAO;

    public DatabaseManager(String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(user);
        config.setPassword(password);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(15);
        config.setConnectionTimeout(3000); // 3 seconds timeout
        config.setLeakDetectionThreshold(5000); // Detect leaks taking more than 5s
        
        this.dataSource = new HikariDataSource(config);
        
        this.profileDAO = new ProfileDAO(this);
        this.friendDAO = new FriendDAO(this);
        this.statsDAO = new StatsDAO(this);
        
        createTables();
    }

    private void createTables() {
        // Init tables through DAOs
        profileDAO.createTable();
        friendDAO.createTable();
        statsDAO.createTable();
        
        // Other legacy tables
        try (Connection conn = getConnection()) {
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

    public ProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public FriendDAO getFriendDAO() {
        return friendDAO;
    }

    public StatsDAO getStatsDAO() {
        return statsDAO;
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
