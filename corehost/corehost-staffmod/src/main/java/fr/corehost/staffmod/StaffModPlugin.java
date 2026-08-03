package fr.corehost.staffmod;

import fr.corehost.staffmod.commands.ReportMessageCommand;
import fr.corehost.staffmod.listeners.ChatListener;
import fr.corehost.staffmod.manager.ReportManager;
import org.bukkit.plugin.java.JavaPlugin;

public class StaffModPlugin extends JavaPlugin {

    private ReportManager reportManager;

    @Override
    public void onEnable() {
        this.reportManager = new ReportManager();

        // Register event
        getServer().getPluginManager().registerEvents(new ChatListener(reportManager), this);

        // Register command
        getCommand("staffmod_report").setExecutor(new ReportMessageCommand(reportManager));

        getLogger().info("StaffMod a été activé avec succès !");
    }

    @Override
    public void onDisable() {
        getLogger().info("StaffMod a été désactivé.");
    }
}
