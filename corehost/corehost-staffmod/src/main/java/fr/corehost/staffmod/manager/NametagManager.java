package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class NametagManager implements Listener {

    private final StaffModPlugin plugin;

    public NametagManager(StaffModPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateNametags, 20L, 20L); // Update every second
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeNametagTeam(event.getPlayer());
    }

    public void cleanup() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            removeNametagTeam(online);
        }
    }

    private void removeNametagTeam(Player player) {
        String teamName = getTeamName(player);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board != null) {
                Team team = board.getTeam(teamName);
                if (team != null) {
                    team.unregister();
                }
            }
        }
    }

    private String getTeamName(Player player) {
        String name = player.getName();
        String teamName = "nt_" + name;
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        return teamName;
    }

    private void updateNametags() {
        Map<UUID, String> prefixes = new HashMap<>();
        
        // 1. Fetch prefixes for all online players once
        for (Player target : Bukkit.getOnlinePlayers()) {
            String prefixText = "&7Joueurs ";
            try {
                LuckPerms api = LuckPermsProvider.get();
                User user = api.getUserManager().getUser(target.getUniqueId());
                if (user != null) {
                    String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                    if (lpPrefix != null) {
                        prefixText = lpPrefix + " ";
                    } else {
                        String group = user.getPrimaryGroup();
                        if (group != null) {
                            String defaultGroup = plugin.getConfig().getString("luckperms.default-group", "default");
                            if (group.equalsIgnoreCase(defaultGroup)) {
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
            prefixes.put(target.getUniqueId(), prefix);
        }

        // 2. Apply to all scoreboards
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;

            for (Player target : Bukkit.getOnlinePlayers()) {
                String teamName = getTeamName(target);

                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                }

                String prefix = prefixes.getOrDefault(target.getUniqueId(), ChatColor.GRAY + "Joueurs ");
                team.setPrefix(prefix);

                String lastColors = ChatColor.getLastColors(prefix);
                if (!lastColors.isEmpty()) {
                    ChatColor color = ChatColor.getByChar(lastColors.charAt(lastColors.length() - 1));
                    if (color != null) {
                        team.setColor(color);
                    }
                }

                if (!team.hasEntry(target.getName())) {
                    team.addEntry(target.getName());
                }
            }
        }
    }
}

