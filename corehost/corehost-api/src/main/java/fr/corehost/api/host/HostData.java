package fr.corehost.api.host;

import java.util.UUID;

public class HostData {
    private UUID hostId;
    private UUID ownerUuid;
    private String ownerName;
    private String gameType;
    private String serverName;
    private HostStatus status;
    private int maxPlayers;
    private int currentPlayers;

    public HostData(UUID hostId, UUID ownerUuid, String ownerName, String gameType, String serverName, int maxPlayers) {
        this.hostId = hostId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.gameType = gameType;
        this.serverName = serverName;
        this.status = HostStatus.STARTING;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = 0;
    }

    // Used for deserialization
    public HostData() {}

    public UUID getHostId() { return hostId; }
    public void setHostId(UUID hostId) { this.hostId = hostId; }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public HostStatus getStatus() { return status; }
    public void setStatus(HostStatus status) { this.status = status; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }
}
