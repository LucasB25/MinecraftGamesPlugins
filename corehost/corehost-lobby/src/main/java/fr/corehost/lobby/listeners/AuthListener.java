package fr.corehost.lobby.listeners;

import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.Constants;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AuthListener implements Listener {

    private final CoreHostLobby plugin;
    private final Random random = new Random();
    private static final Map<UUID, String> blockedPlayers = new HashMap<>(); // UUID -> Code or PIN
    private final Map<UUID, BukkitRunnable> actionbarTasks = new HashMap<>();
    
    public static boolean isBlocked(UUID uuid) {
        return blockedPlayers.containsKey(uuid);
    }
    private final Map<UUID, Boolean> isPending2FA = new HashMap<>(); // True if waiting for 2FA, False if waiting for Link

    public AuthListener(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    private boolean isCracked(UUID uuid) {
        return uuid.version() == 3;
    }

    private boolean isLinked(UUID uuid) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return true; 
        
        String discordId = plugin.getRedisManager().get("corehost:discord_link:player:" + uuid.toString());
        return discordId != null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isCracked(uuid)) {
            if (!isLinked(uuid)) {
                // Player is cracked and not linked. Generate a 6-digit code for them.
                String code = String.format("%06d", random.nextInt(1000000));
                
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    plugin.getRedisManager().setEx("corehost:discord_link:code:" + code, uuid.toString(), 3600);
                }
                
                blockedPlayers.put(uuid, code);
                isPending2FA.put(uuid, false);

                player.sendTitle(ChatColor.RED + "Compte Non Lié", ChatColor.YELLOW + "Liez votre Discord pour jouer !", 10, 70, 20);
                
                String botId = null;
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    botId = plugin.getRedisManager().get("corehost:discord_bot_id");
                }
                String discordUrl = (botId != null && !botId.isEmpty()) ? "https://discord.com/users/" + botId : plugin.getConfig().getString("settings.discord-url", "https://discord.gg/corehost");
                
                TextComponent msg1 = new TextComponent("\n" + Constants.BUNGEE_PREFIX + net.md_5.bungee.api.ChatColor.RED + "Vous devez lier votre compte Discord !\n");
                TextComponent msg2 = new TextComponent("👉 Cliquez ici pour ouvrir le profil du Bot 👈");
                msg2.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                msg2.setBold(true);
                msg2.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordUrl));
                msg2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Ouvrir Discord pour envoyer un MP").color(net.md_5.bungee.api.ChatColor.YELLOW).create()));
                player.spigot().sendMessage(msg1, msg2, new TextComponent("\n"));
                
                startAuthTask(player, uuid, false, code);
                
            } else {
                // Player is cracked and ALREADY linked. Require 2FA PIN.
                String pin = String.format("%04d", random.nextInt(10000));
                
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    plugin.getRedisManager().setEx("corehost:discord_auth_code:" + pin, uuid.toString(), 300); // 5 minutes
                    plugin.getRedisManager().del("corehost:discord_auth:" + uuid.toString()); // Clear previous auth
                }
                
                blockedPlayers.put(uuid, pin);
                isPending2FA.put(uuid, true);

                player.sendTitle(ChatColor.GOLD + "Vérification 2FA", ChatColor.YELLOW + "Autorisez la connexion via Discord", 10, 70, 20);
                
                String botId = null;
                if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    botId = plugin.getRedisManager().get("corehost:discord_bot_id");
                }
                String discordUrl = (botId != null && !botId.isEmpty()) ? "https://discord.com/users/" + botId : plugin.getConfig().getString("settings.discord-url", "https://discord.gg/corehost");
                
                TextComponent msg1 = new TextComponent("\n" + Constants.BUNGEE_PREFIX + net.md_5.bungee.api.ChatColor.GOLD + "Authentification 2FA requise !\n");
                TextComponent msg2 = new TextComponent("👉 Cliquez ici pour MP le Bot 👈");
                msg2.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                msg2.setBold(true);
                msg2.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordUrl));
                msg2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Ouvrir Discord pour envoyer un MP").color(net.md_5.bungee.api.ChatColor.YELLOW).create()));
                player.spigot().sendMessage(msg1, msg2, new TextComponent("\n"));
                
                startAuthTask(player, uuid, true, pin);
            }
        }
    }

    private void startAuthTask(Player player, UUID uuid, boolean is2FA, String expectedCode) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                boolean approved = false;
                if (is2FA) {
                    if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                        String authStr = plugin.getRedisManager().get("corehost:discord_auth:" + uuid.toString());
                        if (authStr != null && authStr.equals("true")) {
                            approved = true;
                        }
                    }
                } else {
                    approved = isLinked(uuid);
                }
                
                if (approved) {
                    blockedPlayers.remove(uuid);
                    isPending2FA.remove(uuid);
                    if (is2FA) {
                        player.sendTitle(ChatColor.GREEN + "Accès Autorisé", ChatColor.GRAY + "Bon jeu sur CoreHost !", 10, 70, 20);
                        player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Authentification réussie !");
                    } else {
                        player.sendTitle(ChatColor.GREEN + "Liaison Réussie", ChatColor.GRAY + "Bon jeu sur CoreHost !", 10, 70, 20);
                        player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Votre compte a bien été lié à Discord !");
                    }
                    this.cancel();
                    actionbarTasks.remove(uuid);
                    return;
                }
                
                String message;
                if (is2FA) {
                    message = ChatColor.GOLD + "Vérification requise ! " + ChatColor.RED + "Envoyez " + ChatColor.YELLOW + expectedCode + ChatColor.RED + " au Bot Discord !";
                } else {
                    message = ChatColor.RED + "Envoyez le code " + ChatColor.YELLOW + expectedCode + ChatColor.RED + " en Message Privé au Bot Discord !";
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        actionbarTasks.put(uuid, task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        blockedPlayers.remove(uuid);
        isPending2FA.remove(uuid);
        if (actionbarTasks.containsKey(uuid)) {
            actionbarTasks.get(uuid).cancel();
            actionbarTasks.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (blockedPlayers.containsKey(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ() || event.getFrom().getY() != event.getTo().getY()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (blockedPlayers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (blockedPlayers.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (blockedPlayers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (blockedPlayers.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (blockedPlayers.containsKey(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (blockedPlayers.containsKey(uuid)) {
            event.setCancelled(true);
            boolean is2FA = isPending2FA.getOrDefault(uuid, false);
            if (is2FA) {
                event.getPlayer().sendMessage(Constants.PREFIX + ChatColor.RED + "Vous devez autoriser la connexion via Discord ! PIN: " + ChatColor.YELLOW + blockedPlayers.get(uuid));
            } else {
                event.getPlayer().sendMessage(Constants.PREFIX + ChatColor.RED + "Vous devez lier votre compte Discord pour parler ! Code: " + ChatColor.YELLOW + blockedPlayers.get(uuid));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (blockedPlayers.containsKey(uuid)) {
            event.setCancelled(true);
            boolean is2FA = isPending2FA.getOrDefault(uuid, false);
            if (is2FA) {
                event.getPlayer().sendMessage(Constants.PREFIX + ChatColor.RED + "Vous devez autoriser la connexion via Discord ! PIN: " + ChatColor.YELLOW + blockedPlayers.get(uuid));
            } else {
                event.getPlayer().sendMessage(Constants.PREFIX + ChatColor.RED + "Vous devez lier votre compte Discord pour faire des commandes ! Code: " + ChatColor.YELLOW + blockedPlayers.get(uuid));
            }
        }
    }
}
