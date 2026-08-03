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
                
                // Cache the message and get the unique ID
                UUID messageId = reportManager.cacheMessage(source.getName(), plainTextMsg);
                
                // Create the warning triangle component
                Component warningIcon = Component.text("⚠ ")
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text("Signaler ce message", NamedTextColor.RED)))
                        .clickEvent(ClickEvent.runCommand("/staffmod_report " + messageId.toString()));
                
                // Render the original message
                Component originalMessage = originalRenderer.render(source, sourceDisplayName, message, viewer);
                
                // Do not show the warning triangle to the player who sent the message
                if (viewer instanceof Player && ((Player) viewer).getUniqueId().equals(source.getUniqueId())) {
                    return originalMessage;
                }
                
                // Prepend the warning icon
                return warningIcon.append(originalMessage);
            }
        });
    }
}
