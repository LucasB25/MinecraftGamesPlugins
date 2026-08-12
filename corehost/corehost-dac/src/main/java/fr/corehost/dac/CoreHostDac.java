package fr.corehost.dac;

import org.bukkit.plugin.java.JavaPlugin;

public class CoreHostDac extends JavaPlugin {

    private DacMapManager mapManager;
    private DacGameManager gameManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostDac activation...");
        saveDefaultConfig();

        // Load map manager
        this.mapManager = new DacMapManager(this);
        this.mapManager.loadMaps();

        // Initialize game manager
        this.gameManager = new DacGameManager(this);

        // Register commands and listeners
        getCommand("dacsetup").setExecutor(new fr.corehost.dac.commands.DacSetupCommand(this));
        getCommand("dacsetup").setTabCompleter(new fr.corehost.dac.commands.DacSetupCommand(this));
        
        fr.corehost.dac.commands.CoreHostDacCommand command = new fr.corehost.dac.commands.CoreHostDacCommand(this);
        if (getCommand("corehostdac") != null) getCommand("corehostdac").setExecutor(command);
        
        fr.corehost.dac.commands.DacCommand dacCommand = new fr.corehost.dac.commands.DacCommand(this);
        if (getCommand("dac") != null) getCommand("dac").setExecutor(dacCommand);
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new DacListener(this), this);

        getLogger().info("CoreHostDac activated!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoreHostDac deactivated.");
        if (gameManager != null && fr.corehost.game.CoreHostGame.getInstance() != null && fr.corehost.game.CoreHostGame.getInstance().getRedisManager() != null) {
            fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(
                fr.corehost.game.CoreHostGame.getInstance().getRedisManager()
            );
            for (DacGameInstance instance : gameManager.getActiveInstances()) {
                try {
                    hostManager.deleteHost(java.util.UUID.fromString(instance.getHostId())).join();
                } catch (Exception e) {
                    getLogger().warning("Failed to delete host from redis on shutdown: " + e.getMessage());
                }
                
                for (java.util.UUID uuid : instance.getPlayers()) {
                    org.bukkit.entity.Player p = getServer().getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        try {
                            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                            out.writeUTF("Connect");
                            out.writeUTF("lobby");
                            p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    public DacMapManager getMapManager() {
        return mapManager;
    }

    public DacGameManager getGameManager() {
        return gameManager;
    }
}
