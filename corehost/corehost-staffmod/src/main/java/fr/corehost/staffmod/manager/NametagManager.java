package fr.corehost.staffmod.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import fr.corehost.staffmod.StaffModPlugin;

@SuppressWarnings("deprecation")
public class NametagManager {

    private final StaffModPlugin plugin;

    public NametagManager(StaffModPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateNametags, 20L, 20L); // Update every second
    }

    private void updateNametags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;

            for (Player target : Bukkit.getOnlinePlayers()) {
                String teamName = "nt_" + target.getName();
                if (teamName.length() > 16) teamName = teamName.substring(0, 16);

                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                }

                String prefixText = "&7Joueurs ";
                try {
                    net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                    net.luckperms.api.model.user.User user = api.getUserManager().getUser(target.getUniqueId());
                    if (user != null) {
                        String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                        if (lpPrefix != null) {
                            prefixText = lpPrefix + " ";
                        } else {
                            String group = user.getPrimaryGroup();
                            if (group != null) {
                                if (group.equalsIgnoreCase("default")) {
                                    prefixText = "&7Joueurs ";
                                } else if (group.equalsIgnoreCase("admin") || group.equalsIgnoreCase("administrateur")) {
                                    prefixText = "&c" + group.substring(0, 1).toUpperCase() + group.substring(1) + " ";
                                } else if (group.equalsIgnoreCase("modo") || group.equalsIgnoreCase("moderateur")) {
                                    prefixText = "&2" + group.substring(0, 1).toUpperCase() + group.substring(1) + " ";
                                } else {
                                    prefixText = "&b" + group.substring(0, 1).toUpperCase() + group.substring(1) + " ";
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                String prefix = ChatColor.translateAlternateColorCodes('&', prefixText);
                if (prefix.length() > 63) prefix = prefix.substring(0, 63);
                team.setPrefix(prefix);

                String lastColors = ChatColor.getLastColors(prefix);
                if (!lastColors.isEmpty()) {
                    team.setColor(ChatColor.getByChar(lastColors.charAt(lastColors.length() - 1)));
                }

                if (!team.hasEntry(target.getName())) {
                    team.addEntry(target.getName());
                }
            }
        }
    }
}
