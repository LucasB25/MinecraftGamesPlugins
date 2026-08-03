package fr.corehost.lobby.headhunt;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class HeadHuntListener implements Listener {

    private final CoreHostLobby plugin;

    public HeadHuntListener(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!event.getAction().name().contains("RIGHT")) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
            Player player = event.getPlayer();
            
            // Allow admins in build mode to bypass (so they can break/place heads)
            if (fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(player.getUniqueId())) {
                return;
            }

            if (plugin.getHeadHuntManager() != null) {
                if (plugin.getHeadHuntManager().isHeadHuntBlock(block.getLocation())) {
                    plugin.getHeadHuntManager().clickHead(player, block.getLocation());
                    event.setCancelled(true);
                }
            }
        }
    }
}
