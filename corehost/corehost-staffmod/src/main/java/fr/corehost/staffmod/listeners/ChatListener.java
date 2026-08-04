package fr.corehost.staffmod.listeners;

import fr.corehost.staffmod.manager.ReportManager;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {

    private final ReportManager reportManager;

    public ChatListener(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Retrieve the current renderer (could be modified by EssentialsChat, VentureChat, etc.)
        ChatRenderer originalRenderer = event.renderer();

        // Wrap the renderer
        event.renderer(new ChatRenderer() {
            @Override
            public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
                // Get plain text of the message for caching
                String plainTextMsg = PlainTextComponentSerializer.plainText().serialize(message);
                
                // Cache the message locally and get the unique ID
                UUID messageId = reportManager.cacheLocalMessage(source.getName(), plainTextMsg);
                
                // Create the warning triangle component
                Component warningIcon = Component.text("⚠ ")
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text("Signaler ce message", NamedTextColor.RED)))
                        .clickEvent(ClickEvent.runCommand("/staffmod_report " + messageId.toString()));
                
                // Determine player's prefix and color based on LuckPerms
                String prefixText = "&7Joueurs";
                try {
                    net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                    net.luckperms.api.model.user.User user = api.getUserManager().getUser(source.getUniqueId());
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

                Component prefixComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(prefixText);

                // Format: [Prefix] PlayerName » Message
                Component formattedMessage = Component.empty()
                        .append(prefixComponent)
                        .append(Component.text(" "))
                        .append(Component.text(source.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(message.color(NamedTextColor.GRAY));
                
                // Do not show the warning triangle to the player who sent the message
                if (viewer instanceof Player && ((Player) viewer).getUniqueId().equals(source.getUniqueId())) {
                    return formattedMessage;
                }
                
                // Only show warning icon to those who have permission (and console shouldn't get clickable text if possible, but we check permission)
                if (viewer instanceof org.bukkit.command.ConsoleCommandSender) {
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
