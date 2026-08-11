package fr.corehost.game.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.bukkit.event.Cancellable;

public class PreSlimeInstanceCreateEvent extends Event implements Cancellable {
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();
    private final String hostId;
    private final String gameType;
    private String templateName;

    public PreSlimeInstanceCreateEvent(String hostId, String gameType) {
        this.hostId = hostId;
        this.gameType = gameType;
        this.templateName = gameType; // par défaut, le template est le nom du jeu
    }

    public String getHostId() { return hostId; }
    public String getGameType() { return gameType; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
}
