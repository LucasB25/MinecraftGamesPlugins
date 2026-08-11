package fr.corehost.proxy.discord;

import fr.corehost.proxy.CoreHostProxy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import fr.corehost.api.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

public class DiscordManager extends ListenerAdapter {

    private final CoreHostProxy plugin;
    private JDA jda;
    private final Random random = new Random();

    public DiscordManager(CoreHostProxy plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String token = plugin.getProxyConfig().getDiscordBotToken();
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warn("Discord Bot Token is empty. Discord linking system is disabled.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            jda.awaitReady();
            
            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                String configBotId = plugin.getProxyConfig().getDiscordBotId();
                String botIdToUse = (configBotId != null && !configBotId.isEmpty()) ? configBotId : jda.getSelfUser().getId();
                plugin.getRedisManager().set("corehost:discord_bot_id", botIdToUse);
                plugin.getLogger().info("Discord Bot started successfully (ID: " + botIdToUse + ").");
            } else {
                plugin.getLogger().info("Discord Bot started successfully (ID: " + jda.getSelfUser().getId() + ").");
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to start Discord Bot", e);
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    /**
     * Generates a random 6-digit code for the player and stores it in Redis.
     */
    public String generateLinkCode(UUID playerUuid) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return null;
        
        String code = String.format("%06d", random.nextInt(1000000));
        
        // Store mapping: Code -> UUID (Expires in 15 minutes)
        plugin.getRedisManager().setEx("corehost:discord_link:code:" + code, playerUuid.toString(), 900);
        return code;
    }

    /**
     * Checks if a player has linked their Discord account.
     */
    public boolean isLinked(UUID playerUuid) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return false;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM discord_links WHERE uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.isFromGuild()) return;

        String message = event.getMessage().getContentRaw().trim();
        String[] args = message.split("\\s+");

        if (args.length >= 1 && args[0].equalsIgnoreCase("/transfer")) {
            handleTransferCommand(event, args);
        } else if (message.matches("^\\d{6}$")) {
            handleLinkCommand(event, message);
        } else if (message.matches("^\\d{4}$")) {
            handle2FALogin(event, message);
        } else {
            event.getChannel().sendMessage("Commandes disponibles :\nEnvoyez simplement votre **code à 6 chiffres** pour lier votre compte Minecraft (ou votre **PIN à 4 chiffres** pour vous connecter).\n`/transfer <NouveauPseudo>` - Pour transférer vos données vers un nouveau pseudo").queue();
        }
    }

    private void handle2FALogin(MessageReceivedEvent event, String pin) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            event.getChannel().sendMessage("Erreur: Impossible de contacter la base de données.").queue();
            return;
        }

        String discordId = event.getAuthor().getId();
        String targetUuid = plugin.getRedisManager().get("corehost:discord_auth_code:" + pin);

        if (targetUuid != null) {
            String linkedDiscordId = getDiscordId(UUID.fromString(targetUuid));
            if (linkedDiscordId != null && linkedDiscordId.equals(discordId)) {
                plugin.getRedisManager().setEx("corehost:discord_auth:" + targetUuid, "true", 3600);
                plugin.getRedisManager().del("corehost:discord_auth_code:" + pin);
                event.getChannel().sendMessage("✅ Connexion autorisée ! Vous pouvez jouer.").queue();
            } else {
                event.getChannel().sendMessage("❌ Ce code de connexion appartient à un compte Minecraft qui n'est pas lié à votre compte Discord !").queue();
            }
        } else {
            event.getChannel().sendMessage("❌ PIN invalide ou expiré.").queue();
        }
    }

    public String getDiscordId(UUID uuid) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return null;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT discord_id FROM discord_links WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUuidByDiscordId(String discordId) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return null;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM discord_links WHERE discord_id = ?")) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("uuid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void handleLinkCommand(MessageReceivedEvent event, String code) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected() || plugin.getDatabaseManager() == null) {
            event.getChannel().sendMessage("Le système de liaison est temporairement indisponible.").queue();
            return;
        }

        String discordId = event.getAuthor().getId();
        String existingUuid = getUuidByDiscordId(discordId);
        if (existingUuid != null) {
            event.getChannel().sendMessage("Ce compte Discord est déjà lié à un compte Minecraft ! Vous ne pouvez lier qu'un seul compte Minecraft. Si vous avez changé de pseudo, utilisez `/transfer <NouveauPseudo>`.").queue();
            return;
        }

        String playerUuidStr = plugin.getRedisManager().get("corehost:discord_link:code:" + code);
        if (playerUuidStr == null) {
            event.getChannel().sendMessage("Ce code est invalide ou a expiré.").queue();
            return;
        }

        UUID playerUuid = UUID.fromString(playerUuidStr);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO discord_links (uuid, discord_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE discord_id = ?")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, discordId);
            stmt.setString(3, discordId);
            stmt.executeUpdate();
            
            plugin.getRedisManager().del("corehost:discord_link:code:" + code);
            plugin.getRedisManager().setEx("corehost:discord_auth:" + playerUuid.toString(), "true", 3600);
            plugin.getRedisManager().set("corehost:discord_link:player:" + playerUuid.toString(), discordId);
            event.getChannel().sendMessage("✅ Votre compte Minecraft a été lié avec succès ! Vous pouvez maintenant jouer.").queue();
        } catch (SQLException e) {
            e.printStackTrace();
            event.getChannel().sendMessage("Une erreur est survenue lors de la liaison.").queue();
        }
    }

    private void handleTransferCommand(MessageReceivedEvent event, String[] args) {
        if (plugin.getDatabaseManager() == null) {
            event.getChannel().sendMessage("Le système est temporairement indisponible.").queue();
            return;
        }

        String discordId = event.getAuthor().getId();
        String oldUuidStr = getUuidByDiscordId(discordId);

        if (oldUuidStr == null) {
            event.getChannel().sendMessage("Vous n'avez aucun compte Minecraft lié à ce compte Discord. Utilisez `/link <code>`.").queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("Veuillez spécifier votre nouveau pseudo. Exemple : `/transfer DarkKnight_Crack`").queue();
            return;
        }

        String newUsername = args[1];
        UUID newUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newUsername).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String newUuidStr = newUuid.toString();

        if (oldUuidStr.equals(newUuidStr)) {
            event.getChannel().sendMessage("Votre nouveau pseudo est le même que l'ancien.").queue();
            return;
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update players table
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE players SET uuid = ? WHERE uuid = ?")) {
                    stmt.setString(1, newUuidStr);
                    stmt.setString(2, oldUuidStr);
                    stmt.executeUpdate();
                }
                
                // Update friends table (player1)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE friends SET player1_uuid = ? WHERE player1_uuid = ?")) {
                    stmt.setString(1, newUuidStr);
                    stmt.setString(2, oldUuidStr);
                    stmt.executeUpdate();
                }
                
                // Update friends table (player2)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE friends SET player2_uuid = ? WHERE player2_uuid = ?")) {
                    stmt.setString(1, newUuidStr);
                    stmt.setString(2, oldUuidStr);
                    stmt.executeUpdate();
                }
                
                // Update discord_links
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE discord_links SET uuid = ? WHERE uuid = ?")) {
                    stmt.setString(1, newUuidStr);
                    stmt.setString(2, oldUuidStr);
                    stmt.executeUpdate();
                }
                
                conn.commit();
                event.getChannel().sendMessage("✅ Vos données ont été transférées avec succès vers le pseudo **" + newUsername + "** ! Vous pouvez maintenant vous connecter.").queue();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                event.getChannel().sendMessage("❌ Une erreur est survenue lors du transfert des données.").queue();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
