package fr.corehost.lobby.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface CustomMenu extends InventoryHolder {
    /**
     * Called when a player clicks an item inside the custom menu.
     * 
     * @param event The InventoryClickEvent
     * @param player The player who clicked
     */
    void onClick(InventoryClickEvent event, Player player);
}
