package fr.corehost.game.spectator;

import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import fr.corehost.game.CoreHostGame;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpectatorManager {

    private final CoreHostGame plugin;
    private final Set<UUID> spectators = new HashSet<>();

    public SpectatorManager(CoreHostGame plugin) {
        this.plugin = plugin;
    }

    public void setSpectator(Player player, boolean isSpectator) {
        if (isSpectator) {
            spectators.add(player.getUniqueId());
            
            // Apply Adventure Mode & flight
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(0.1f);
            
            // Reset state
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setFireTicks(0);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.setCollidable(false);

            // Hide from normal players, but we can do that generally or rely on IsolationListener
            hideSpectator(player);

            // Give Items
            setupInventory(player);
            
            player.sendMessage(CC.GRAY + "Vous êtes maintenant en " + CC.AQUA + "Mode Spectateur" + CC.GRAY + ".");
        } else {
            spectators.remove(player.getUniqueId());
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);
            player.setCollidable(true);
            
            showSpectator(player);
            
            player.getInventory().clear();
        }
    }
    
    private void hideSpectator(Player spectator) {

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(spectator)) continue;
            
            // Only hide from people who are NOT spectators
            if (!isSpectator(online)) {
                online.showPlayer(plugin, spectator);
                online.hideEntity(plugin, spectator);
            } else {
                // Spectators can see other spectators
                online.showPlayer(plugin, spectator);
                online.showEntity(plugin, spectator);
                spectator.showPlayer(plugin, online);
                spectator.showEntity(plugin, online);
            }
        }
    }
    
    private void showSpectator(Player formerSpectator) {
        String worldName = formerSpectator.getWorld().getName();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(formerSpectator)) continue;
            
            if (online.getWorld().getName().equals(worldName)) {
                if (formerSpectator.hasMetadata("vanished") && formerSpectator.getMetadata("vanished").get(0).asBoolean()) {
                    online.hidePlayer(plugin, formerSpectator);
                } else {
                    online.showPlayer(plugin, formerSpectator);
                    online.showEntity(plugin, formerSpectator);
                }
                
                if (online.hasMetadata("vanished") && online.getMetadata("vanished").get(0).asBoolean()) {
                    formerSpectator.hidePlayer(plugin, online);
                } else {
                    formerSpectator.showPlayer(plugin, online);
                    formerSpectator.showEntity(plugin, online);
                }
            }
        }
    }

    private void setupInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        // Compass (Teleport)
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName(CC.AQUA + "Téléportation");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(0, compass);
        
        // Feather (Speed)
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherMeta = feather.getItemMeta();
        if (featherMeta != null) {
            featherMeta.setDisplayName(CC.YELLOW + "Vitesse de vol: x1");
            feather.setItemMeta(featherMeta);
        }
        player.getInventory().setItem(4, feather);
        
        // Bed (Leave)
        ItemStack bed = new ItemStack(Material.RED_BED);
        ItemMeta bedMeta = bed.getItemMeta();
        if (bedMeta != null) {
            bedMeta.setDisplayName(CC.RED + "Quitter vers le Lobby");
            bed.setItemMeta(bedMeta);
        }
        player.getInventory().setItem(8, bed);
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }
}
