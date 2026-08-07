package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModManager {

    private final StaffModPlugin plugin;
    private final Set<UUID> modPlayers = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    public ModManager(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isModMode(UUID uuid) {
        return modPlayers.contains(uuid);
    }

    public void setModMode(Player player, boolean mod) {
        setModMode(player, mod, true);
    }

    public void setModMode(Player player, boolean mod, boolean notify) {
        UUID uuid = player.getUniqueId();
        if (mod) {
            if (isModMode(uuid)) {
                if (notify && player.isOnline()) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà activé !", NamedTextColor.RED)));
                }
                return;
            }
            modPlayers.add(uuid);
            player.setMetadata("modmode", new FixedMetadataValue(plugin, true));
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "true", 86400);
            }

            // Annuler le parkour si en cours dans le Lobby
            org.bukkit.plugin.Plugin lobbyPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CoreHost-Lobby");
            if (lobbyPlugin != null && lobbyPlugin.isEnabled()) {
                try {
                    Object parkourMgr = lobbyPlugin.getClass().getMethod("getParkourManager").invoke(lobbyPlugin);
                    if (parkourMgr != null) {
                        parkourMgr.getClass().getMethod("cancelParkour", Player.class).invoke(parkourMgr, player);
                    }
                } catch (Exception ignored) {}
            }
            
            // Sortir le joueur d'une instance Sumo s'il est en jeu
            org.bukkit.plugin.Plugin sumoPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CoreHost-Sumo");
            if (sumoPlugin != null && sumoPlugin.isEnabled()) {
                try {
                    Object gameMgr = sumoPlugin.getClass().getMethod("getGameManager").invoke(sumoPlugin);
                    if (gameMgr != null) {
                        java.util.Optional<?> optInstance = (java.util.Optional<?>) gameMgr.getClass().getMethod("getInstanceForPlayer", Player.class).invoke(gameMgr, player);
                        if (optInstance.isPresent()) {
                            Object instance = optInstance.get();
                            instance.getClass().getMethod("removePlayer", Player.class).invoke(instance, player);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Save Inventory
            savedInventories.put(uuid, player.getInventory().getContents());
            savedArmor.put(uuid, player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            
            // Give Mod Items
            giveModItems(player);

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(0.2f);
            
            if (notify && player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Mode Modération activé !", NamedTextColor.GREEN)));
            }
        } else {
            if (!isModMode(uuid)) {
                if (notify && player.isOnline()) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà désactivé !", NamedTextColor.RED)));
                }
                return;
            }
            modPlayers.remove(uuid);
            player.removeMetadata("modmode", plugin);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "false", 86400);
            }
            
            // Sortir le joueur d'une instance Sumo s'il est en jeu
            org.bukkit.plugin.Plugin sumoPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CoreHost-Sumo");
            if (sumoPlugin != null && sumoPlugin.isEnabled()) {
                try {
                    Object gameMgr = sumoPlugin.getClass().getMethod("getGameManager").invoke(sumoPlugin);
                    if (gameMgr != null) {
                        java.util.Optional<?> optInstance = (java.util.Optional<?>) gameMgr.getClass().getMethod("getInstanceForPlayer", Player.class).invoke(gameMgr, player);
                        if (optInstance.isPresent()) {
                            Object instance = optInstance.get();
                            instance.getClass().getMethod("removePlayer", Player.class).invoke(instance, player);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Restore Inventory
            player.getInventory().clear();
            if (savedInventories.containsKey(uuid)) {
                player.getInventory().setContents(savedInventories.get(uuid));
                savedInventories.remove(uuid);
            }
            if (savedArmor.containsKey(uuid)) {
                player.getInventory().setArmorContents(savedArmor.get(uuid));
                savedArmor.remove(uuid);
            }

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);

            // Vérifier si nous sommes sur le serveur Lobby
            org.bukkit.plugin.Plugin lobbyPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CoreHost-Lobby");
            if (lobbyPlugin != null && lobbyPlugin.isEnabled()) {
                // Give Lobby Items if in Lobby world
                giveLobbyItems(player);

                // Teleport to spawn location
                Location spawn = player.getWorld().getSpawnLocation().clone();
                spawn.setX(spawn.getBlockX() + 0.5);
                spawn.setZ(spawn.getBlockZ() + 0.5);
                spawn.setYaw(spawn.getYaw() + 180f);
                player.teleport(spawn);
            } else {
                // Envoyer au lobby via BungeeCord
                @SuppressWarnings("UnstableApiUsage")
                com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF("lobby");
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
            
            if (notify && player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Mode Modération désactivé !", NamedTextColor.RED)));
            }
        }
    }

    private void giveLobbyItems(Player player) {
        Inventory inv = player.getInventory();

        // Slot 4: Jouer (Compass)
        ItemStack searchHost = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchHost.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("Jouer ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            searchHost.setItemMeta(searchMeta);
        }
        inv.setItem(4, searchHost);

        // Slot 7: Visibility (Lime Dye)
        ItemStack visibility = new ItemStack(Material.LIME_DYE);
        ItemMeta visMeta = visibility.getItemMeta();
        if (visMeta != null) {
            visMeta.displayName(Component.text("Joueurs : Visibles ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            visibility.setItemMeta(visMeta);
        }
        inv.setItem(7, visibility);

        // Slot 8: Profile (Player Head)
        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.displayName(Component.text("Mon Profil ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            profile.setItemMeta(profileMeta);
        }
        inv.setItem(8, profile);
    }

    private void giveModItems(Player player) {
        Inventory inv = player.getInventory();
        
        // Epée KB 1
        ItemStack kb1 = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta kb1Meta = kb1.getItemMeta();
        if (kb1Meta != null) {
            kb1Meta.displayName(Component.text("Knockback I", NamedTextColor.RED, TextDecoration.BOLD));
            kb1Meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            kb1Meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Repousser légèrement un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic pour frapper", NamedTextColor.YELLOW)
            ));
            kb1.setItemMeta(kb1Meta);
        }
        inv.setItem(0, kb1);
        
        // Epée KB 2
        ItemStack kb2 = new ItemStack(Material.STONE_SWORD);
        ItemMeta kb2Meta = kb2.getItemMeta();
        if (kb2Meta != null) {
            kb2Meta.displayName(Component.text("Knockback II", NamedTextColor.RED, TextDecoration.BOLD));
            kb2Meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            kb2Meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Éjecter un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic pour frapper", NamedTextColor.YELLOW)
            ));
            kb2.setItemMeta(kb2Meta);
        }
        inv.setItem(1, kb2);
        
        // Boussole
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.displayName(Component.text("Téléportation Aléatoire", NamedTextColor.AQUA, TextDecoration.BOLD));
            compassMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Se téléporter sur un joueur aléatoire", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit pour exécuter", NamedTextColor.YELLOW)
            ));
            compass.setItemMeta(compassMeta);
        }
        inv.setItem(2, compass);
        
        // Glace (Freeze)
        ItemStack freeze = new ItemStack(Material.PACKED_ICE);
        ItemMeta freezeMeta = freeze.getItemMeta();
        if (freezeMeta != null) {
            freezeMeta.displayName(Component.text("Geler un Joueur", NamedTextColor.AQUA, TextDecoration.BOLD));
            freezeMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Immobiliser ou libérer un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit sur un joueur", NamedTextColor.YELLOW)
            ));
            freeze.setItemMeta(freezeMeta);
        }
        inv.setItem(3, freeze);
        
        // Livre (SS)
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.displayName(Component.text("Inspecter (SS)", NamedTextColor.GOLD, TextDecoration.BOLD));
            bookMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Ouvrir le menu de modération d'un joueur", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit sur un joueur", NamedTextColor.YELLOW)
            ));
            book.setItemMeta(bookMeta);
        }
        inv.setItem(4, book);
        
        // Vanish
        boolean isVanished = plugin.getVanishManager().isVanished(player.getUniqueId());
        ItemStack vanish = new ItemStack(isVanished ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta vanishMeta = vanish.getItemMeta();
        if (vanishMeta != null) {
            vanishMeta.displayName(Component.text("Vanish : " + (isVanished ? "ON" : "OFF"), isVanished ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD));
            vanishMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Activer ou désactiver l'invisibilité", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit pour basculer", NamedTextColor.YELLOW)
            ));
            vanish.setItemMeta(vanishMeta);
        }
        inv.setItem(7, vanish);
        
        // Quitter
        ItemStack leave = new ItemStack(Material.RED_BED);
        ItemMeta leaveMeta = leave.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.displayName(Component.text("Quitter le Mode Modération", NamedTextColor.RED, TextDecoration.BOLD));
            leaveMeta.lore(Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Désactiver le mode modération", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit pour quitter", NamedTextColor.YELLOW)
            ));
            leave.setItemMeta(leaveMeta);
        }
        inv.setItem(8, leave);
    }

    public void handleJoin(Player player) {
        if (plugin.getRedisManager() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (player.hasPermission("staffmod.mod")) {
                    plugin.getRedisManager().setEx("corehost:modmode:" + player.getUniqueId().toString(), "false", 86400);
                }
                
                // Handle pending TP asynchronously
                String pendingTp = plugin.getRedisManager().get("corehost:pending_tp:" + player.getUniqueId().toString());
                if (pendingTp != null) {
                    plugin.getRedisManager().del("corehost:pending_tp:" + player.getUniqueId().toString());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            Player target = Bukkit.getPlayerExact(pendingTp);
                            if (target != null && target.isOnline()) {
                                player.teleport(target.getLocation());
                                player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté à " + target.getName(), NamedTextColor.GREEN)));
                            } else {
                                player.sendMessage(plugin.getPrefix().append(Component.text("Le joueur s'est déconnecté entre-temps.", NamedTextColor.RED)));
                            }
                        }
                    }, 10L); // 10 ticks = 0.5 seconds
                }
            });
        }
        
        if (player.hasPermission("staffmod.mod") && isModMode(player.getUniqueId())) {
            setModMode(player, false, false);
        }
    }

    public void handleQuit(Player player) {
        if (isModMode(player.getUniqueId())) {
            setModMode(player, false, false);
        } else {
            savedInventories.remove(player.getUniqueId());
            savedArmor.remove(player.getUniqueId());
        }
    }
}

