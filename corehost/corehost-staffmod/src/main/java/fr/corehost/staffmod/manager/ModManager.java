package fr.corehost.staffmod.manager;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModManager {

    private final StaffModPlugin plugin;
    private final Set<UUID> modPlayers = new HashSet<>();

    public ModManager(StaffModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isModMode(UUID uuid) {
        return modPlayers.contains(uuid);
    }

    private final java.util.Map<UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new java.util.HashMap<>();
    private final java.util.Map<UUID, org.bukkit.inventory.ItemStack[]> savedArmor = new java.util.HashMap<>();

    public void setModMode(Player player, boolean mod) {
        UUID uuid = player.getUniqueId();
        if (mod) {
            if (isModMode(uuid)) {
                if (player.isOnline()) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà activé !", NamedTextColor.RED)));
                }
                return;
            }
            modPlayers.add(uuid);
            player.setMetadata("modmode", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "true", 86400);
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
            
            player.sendMessage(plugin.getPrefix().append(Component.text("Mode Modération activé !", NamedTextColor.GREEN)));
        } else {
            if (!isModMode(uuid)) {
                if (player.isOnline()) {
                    player.sendMessage(plugin.getPrefix().append(Component.text("Le mode Modération est déjà désactivé !", NamedTextColor.RED)));
                }
                return;
            }
            modPlayers.remove(uuid);
            player.removeMetadata("modmode", plugin);
            if (plugin.getRedisManager() != null) {
                plugin.getRedisManager().setEx("corehost:modmode:" + uuid.toString(), "false", 86400);
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

            // Give Lobby Items
            giveLobbyItems(player);

            // Teleport to Lobby spawn
            org.bukkit.Location spawn = player.getWorld().getSpawnLocation().clone();
            spawn.setX(spawn.getBlockX() + 0.5);
            spawn.setZ(spawn.getBlockZ() + 0.5);
            spawn.setYaw(spawn.getYaw() + 180f);
            player.teleport(spawn);
            
            if (player.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(Component.text("Mode Modération désactivé !", NamedTextColor.RED)));
            }
        }
    }

    private void giveLobbyItems(Player player) {
        org.bukkit.inventory.Inventory inv = player.getInventory();

        // Slot 4: Jouer (Compass)
        org.bukkit.inventory.ItemStack searchHost = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta searchMeta = searchHost.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("Jouer ", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            searchHost.setItemMeta(searchMeta);
        }
        inv.setItem(4, searchHost);

        // Slot 7: Visibility (Lime Dye)
        org.bukkit.inventory.ItemStack visibility = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIME_DYE);
        org.bukkit.inventory.meta.ItemMeta visMeta = visibility.getItemMeta();
        if (visMeta != null) {
            visMeta.displayName(Component.text("Joueurs : Visibles ", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            visibility.setItemMeta(visMeta);
        }
        inv.setItem(7, visibility);

        // Slot 8: Profile (Player Head)
        org.bukkit.inventory.ItemStack profile = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta profileMeta = (org.bukkit.inventory.meta.SkullMeta) profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.displayName(Component.text("Mon Profil ", NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text("(Clic-Droit)", NamedTextColor.GRAY)));
            profile.setItemMeta(profileMeta);
        }
        inv.setItem(8, profile);
    }

    private void giveModItems(Player player) {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        
        // Epée KB 1
        org.bukkit.inventory.ItemStack kb1 = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_SWORD);
        org.bukkit.inventory.meta.ItemMeta kb1Meta = kb1.getItemMeta();
        if (kb1Meta != null) {
            kb1Meta.displayName(Component.text("Knockback I", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            kb1Meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1, true);
            kb1Meta.lore(java.util.Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Repousser légèrement un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic pour frapper", NamedTextColor.YELLOW)
            ));
            kb1.setItemMeta(kb1Meta);
        }
        inv.setItem(0, kb1);
        
        // Epée KB 2
        org.bukkit.inventory.ItemStack kb2 = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD);
        org.bukkit.inventory.meta.ItemMeta kb2Meta = kb2.getItemMeta();
        if (kb2Meta != null) {
            kb2Meta.displayName(Component.text("Knockback II", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            kb2Meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 2, true);
            kb2Meta.lore(java.util.Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Éjecter un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic pour frapper", NamedTextColor.YELLOW)
            ));
            kb2.setItemMeta(kb2Meta);
        }
        inv.setItem(1, kb2);
        
        // Boussole
        org.bukkit.inventory.ItemStack compass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.displayName(Component.text("Téléportation Aléatoire", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD));
            compassMeta.lore(java.util.Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Se téléporter sur un joueur aléatoire", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit pour exécuter", NamedTextColor.YELLOW)
            ));
            compass.setItemMeta(compassMeta);
        }
        inv.setItem(2, compass);
        
        // Glace (Freeze)
        org.bukkit.inventory.ItemStack freeze = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PACKED_ICE);
        org.bukkit.inventory.meta.ItemMeta freezeMeta = freeze.getItemMeta();
        if (freezeMeta != null) {
            freezeMeta.displayName(Component.text("Geler un Joueur", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD));
            freezeMeta.lore(java.util.Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Immobiliser ou libérer un suspect", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit sur un joueur", NamedTextColor.YELLOW)
            ));
            freeze.setItemMeta(freezeMeta);
        }
        inv.setItem(3, freeze);
        
        // Livre (SS)
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.displayName(Component.text("Inspecter (SS)", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            bookMeta.lore(java.util.Arrays.asList(
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
        org.bukkit.inventory.ItemStack vanish = new org.bukkit.inventory.ItemStack(isVanished ? org.bukkit.Material.LIME_DYE : org.bukkit.Material.GRAY_DYE);
        org.bukkit.inventory.meta.ItemMeta vanishMeta = vanish.getItemMeta();
        if (vanishMeta != null) {
            vanishMeta.displayName(Component.text("Vanish : " + (isVanished ? "ON" : "OFF"), isVanished ? NamedTextColor.GREEN : NamedTextColor.GRAY, net.kyori.adventure.text.format.TextDecoration.BOLD));
            vanishMeta.lore(java.util.Arrays.asList(
                Component.empty(),
                Component.text("▪ ", NamedTextColor.DARK_GRAY).append(Component.text("Activer ou désactiver l'invisibilité", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("► Clic Droit pour basculer", NamedTextColor.YELLOW)
            ));
            vanish.setItemMeta(vanishMeta);
        }
        inv.setItem(7, vanish);
        
        // Quitter
        org.bukkit.inventory.ItemStack leave = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_BED);
        org.bukkit.inventory.meta.ItemMeta leaveMeta = leave.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.displayName(Component.text("Quitter le Mode Modération", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            leaveMeta.lore(java.util.Arrays.asList(
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
        if (player.hasPermission("staffmod.mod")) {
            boolean shouldMod = false;
            if (plugin.getRedisManager() != null) {
                String isM = plugin.getRedisManager().get("corehost:modmode:" + player.getUniqueId().toString());
                if ("true".equals(isM)) {
                    shouldMod = true;
                }
                
                // Handle pending TP
                String pendingTp = plugin.getRedisManager().get("corehost:pending_tp:" + player.getUniqueId().toString());
                if (pendingTp != null) {
                    plugin.getRedisManager().del("corehost:pending_tp:" + player.getUniqueId().toString());
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Player target = org.bukkit.Bukkit.getPlayerExact(pendingTp);
                        if (target != null && target.isOnline()) {
                            player.teleport(target.getLocation());
                            player.sendMessage(plugin.getPrefix().append(Component.text("Téléporté à " + target.getName(), NamedTextColor.GREEN)));
                        } else {
                            player.sendMessage(plugin.getPrefix().append(Component.text("Le joueur s'est déconnecté entre-temps.", NamedTextColor.RED)));
                        }
                    }, 10L); // 10 ticks = 0.5 seconds
                }
            }
            if (shouldMod) {
                setModMode(player, true);
            }
        }
    }

    public void handleQuit(Player player) {
        if (isModMode(player.getUniqueId())) {
            setModMode(player, false);
        }
    }
}
