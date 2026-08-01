package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SumoSettingsMenu implements CustomMenu {

    private final Inventory inventory;
    private final CoreHostLobby plugin;

    public SumoSettingsMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Format Sumo");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) {
                inventory.setItem(i, filler);
            }
        }

        // BO3 Item
        ItemStack bo3 = new ItemStack(Material.SLIME_BALL);
        bo3.setAmount(3);
        ItemMeta bo3Meta = bo3.getItemMeta();
        if (bo3Meta != null) {
            bo3Meta.setDisplayName(ChatColor.YELLOW + "Best Of 3");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Le premier arrivé à 2 victoires");
            lore.add(ChatColor.GRAY + "gagne la partie.");
            lore.add("");
            lore.add(ChatColor.GREEN + "► Cliquez pour héberger en BO3");
            bo3Meta.setLore(lore);
            bo3.setItemMeta(bo3Meta);
        }
        inventory.setItem(11, bo3);

        // BO5 Item
        ItemStack bo5 = new ItemStack(Material.SLIME_BALL);
        bo5.setAmount(5);
        ItemMeta bo5Meta = bo5.getItemMeta();
        if (bo5Meta != null) {
            bo5Meta.setDisplayName(ChatColor.YELLOW + "Best Of 5");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Le premier arrivé à 3 victoires");
            lore.add(ChatColor.GRAY + "gagne la partie.");
            lore.add("");
            lore.add(ChatColor.GREEN + "► Cliquez pour héberger en BO5");
            bo5Meta.setLore(lore);
            bo5.setItemMeta(bo5Meta);
        }
        inventory.setItem(13, bo5);

        // BO7 Item
        ItemStack bo7 = new ItemStack(Material.SLIME_BALL);
        bo7.setAmount(7);
        ItemMeta bo7Meta = bo7.getItemMeta();
        if (bo7Meta != null) {
            bo7Meta.setDisplayName(ChatColor.YELLOW + "Best Of 7");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Le premier arrivé à 4 victoires");
            lore.add(ChatColor.GRAY + "gagne la partie.");
            lore.add("");
            lore.add(ChatColor.GREEN + "► Cliquez pour héberger en BO7");
            bo7Meta.setLore(lore);
            bo7.setItemMeta(bo7Meta);
        }
        inventory.setItem(15, bo7);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "◄ Retour");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(22, back);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getSlot();

        if (slot == 22) { // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new HostCreateMenu().open(player);
            return;
        }

        int bestOf = 3; // Default BO3
        if (slot == 11) bestOf = 3; // BO3
        if (slot == 13) bestOf = 5; // BO5
        if (slot == 15) bestOf = 7; // BO7

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        player.closeInventory();
        plugin.getCloudNetServiceManager().createHost(player, "Sumo", bestOf);
    }
}
