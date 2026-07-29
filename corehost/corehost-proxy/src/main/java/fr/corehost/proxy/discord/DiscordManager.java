package fr.corehost.proxy.discord;

import fr.corehost.proxy.CoreHostProxy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return false;
        String discordId = plugin.getRedisManager().get("corehost:discord_link:player:" + playerUuid.toString());
        return discordId != null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // We only accept commands in Private Messages (Direct Messages)
        if (event.isFromGuild()) return;

        String message = event.getMessage().getContentRaw().trim();
        String[] args = message.split("\\s+");

        if (args.length >= 1 && args[0].equalsIgnoreCase("/transfer")) {
            handleTransferCommand(event, args);
        } else if (message.matches("^\\d{6}$")) {
            // If the message is exactly a 6-digit code, treat it as a link attempt
            handleLinkCommand(event, message);
        } else if (message.matches("^\\d{4}$")) {
            // If the message is exactly a 4-digit code, treat it as a 2FA login PIN
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
            // Verify that this Discord account actually owns the Minecraft account they are trying to log into
            String linkedDiscordId = plugin.getRedisManager().get("corehost:discord_link:player:" + targetUuid);
            if (linkedDiscordId != null && linkedDiscordId.equals(discordId)) {
                // Approved!
                plugin.getRedisManager().setEx("corehost:discord_auth:" + targetUuid, "true", 3600); // 1 hour session
                plugin.getRedisManager().del("corehost:discord_auth_code:" + pin); // Invalidate PIN
                event.getChannel().sendMessage("✅ Connexion autorisée ! Vous pouvez jouer.").queue();
            } else {
                event.getChannel().sendMessage("❌ Ce code de connexion appartient à un compte Minecraft qui n'est pas lié à votre compte Discord !").queue();
            }
        } else {
            event.getChannel().sendMessage("❌ PIN invalide ou expiré.").queue();
        }
    }

    private void handleLinkCommand(MessageReceivedEvent event, String code) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            event.getChannel().sendMessage("Le système de liaison est temporairement indisponible.").queue();
            return;
        }

        String discordId = event.getAuthor().getId();

        // 1. Check if this Discord account is already linked to another Minecraft account
        String existingUuid = plugin.getRedisManager().get("corehost:discord_link:discord:" + discordId);
        if (existingUuid != null) {
            event.getChannel().sendMessage("Ce compte Discord est déjà lié à un compte Minecraft ! Vous ne pouvez lier qu'un seul compte Minecraft. Si vous avez changé de pseudo, utilisez `/transfer <NouveauPseudo>`.").queue();
            return;
        }

        // 2. Validate Code
        String playerUuidStr = plugin.getRedisManager().get("corehost:discord_link:code:" + code);
        if (playerUuidStr == null) {
            event.getChannel().sendMessage("Ce code est invalide ou a expiré.").queue();
            return;
        }

        // 3. Link them together
        UUID playerUuid = UUID.fromString(playerUuidStr);
        plugin.getRedisManager().set("corehost:discord_link:player:" + playerUuidStr, discordId);
        plugin.getRedisManager().set("corehost:discord_link:discord:" + discordId, playerUuidStr);
        plugin.getRedisManager().del("corehost:discord_link:code:" + code);

        event.getChannel().sendMessage("✅ Votre compte Minecraft a été lié avec succès ! Vous pouvez maintenant jouer.").queue();
    }
    private void handleTransferCommand(MessageReceivedEvent event, String[] args) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            event.getChannel().sendMessage("Le système est temporairement indisponible.").queue();
            return;
        }

        String discordId = event.getAuthor().getId();
        String oldUuidStr = plugin.getRedisManager().get("corehost:discord_link:discord:" + discordId);

        if (oldUuidStr == null) {
            event.getChannel().sendMessage("Vous n'avez aucun compte Minecraft lié à ce compte Discord. Utilisez `/link <code>`.").queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("Veuillez spécifier votre nouveau pseudo. Exemple : `/transfer DarkKnight_Crack`").queue();
            return;
        }

        String newUsername = args[1];
        
        // Compute the Offline UUID for the new username
        // The UUID is generated using UUID.nameUUIDFromBytes(("OfflinePlayer:" + newUsername).getBytes(StandardCharsets.UTF_8))
        UUID newUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newUsername).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String newUuidStr = newUuid.toString();

        if (oldUuidStr.equals(newUuidStr)) {
            event.getChannel().sendMessage("Votre nouveau pseudo est le même que l'ancien.").queue();
            return;
        }

        // --- Data Transfer (Redis Keys) ---
        // 1. Transfer corehost:friends:<uuid>
        String oldFriends = plugin.getRedisManager().get("corehost:friends:" + oldUuidStr);
        if (oldFriends != null) plugin.getRedisManager().set("corehost:friends:" + newUuidStr, oldFriends);
        plugin.getRedisManager().del("corehost:friends:" + oldUuidStr);

        // 2. Transfer settings
        String oldSettings = plugin.getRedisManager().get("corehost:settings:friend_requests_blocked:" + oldUuidStr);
        if (oldSettings != null) plugin.getRedisManager().set("corehost:settings:friend_requests_blocked:" + newUuidStr, oldSettings);
        plugin.getRedisManager().del("corehost:settings:friend_requests_blocked:" + oldUuidStr);

        // --- Update Link ---
        plugin.getRedisManager().set("corehost:discord_link:player:" + newUuidStr, discordId);
        plugin.getRedisManager().set("corehost:discord_link:discord:" + discordId, newUuidStr);
        plugin.getRedisManager().del("corehost:discord_link:player:" + oldUuidStr);

        // Update their cached name just in case
        plugin.getRedisManager().set("corehost:friend_names:" + newUuidStr, newUsername);

        event.getChannel().sendMessage("✅ Vos données ont été transférées avec succès vers le pseudo **" + newUsername + "** ! Vous pouvez maintenant vous connecter.").queue();
    }
}
