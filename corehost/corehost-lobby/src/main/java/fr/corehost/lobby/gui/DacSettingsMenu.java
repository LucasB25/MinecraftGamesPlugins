package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.ItemBuilder;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DacSettingsMenu implements CustomMenu {

    private final Inventory inventory;
    private final CoreHostLobby plugin;

    public DacSettingsMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, CC.DARK_GRAY + "» " + CC.GOLD + "Format DAC");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration
        LobbyMenuUtils.fillBorder(inventory);

        // 0 Vie Item (1 attempt)
        ItemStack zeroLife = new ItemBuilder(Material.RED_DYE)
            .setName(CC.YELLOW + "" + CC.BOLD + "Mort Subite (0 Vie)")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Nombre de vies : " + CC.WHITE + "0",
                "",
                CC.GRAY + "Une seule erreur",
                CC.GRAY + "et c'est éliminé !",
                "",
                CC.GREEN + "► Cliquez pour héberger avec 0 vie"
            ).build();
        inventory.setItem(10, zeroLife);

        // 1 Vie Item (2 attempts)
        ItemStack oneLife = new ItemBuilder(Material.ORANGE_DYE)
            .setName(CC.YELLOW + "" + CC.BOLD + "Classique (1 Vie)")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Nombre de vies : " + CC.WHITE + "1",
                "",
                CC.GRAY + "Vous avez droit à une",
                CC.GRAY + "seconde chance.",
                "",
                CC.GREEN + "► Cliquez pour héberger avec 1 vie"
            ).build();
        inventory.setItem(12, oneLife);

        // 2 Vies Item (3 attempts)
        ItemStack twoLives = new ItemBuilder(Material.LIME_DYE)
            .setName(CC.YELLOW + "" + CC.BOLD + "Long (2 Vies)")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Nombre de vies : " + CC.WHITE + "2",
                "",
                CC.GRAY + "Idéal pour apprendre ou",
                CC.GRAY + "pour de plus longues parties.",
                "",
                CC.GREEN + "► Cliquez pour héberger avec 2 vies"
            ).build();
        inventory.setItem(14, twoLives);

        // 3 Vies Item (4 attempts)
        ItemStack threeLives = new ItemBuilder(Material.GREEN_DYE)
            .setName(CC.YELLOW + "" + CC.BOLD + "Très Long (3 Vies)")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Nombre de vies : " + CC.WHITE + "3",
                "",
                CC.GRAY + "Partie très longue,",
                CC.GRAY + "pour les plus persévérants !",
                "",
                CC.GREEN + "► Cliquez pour héberger avec 3 vies"
            ).build();
        inventory.setItem(16, threeLives);

        // Back button
        inventory.setItem(22, LobbyMenuUtils.getBackToCreateButton());
    }

    public void open(Player player) {
        if (player.hasMetadata("modmode")) {
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + CC.RED + "Vous ne pouvez pas créer un host en mode Modération !");
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        if (player.hasMetadata("modmode")) {
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + CC.RED + "Vous ne pouvez pas créer un host en mode Modération !");
            player.closeInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getSlot();

        if (slot == 22) { // Back
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new HostCreateMenu().open(player);
            return;
        }

        int initialLives = 1; // Default
        if (slot == 10) initialLives = 1; // 0 Extra life
        if (slot == 12) initialLives = 2; // 1 Extra life
        if (slot == 14) initialLives = 3; // 2 Extra lives
        if (slot == 16) initialLives = 4; // 3 Extra lives

        // If a slot other than 10, 12, 14, 16 is clicked, ignore
        if (slot != 10 && slot != 12 && slot != 14 && slot != 16) return;

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        player.closeInventory();
        // createHost for DAC, bestOf=3(unused), doubleJump=false, customKB=false
        plugin.getCloudNetServiceManager().createHost(player, "DAC", 3, false, false, initialLives);
    }
}
