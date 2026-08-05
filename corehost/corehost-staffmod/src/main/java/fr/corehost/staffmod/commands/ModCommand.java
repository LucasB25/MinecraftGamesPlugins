package fr.corehost.staffmod.commands;

import com.google.gson.JsonObject;
import fr.corehost.staffmod.StaffModPlugin;
import fr.corehost.staffmod.gui.PlayerSSGUI;
import fr.corehost.staffmod.gui.ReportGUI;
import fr.corehost.staffmod.gui.StaffListGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ModCommand implements TabExecutor {

    private final StaffModPlugin plugin;

    public ModCommand(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("staffmod.mod")) {
            player.sendMessage(plugin.getPrefix().append(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED)));
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("on")) {
                if (plugin.getModManager().isModMode(player.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà activé !", NamedTextColor.RED)));
                    return true;
                }
                plugin.getVanishManager().setVanished(player, true);
                plugin.getModManager().setModMode(player, true);
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (!plugin.getModManager().isModMode(player.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà désactivé !", NamedTextColor.RED)));
                    return true;
                }
                plugin.getModManager().setModMode(player, false);
                plugin.getVanishManager().setVanished(player, false);
                return true;
            } else if (args[0].equalsIgnoreCase("reports")) {
                new ReportGUI(plugin).open(player);
                return true;
            } else if (args[0].equalsIgnoreCase("staff")) {
                new StaffListGUI(plugin).open(player);
                return true;
            } else if (args[0].equalsIgnoreCase("sc")) {
                if (!player.hasPermission("staffmod.staffchat")) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED)));
                    return true;
                }
                if (args.length == 1) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Usage: /mod sc <message>", NamedTextColor.RED)));
                    return true;
                }
                
                String[] msgArgs = new String[args.length - 1];
                System.arraycopy(args, 1, msgArgs, 0, msgArgs.length);
                String message = String.join(" ", msgArgs);

                JsonObject json = new JsonObject();
                json.addProperty("action", "STAFF_CHAT");
                json.addProperty("sender", player.getName());
                
                String rankText = "";
                try {
                    LuckPerms api = LuckPermsProvider.get();
                    User user = api.getUserManager().getUser(player.getUniqueId());
                    if (user != null) {
                        String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                        if (lpPrefix != null) {
                            rankText = lpPrefix;
                        } else {
                            String group = user.getPrimaryGroup();
                            if (group != null) {
                                rankText = group.substring(0, 1).toUpperCase() + group.substring(1);
                            }
                        }
                    }
                } catch (Exception ignored) {}
                json.addProperty("rank", rankText);
                json.addProperty("message", message);

                if (plugin.getRedisManager() != null) {
                    plugin.getRedisManager().publish("corehost:staff:events", json.toString());
                }
                return true;
            } else if (args[0].equalsIgnoreCase("ss")) {
                if (!player.hasPermission("staffmod.mod")) return true;
                if (args.length != 2) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Usage: /mod ss <joueur>", NamedTextColor.RED)));
                    return true;
                }
                
                String targetName = args[1];
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Vous ne pouvez pas utiliser cela sur vous-même.", NamedTextColor.RED)));
                    return true;
                }
                new PlayerSSGUI(plugin, targetName).open(player);
                return true;
            }
        }

        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getPrefix().append(Component.text("Aide Modération", NamedTextColor.DARK_RED)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod on ", NamedTextColor.RED)).append(Component.text("- Active le mode modération", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod off ", NamedTextColor.RED)).append(Component.text("- Désactive le mode modération", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod staff ", NamedTextColor.RED)).append(Component.text("- Voir le staff en ligne", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod reports ", NamedTextColor.RED)).append(Component.text("- Gérer les signalements", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod sc <msg> ", NamedTextColor.RED)).append(Component.text("- Message dans le Staff Chat", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/mod ss <joueur> ", NamedTextColor.RED)).append(Component.text("- Modération d'un joueur", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" ▪ ", NamedTextColor.DARK_GRAY).append(Component.text("/staffmod_report <id> ", NamedTextColor.RED)).append(Component.text("- Gérer un signalement", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("staffmod.mod")) {
            if ("on".startsWith(args[0].toLowerCase())) completions.add("on");
            if ("off".startsWith(args[0].toLowerCase())) completions.add("off");
            if ("reports".startsWith(args[0].toLowerCase())) completions.add("reports");
            if ("staff".startsWith(args[0].toLowerCase())) completions.add("staff");
            if ("sc".startsWith(args[0].toLowerCase())) completions.add("sc");
            if ("ss".startsWith(args[0].toLowerCase())) completions.add("ss");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ss") && sender.hasPermission("staffmod.mod")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[1].toLowerCase()) && !online.getName().equalsIgnoreCase(sender.getName())) {
                    completions.add(online.getName());
                }
            }
        }
        return completions;
    }
}

