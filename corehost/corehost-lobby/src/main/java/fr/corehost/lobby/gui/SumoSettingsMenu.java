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

public class SumoSettingsMenu implements CustomMenu {

    private final Inventory inventory;
    private final CoreHostLobby plugin;
    private boolean doubleJumpEnabled = false;
    private boolean customKBEnabled = false;

    public SumoSettingsMenu(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, CC.DARK_GRAY + "» " + CC.GOLD + "Format Sumo");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration (unified Pink + Purple)
        LobbyMenuUtils.fillBorder(inventory);

        updateDoubleJumpItem();
        updateCustomKBItem();

        // BO3 Item
        ItemStack bo3 = new ItemBuilder(Material.SLIME_BALL, 3)
            .setName(CC.YELLOW + "" + CC.BOLD + "Best Of 3")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Victoires nécessaires : " + CC.WHITE + "2",
                "",
                CC.GRAY + "Le premier arrivé à 2 victoires",
                CC.GRAY + "gagne la partie.",
                "",
                CC.GREEN + "► Cliquez pour héberger en BO3"
            ).build();
        inventory.setItem(11, bo3);

        // BO5 Item
        ItemStack bo5 = new ItemBuilder(Material.SLIME_BALL, 5)
            .setName(CC.YELLOW + "" + CC.BOLD + "Best Of 5")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Victoires nécessaires : " + CC.WHITE + "3",
                "",
                CC.GRAY + "Le premier arrivé à 3 victoires",
                CC.GRAY + "gagne la partie.",
                "",
                CC.GREEN + "► Cliquez pour héberger en BO5"
            ).build();
        inventory.setItem(13, bo5);

        // BO7 Item
        ItemStack bo7 = new ItemBuilder(Material.SLIME_BALL, 7)
            .setName(CC.YELLOW + "" + CC.BOLD + "Best Of 7")
            .setLore(
                "",
                CC.DARK_GRAY + "▪ " + CC.GRAY + "Victoires nécessaires : " + CC.WHITE + "4",
                "",
                CC.GRAY + "Le premier arrivé à 4 victoires",
                CC.GRAY + "gagne la partie.",
                "",
                CC.GREEN + "► Cliquez pour héberger en BO7"
            ).build();
        inventory.setItem(15, bo7);

        // Back button (unified)
        inventory.setItem(22, LobbyMenuUtils.getBackToCreateButton());
    }

    private void updateDoubleJumpItem() {
        ItemStack doubleJumpItem;
        if (doubleJumpEnabled) {
            doubleJumpItem = new ItemBuilder(Material.FEATHER)
                .setName(CC.AQUA + "" + CC.BOLD + "Double Saut : " + CC.GREEN + "Activé")
                .setLore(
                    "",
                    CC.GRAY + "Permet un double saut par manche.",
                    "",
                    CC.YELLOW + "► Cliquez pour " + CC.RED + "désactiver"
                ).build();
        } else {
            doubleJumpItem = new ItemBuilder(Material.FEATHER)
                .setName(CC.AQUA + "" + CC.BOLD + "Double Saut : " + CC.RED + "Désactivé")
                .setLore(
                    "",
                    CC.GRAY + "Permet un double saut par manche.",
                    "",
                    CC.YELLOW + "► Cliquez pour " + CC.GREEN + "activer"
                ).build();
        }
        inventory.setItem(4, doubleJumpItem);
    }

    private void updateCustomKBItem() {
        ItemStack customKBItem;
        if (customKBEnabled) {
            customKBItem = new ItemBuilder(Material.ANVIL)
                .setName(CC.AQUA + "" + CC.BOLD + "Knockback : " + CC.GREEN + "Custom")
                .setLore(
                    "",
                    CC.GRAY + "Utilise un recul customisé et",
                    CC.GRAY + "optimisé pour le Sumo.",
                    "",
                    CC.YELLOW + "► Cliquez pour repasser en " + CC.RED + "Vanilla"
                ).build();
        } else {
            customKBItem = new ItemBuilder(Material.ANVIL)
                .setName(CC.AQUA + "" + CC.BOLD + "Knockback : " + CC.RED + "Vanilla")
                .setLore(
                    "",
                    CC.GRAY + "Utilise le recul par défaut",
                    CC.GRAY + "de Minecraft.",
                    "",
                    CC.YELLOW + "► Cliquez pour passer en " + CC.GREEN + "Custom"
                ).build();
        }
        inventory.setItem(2, customKBItem); // Placé avant le Double Jump (Slot 2)
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

        if (slot == 4) {
            doubleJumpEnabled = !doubleJumpEnabled;
            updateDoubleJumpItem();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (slot == 2) {
            customKBEnabled = !customKBEnabled;
            updateCustomKBItem();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        int bestOf = 3; // Default BO3
        if (slot == 11) bestOf = 3; // BO3
        if (slot == 13) bestOf = 5; // BO5
        if (slot == 15) bestOf = 7; // BO7

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        player.closeInventory();
        plugin.getCloudNetServiceManager().createHost(player, "Sumo", bestOf, doubleJumpEnabled, customKBEnabled);
    }
}
