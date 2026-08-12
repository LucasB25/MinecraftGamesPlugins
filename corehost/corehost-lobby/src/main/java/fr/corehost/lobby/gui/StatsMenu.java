package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import fr.corehost.api.utils.CC;
import fr.corehost.api.profile.PlayerProfile;
import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.ItemBuilder;

public class StatsMenu implements CustomMenu {

    private Inventory inventory;
    private final CoreHostLobby plugin;
    private final Player player;

    public StatsMenu(CoreHostLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    private void initializeItems() {
        LobbyMenuUtils.fillBorder(inventory);

        if (plugin.getProfileManager() == null) return;
        
        PlayerProfile profile = plugin.getProfileManager().getCachedProfile(player.getUniqueId());
        if (profile == null) return;

        // Sumo Stats
        int sumoWins = profile.getStat("sumo", "wins");
        int sumoLosses = profile.getStat("sumo", "losses");
        int sumoGames = sumoWins + sumoLosses;
        double sumoRatio = sumoLosses == 0 ? sumoWins : Math.round((double) sumoWins / sumoLosses * 100.0) / 100.0;

        ItemStack sumoStats = new ItemBuilder(Material.SLIME_BALL)
            .setName(CC.GREEN + "" + CC.BOLD + "Statistiques Sumo")
            .setLore(
                "",
                CC.GRAY + "Parties Jouées: " + CC.YELLOW + sumoGames,
                CC.GRAY + "Victoires: " + CC.GREEN + sumoWins,
                CC.GRAY + "Défaites: " + CC.RED + sumoLosses,
                CC.GRAY + "Ratio V/D: " + CC.AQUA + sumoRatio,
                ""
            ).build();
        inventory.setItem(11, sumoStats);

        // DAC Stats
        int dacWins = profile.getStat("dac", "wins");
        int dacLosses = profile.getStat("dac", "losses");
        int dacGames = dacWins + dacLosses;
        double dacRatio = dacLosses == 0 ? dacWins : Math.round((double) dacWins / dacLosses * 100.0) / 100.0;

        ItemStack dacStats = new ItemBuilder(Material.WATER_BUCKET)
            .setName(CC.BLUE + "" + CC.BOLD + "Statistiques DAC")
            .setLore(
                "",
                CC.GRAY + "Parties Jouées: " + CC.YELLOW + dacGames,
                CC.GRAY + "Victoires: " + CC.GREEN + dacWins,
                CC.GRAY + "Défaites: " + CC.RED + dacLosses,
                CC.GRAY + "Ratio V/D: " + CC.AQUA + dacRatio,
                ""
            ).build();
        inventory.setItem(15, dacStats);

        // Back button
        ItemStack backButton = new ItemBuilder(Material.ARROW)
            .setName(CC.RED + "Retour")
            .setLore(CC.GRAY + "Retourner au profil")
            .build();
        inventory.setItem(22, backButton);
    }

    public void open() {
        this.inventory = Bukkit.createInventory(this, 27, "Statistiques de Jeu");
        initializeItems();
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event, Player p) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        if (clicked.getType() == Material.ARROW) {
            new PlayerProfileMenu(plugin, p).open(p);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
