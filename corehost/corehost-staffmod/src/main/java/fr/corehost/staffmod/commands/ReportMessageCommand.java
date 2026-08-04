package fr.corehost.staffmod.commands;

import fr.corehost.staffmod.manager.ReportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReportMessageCommand implements CommandExecutor {

    private final ReportManager reportManager;
    private final java.util.Map<UUID, Long> cooldowns = new java.util.HashMap<>();
    private static final long COOLDOWN_TIME = 5000L; // 5 seconds

    public ReportMessageCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seul un joueur peut signaler un message.");
            return true;
        }

        Player reporter = (Player) sender;

        if (cooldowns.containsKey(reporter.getUniqueId())) {
            long timeLeft = (cooldowns.get(reporter.getUniqueId()) + COOLDOWN_TIME) - System.currentTimeMillis();
            if (timeLeft > 0) {
                reporter.sendMessage(Component.text("Veuillez patienter " + (timeLeft / 1000) + " secondes avant de signaler un autre message.", NamedTextColor.RED));
                return true;
            }
        }

        if (args.length != 1) {
            reporter.sendMessage(Component.text("Erreur: Argument manquant.", NamedTextColor.RED));
            return true;
        }

        UUID messageId;
        try {
            messageId = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            reporter.sendMessage(Component.text("Erreur: ID de message invalide.", NamedTextColor.RED));
            return true;
        }

        ReportManager.CachedMessage cachedMessage = reportManager.getLocalMessage(messageId);

        if (cachedMessage == null) {
            reporter.sendMessage(Component.text("Erreur: Ce message a expiré ou n'existe plus.", NamedTextColor.RED));
            return true;
        }

        if (cachedMessage.getSenderName().equalsIgnoreCase(reporter.getName())) {
            reporter.sendMessage(Component.text("Vous ne pouvez pas signaler vos propres messages.", NamedTextColor.RED));
            return true;
        }

        // Send confirmation to the reporter
        reporter.sendMessage(Component.text("Vous avez signalé un message de ", NamedTextColor.GREEN)
                .append(Component.text(cachedMessage.getSenderName(), NamedTextColor.YELLOW))
                .append(Component.text(".", NamedTextColor.GREEN)));

        // Create global active report
        reportManager.createActiveReport(messageId, cachedMessage, "server");

        cooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());

        return true;
    }
}
