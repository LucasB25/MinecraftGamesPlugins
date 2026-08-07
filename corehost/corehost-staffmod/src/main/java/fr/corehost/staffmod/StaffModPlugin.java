package fr.corehost.staffmod;

import fr.corehost.api.redis.RedisManager;
import fr.corehost.staffmod.commands.ModCommand;
import fr.corehost.staffmod.commands.ReportMessageCommand;
import fr.corehost.staffmod.gui.GUIListener;
import fr.corehost.staffmod.listeners.ChatListener;
import fr.corehost.staffmod.listeners.ModInteractListener;
import fr.corehost.staffmod.listeners.StaffListener;
import fr.corehost.staffmod.manager.FreezeManager;
import fr.corehost.staffmod.manager.ModManager;
import fr.corehost.staffmod.manager.NametagManager;
import fr.corehost.staffmod.manager.ReportManager;
import fr.corehost.staffmod.manager.VanishManager;
import fr.corehost.staffmod.redis.StaffPubSubListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public class StaffModPlugin extends JavaPlugin {

    private ReportManager reportManager;
    private RedisManager redisManager;
    private VanishManager vanishManager;
    private FreezeManager freezeManager;
    private ModManager modManager;
    private NametagManager nametagManager;
    
    private Component prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String rawPrefix = getConfig().getString("settings.prefix", "&8[&cStaff&8] &7");
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(rawPrefix);
        
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        
        this.redisManager = new RedisManager(redisHost, redisPort, redisPassword);
        
        if (!this.redisManager.isConnected()) {
            getLogger().severe("Impossible de se connecter à Redis depuis StaffMod !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.reportManager = new ReportManager(this.redisManager);
        this.vanishManager = new VanishManager(this);
        this.freezeManager = new FreezeManager(this);
        this.modManager = new ModManager(this);
        
        // Listen to global events
        this.redisManager.subscribe(new StaffPubSubListener(this), "corehost:staff:events");

        // Register events
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffListener(modManager, freezeManager, vanishManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ModInteractListener(this), this);

        // Register commands
        getCommand("staffmod_report").setExecutor(new ReportMessageCommand(reportManager));
        getCommand("mod").setExecutor(new ModCommand(this));

        // Start global Nametag manager
        this.nametagManager = new NametagManager(this);
        getServer().getPluginManager().registerEvents(this.nametagManager, this);

        // Register BungeeCord channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("StaffMod a été activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (this.nametagManager != null) {
            this.nametagManager.cleanup();
        }
        if (this.redisManager != null) {
            this.redisManager.close();
        }
        getLogger().info("StaffMod a été désactivé.");
    }
    
    public RedisManager getRedisManager() {
        return redisManager;
    }
    
    public ReportManager getReportManager() {
        return reportManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public ModManager getModManager() {
        return modManager;
    }
    
    public Component getPrefix() {
        return prefix;
    }
}

