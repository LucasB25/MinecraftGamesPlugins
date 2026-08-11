package fr.corehost.game.spectator.gui;

import fr.corehost.game.spectator.SpectatorManager;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class SpectatorTeleportMenu {

    private final SpectatorManager manager;
    private static final String MENU_TITLE = CC.AQUA + "Téléportation";

    public SpectatorTeleportMenu(SpectatorManager manager) {
        this.manager = manager;
    }

    public void openMenu(Player spectator) {
        List<Player> alivePlayers = new ArrayList<>();
        String worldName = spectator.getWorld().getName();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(worldName) && !manager.isSpectator(p) && !p.equals(spectator)) {
                alivePlayers.add(p);
            }
        }

        int size = ((alivePlayers.size() / 9) + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54; // Max inventory size

        Inventory inv = Bukkit.createInventory(null, size, MENU_TITLE);

        for (Player target : alivePlayers) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target); // For 1.12+ it's setOwningPlayer
                meta.setDisplayName(CC.GREEN + target.getName());
                List<String> lore = new ArrayList<>();
                lore.add(CC.GRAY + "Cliquez pour vous téléporter à " + CC.YELLOW + target.getName());
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }

        spectator.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;

        event.setCancelled(true);
        Player spectator = (Player) event.getWhoClicked();

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = meta.getOwningPlayer().getPlayer();
                if (target != null && target.isOnline() && !manager.isSpectator(target)) {
                    spectator.teleport(target.getLocation());
                    spectator.sendMessage(CC.GRAY + "Téléporté vers " + CC.GREEN + target.getName() + CC.GRAY + ".");
                    spectator.closeInventory();
                } else {
                    spectator.sendMessage(CC.RED + "Ce joueur n'est plus disponible.");
                    spectator.closeInventory();
                }
            }
        }
    }
}
