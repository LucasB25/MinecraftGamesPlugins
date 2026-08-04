package fr.corehost.staffmod;
import fr.corehost.staffmod.commands.ModCommand;
import fr.corehost.staffmod.commands.ReportMessageCommand;
import fr.corehost.staffmod.gui.GUIListener;
import fr.corehost.staffmod.listeners.ChatListener;
import fr.corehost.staffmod.listeners.StaffListener;
import fr.corehost.staffmod.manager.FreezeManager;
import fr.corehost.staffmod.manager.ModManager;
import fr.corehost.staffmod.manager.ReportManager;
import fr.corehost.staffmod.manager.VanishManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class StaffModPlugin extends JavaPlugin {

    private ReportManager reportManager;
    private fr.corehost.api.redis.RedisManager redisManager;
    private VanishManager vanishManager;
    private FreezeManager freezeManager;
    private ModManager modManager;
    
    private Component prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String rawPrefix = getConfig().getString("settings.prefix", "&8[&cStaff&8] &7");
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(rawPrefix);
        
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        
        this.redisManager = new fr.corehost.api.redis.RedisManager(redisHost, redisPort, redisPassword);
        
        if (!this.redisManager.isConnected()) {
            getLogger().severe("Impossible de se connecter a Redis depuis StaffMod !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.reportManager = new ReportManager(this.redisManager);
        this.vanishManager = new VanishManager(this);
        this.freezeManager = new FreezeManager(this);
        this.modManager = new ModManager(this);
        
        // Listen to global events
        this.redisManager.subscribe(new fr.corehost.staffmod.redis.StaffPubSubListener(this), "corehost:staff:events");

        // Register event
        getServer().getPluginManager().registerEvents(new ChatListener(reportManager), this);
        getServer().getPluginManager().registerEvents(new StaffListener(modManager, freezeManager, vanishManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Register command
        getCommand("staffmod_report").setExecutor(new ReportMessageCommand(reportManager));
        getCommand("mod").setExecutor(new ModCommand(this));

        // Start global Nametag updater
        new fr.corehost.staffmod.manager.NametagManager(this);

        getLogger().info("StaffMod a ete active avec succes !");
    }

    @Override
    public void onDisable() {
        if (this.redisManager != null) {
            this.redisManager.close();
        }
        getLogger().info("StaffMod a ete desactive.");
    }
    
    public fr.corehost.api.redis.RedisManager getRedisManager() {
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
