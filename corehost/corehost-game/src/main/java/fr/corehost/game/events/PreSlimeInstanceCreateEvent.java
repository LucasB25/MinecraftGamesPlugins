package fr.corehost.game.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreSlimeInstanceCreateEvent extends Event {
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
}
