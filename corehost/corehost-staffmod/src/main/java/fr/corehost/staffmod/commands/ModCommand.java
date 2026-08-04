package fr.corehost.staffmod.commands;

import fr.corehost.staffmod.StaffModPlugin;
import fr.corehost.staffmod.gui.ModGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.command.TabExecutor;

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
                plugin.getModManager().setModMode(player, true);
                plugin.getVanishManager().setVanished(player, true);
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.getModManager().setModMode(player, false);
                plugin.getVanishManager().setVanished(player, false);
                return true;
            } else if (args[0].equalsIgnoreCase("gui")) {
                new ModGUI(plugin).open(player);
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

                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("action", "STAFF_CHAT");
                json.addProperty("sender", player.getName());
                
                String rankText = "";
                try {
                    net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                    net.luckperms.api.model.user.User user = api.getUserManager().getUser(player.getUniqueId());
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
            }
        }

        player.sendMessage(Component.text("----- Aide Moderation -----", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/mod on ", NamedTextColor.YELLOW).append(Component.text("- Active le mode moderation et le vanish", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mod off ", NamedTextColor.YELLOW).append(Component.text("- Desactive le mode moderation et le vanish", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mod gui ", NamedTextColor.YELLOW).append(Component.text("- Ouvre le menu de moderation", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mod sc <msg> ", NamedTextColor.YELLOW).append(Component.text("- Envoyer un message dans le Staff Chat", NamedTextColor.WHITE)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("staffmod.mod")) {
            if ("on".startsWith(args[0].toLowerCase())) completions.add("on");
            if ("off".startsWith(args[0].toLowerCase())) completions.add("off");
            if ("gui".startsWith(args[0].toLowerCase())) completions.add("gui");
            if ("sc".startsWith(args[0].toLowerCase())) completions.add("sc");
        }
        return completions;
    }
}
