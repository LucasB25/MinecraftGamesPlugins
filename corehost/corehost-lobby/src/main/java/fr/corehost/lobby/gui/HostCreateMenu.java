package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import fr.corehost.lobby.CoreHostLobby;

public class HostCreateMenu implements CustomMenu {

    private final Inventory inventory;

    public HostCreateMenu() {
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Création de Host");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) {
                inventory.setItem(i, filler);
            }
        }

        // Sumo Item
        ItemStack sumoItem = new ItemStack(Material.SLIME_BALL);
        ItemMeta sumoMeta = sumoItem.getItemMeta();
        if (sumoMeta != null) {
            sumoMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Créer un Mini-Jeu (Sumo)");
            sumoMeta.setLore(java.util.Arrays.asList(
                "",
                ChatColor.GRAY + "Démarrez un serveur Sumo",
                ChatColor.GRAY + "pour expulser vos adversaires",
                ChatColor.GRAY + "de l'arène !",
                "",
                ChatColor.GREEN + "► Cliquez pour héberger"
            ));
            sumoItem.setItemMeta(sumoMeta);
        }
        inventory.setItem(11, sumoItem);

        // CTF Item
        ItemStack ctfItem = new ItemStack(Material.RED_BANNER);
        ItemMeta ctfMeta = ctfItem.getItemMeta();
        if (ctfMeta != null) {
            ctfMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Créer un Mini-Jeu (CTF)");
            ctfMeta.setLore(java.util.Arrays.asList(
                "",
                ChatColor.GRAY + "Capture the Flag !",
                ChatColor.GRAY + "Volez le drapeau adverse",
                ChatColor.GRAY + "pour gagner la partie.",
                "",
                ChatColor.GREEN + "► Cliquez pour héberger"
            ));
            ctfItem.setItemMeta(ctfMeta);
        }
        inventory.setItem(15, ctfItem);
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
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.SLIME_BALL) {
            player.closeInventory();
            JavaPlugin.getPlugin(CoreHostLobby.class).getCloudNetServiceManager().createHost(player, "Sumo");
        } else if (clicked.getType() == Material.RED_BANNER) {
            player.closeInventory();
            JavaPlugin.getPlugin(CoreHostLobby.class).getCloudNetServiceManager().createHost(player, "CTF");
        }
    }
}
