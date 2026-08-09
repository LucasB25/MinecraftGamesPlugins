package fr.corehost.lobby.gui;

import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Format Sumo");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration (unified Pink + Purple)
        LobbyMenuUtils.fillBorder(inventory);

        updateDoubleJumpItem();
        updateCustomKBItem();

        // BO3 Item
        ItemStack bo3 = new ItemBuilder(Material.SLIME_BALL, 3)
            .setName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Best Of 3")
            .setLore(
                "",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Victoires nécessaires : " + ChatColor.WHITE + "2",
                "",
                ChatColor.GRAY + "Le premier arrivé à 2 victoires",
                ChatColor.GRAY + "gagne la partie.",
                "",
                ChatColor.GREEN + "► Cliquez pour héberger en BO3"
            ).build();
        inventory.setItem(11, bo3);

        // BO5 Item
        ItemStack bo5 = new ItemBuilder(Material.SLIME_BALL, 5)
            .setName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Best Of 5")
            .setLore(
                "",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Victoires nécessaires : " + ChatColor.WHITE + "3",
                "",
                ChatColor.GRAY + "Le premier arrivé à 3 victoires",
                ChatColor.GRAY + "gagne la partie.",
                "",
                ChatColor.GREEN + "► Cliquez pour héberger en BO5"
            ).build();
        inventory.setItem(13, bo5);

        // BO7 Item
        ItemStack bo7 = new ItemBuilder(Material.SLIME_BALL, 7)
            .setName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Best Of 7")
            .setLore(
                "",
                ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Victoires nécessaires : " + ChatColor.WHITE + "4",
                "",
                ChatColor.GRAY + "Le premier arrivé à 4 victoires",
                ChatColor.GRAY + "gagne la partie.",
                "",
                ChatColor.GREEN + "► Cliquez pour héberger en BO7"
            ).build();
        inventory.setItem(15, bo7);

        // Back button (unified)
        inventory.setItem(22, LobbyMenuUtils.getBackToCreateButton());
    }

    private void updateDoubleJumpItem() {
        ItemStack doubleJumpItem;
        if (doubleJumpEnabled) {
            doubleJumpItem = new ItemBuilder(Material.FEATHER)
                .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Double Saut : " + ChatColor.GREEN + "Activé")
                .setLore(
                    "",
                    ChatColor.GRAY + "Permet un double saut par manche.",
                    "",
                    ChatColor.YELLOW + "► Cliquez pour " + ChatColor.RED + "désactiver"
                ).build();
        } else {
            doubleJumpItem = new ItemBuilder(Material.FEATHER)
                .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Double Saut : " + ChatColor.RED + "Désactivé")
                .setLore(
                    "",
                    ChatColor.GRAY + "Permet un double saut par manche.",
                    "",
                    ChatColor.YELLOW + "► Cliquez pour " + ChatColor.GREEN + "activer"
                ).build();
        }
        inventory.setItem(4, doubleJumpItem);
    }

    private void updateCustomKBItem() {
        ItemStack customKBItem;
        if (customKBEnabled) {
            customKBItem = new ItemBuilder(Material.ANVIL)
                .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Knockback : " + ChatColor.GREEN + "Custom")
                .setLore(
                    "",
                    ChatColor.GRAY + "Utilise un recul customisé et",
                    ChatColor.GRAY + "optimisé pour le Sumo.",
                    "",
                    ChatColor.YELLOW + "► Cliquez pour repasser en " + ChatColor.RED + "Vanilla"
                ).build();
        } else {
            customKBItem = new ItemBuilder(Material.ANVIL)
                .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Knockback : " + ChatColor.RED + "Vanilla")
                .setLore(
                    "",
                    ChatColor.GRAY + "Utilise le recul par défaut",
                    ChatColor.GRAY + "de Minecraft.",
                    "",
                    ChatColor.YELLOW + "► Cliquez pour passer en " + ChatColor.GREEN + "Custom"
                ).build();
        }
        inventory.setItem(2, customKBItem); // Placé avant le Double Jump (Slot 2)
    }

    public void open(Player player) {
        if (player.hasMetadata("modmode")) {
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + ChatColor.RED + "Vous ne pouvez pas créer un host en mode Modération !");
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
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + ChatColor.RED + "Vous ne pouvez pas créer un host en mode Modération !");
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
