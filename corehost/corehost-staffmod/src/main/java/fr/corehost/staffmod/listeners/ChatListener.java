package fr.corehost.staffmod.listeners;

import fr.corehost.staffmod.manager.ReportManager;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {

    private final fr.corehost.staffmod.StaffModPlugin plugin;
    private final ReportManager reportManager;
    private final java.util.Map<UUID, Long> lastMessageTime = new java.util.HashMap<>();
    private final java.util.Map<UUID, String> lastMessageContent = new java.util.HashMap<>();
    
    private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile("(?i)\\b(?:https?://)?(?:www\\.)?[a-z0-9-]+\\.(?:com|org|net|fr|eu|io|gg)\\b");
    private static final java.util.regex.Pattern IP_PATTERN = java.util.regex.Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

    public ChatListener(fr.corehost.staffmod.StaffModPlugin plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player source = event.getPlayer();
        String plainTextMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        if (!source.hasPermission("staffmod.bypasschat")) {
            long now = System.currentTimeMillis();
            if (lastMessageTime.containsKey(source.getUniqueId()) && (now - lastMessageTime.get(source.getUniqueId())) < 1500) {
                source.sendMessage(Component.text("Veuillez patienter entre chaque message.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            if (lastMessageContent.containsKey(source.getUniqueId()) && lastMessageContent.get(source.getUniqueId()).equalsIgnoreCase(plainTextMsg)) {
                source.sendMessage(Component.text("Veuillez ne pas répéter le même message.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            
            lastMessageTime.put(source.getUniqueId(), now);
            lastMessageContent.put(source.getUniqueId(), plainTextMsg);
            
            if (URL_PATTERN.matcher(plainTextMsg).find() || IP_PATTERN.matcher(plainTextMsg).find()) {
                source.sendMessage(Component.text("Les liens et adresses IP sont interdits dans le chat.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            
            String lowerMsg = plainTextMsg.toLowerCase();
            java.util.List<String> forbiddenWords = plugin.getConfig().getStringList("chat.forbidden_words");
            if (forbiddenWords == null || forbiddenWords.isEmpty()) {
                forbiddenWords = java.util.Arrays.asList("fdp", "connard", "salope", "tg", "ntm", "bite", "pute", "enculé", "ez");
            }
            
            for (String word : forbiddenWords) {
                if (lowerMsg.matches(".*\\b" + word + "\\b.*")) {
                    source.sendMessage(Component.text("Votre message contient un vocabulaire inapproprié.", NamedTextColor.RED));
                    event.setCancelled(true);
                    return;
                }
            }
        }

        
        // Cache the message once for the entire chat event
        final UUID messageId = reportManager.cacheLocalMessage(source.getName(), plainTextMsg);

        // Wrap the renderer
        event.renderer(new ChatRenderer() {
            @Override
            public Component render(Player sourcePlayer, Component sourceDisplayName, Component message, Audience viewer) {
                // Create the warning triangle component using the pre-generated messageId
                Component warningIcon = Component.text("⚠ ")
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text("Signaler ce message", NamedTextColor.RED)))
                        .clickEvent(ClickEvent.runCommand("/staffmod_report " + messageId.toString()));
                
                // Determine player's prefix and color based on LuckPerms
                String prefixText = "&7Joueurs";
                try {
                    net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                    net.luckperms.api.model.user.User user = api.getUserManager().getUser(sourcePlayer.getUniqueId());
                    if (user != null) {
                        String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                        if (lpPrefix != null) {
                            prefixText = lpPrefix;
                        } else {
                            String group = user.getPrimaryGroup();
                            if (group != null) {
                                if (group.equalsIgnoreCase("default")) {
                                    prefixText = "&7Joueurs";
                                } else if (group.equalsIgnoreCase("admin") || group.equalsIgnoreCase("administrateur")) {
                                    prefixText = "&c" + group.substring(0, 1).toUpperCase() + group.substring(1);
                                } else if (group.equalsIgnoreCase("modo") || group.equalsIgnoreCase("moderateur")) {
                                    prefixText = "&2" + group.substring(0, 1).toUpperCase() + group.substring(1);
                                } else {
                                    prefixText = "&b" + group.substring(0, 1).toUpperCase() + group.substring(1);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixText);

                // Format: [Prefix] PlayerName » Message
                Component formattedMessage = Component.empty()
                        .append(prefixComponent)
                        .append(Component.text(" "))
                        .append(Component.text(sourcePlayer.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(message.color(NamedTextColor.GRAY));
                
                // Do not show the warning triangle to the player who sent the message
                if (viewer instanceof Player && ((Player) viewer).getUniqueId().equals(sourcePlayer.getUniqueId())) {
                    return formattedMessage;
                }
                
                // Console sender shouldn't get clickable text
                if (viewer instanceof ConsoleCommandSender) {
                    return formattedMessage;
                }
                
                if (viewer instanceof Player && !((Player) viewer).hasPermission("staffmod.use")) {
                    return formattedMessage;
                }
                
                // Prepend the warning icon
                return warningIcon.append(formattedMessage);
            }
        });
    }
}

