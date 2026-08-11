package fr.corehost.api.database;

import fr.corehost.api.redis.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

public class DatabaseMigration {

    public static void migrateFromRedis(RedisManager redis, DatabaseManager db) {
        try (Jedis jedis = redis.getPool().getResource()) {
            
            // On utilise un lock Redis pour s'assurer qu'un seul serveur fait la migration
            // Si la clé existe, ça veut dire que la migration a déjà été faite
            if (jedis.set("corehost:migration:sql", "true", SetParams.setParams().nx()) == null) {
                return; // Déjà migré !
            }

            System.out.println("[CoreHost] Démarrage de la migration des données Redis vers SQL...");

            try (Connection conn = db.getConnection()) {
                conn.setAutoCommit(false); // Pour accélérer les requêtes (batch)

                try {
                    // 1. Migration des pseudos (corehost:uuid_to_name:*)
                    Set<String> uuidKeys = jedis.keys("corehost:uuid_to_name:*");
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO players (uuid, name) VALUES (?, ?)")) {
                        for (String key : uuidKeys) {
                            String uuid = key.replace("corehost:uuid_to_name:", "");
                            String name = jedis.get(key);
                            if (name != null) {
                                stmt.setString(1, uuid);
                                stmt.setString(2, name);
                                stmt.addBatch();
                            }
                        }
                        stmt.executeBatch();
                    }

                    // 2. Migration des paramètres (requests_blocked)
                    Set<String> blockKeys = jedis.keys("corehost:settings:requests_blocked:*");
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE players SET requests_blocked = ? WHERE uuid = ?")) {
                        for (String key : blockKeys) {
                            String uuid = key.replace("corehost:settings:requests_blocked:", "");
                            String val = jedis.get(key);
                            if ("true".equals(val)) {
                                stmt.setBoolean(1, true);
                                stmt.setString(2, uuid);
                                stmt.addBatch();
                            }
                        }
                        stmt.executeBatch();
                    }

                    // 3. Migration des amis (corehost:friends:*)
                    Set<String> friendsKeys = jedis.keys("corehost:friends:*");
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO friends (player1_uuid, player2_uuid) VALUES (?, ?)")) {
                        for (String key : friendsKeys) {
                            String uuid1 = key.replace("corehost:friends:", "");
                            Set<String> friends = jedis.smembers(key);
                            for (String uuid2 : friends) {
                                stmt.setString(1, uuid1);
                                stmt.setString(2, uuid2);
                                stmt.addBatch();
                            }
                        }
                        stmt.executeBatch();
                    }

                    // 4. Migration des liens Discord (corehost:discord_link:player:*)
                    Set<String> discordKeys = jedis.keys("corehost:discord_link:player:*");
                    try (PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO discord_links (uuid, discord_id) VALUES (?, ?)")) {
                        for (String key : discordKeys) {
                            String uuid = key.replace("corehost:discord_link:player:", "");
                            String discordId = jedis.get(key);
                            if (discordId != null) {
                                stmt.setString(1, uuid);
                                stmt.setString(2, discordId);
                                stmt.addBatch();
                            }
                        }
                        stmt.executeBatch();
                    }

                    conn.commit();
                    System.out.println("[CoreHost] Migration Redis -> SQL terminée avec succès !");

                } catch (SQLException e) {
                    conn.rollback();
                    jedis.del("corehost:migration:sql"); // On supprime le lock pour réessayer plus tard
                    System.err.println("[CoreHost] Erreur lors de la migration Redis -> SQL !");
                    e.printStackTrace();
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                jedis.del("corehost:migration:sql");
                e.printStackTrace();
            }

        }
    }
}
