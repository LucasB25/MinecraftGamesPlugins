package fr.corehost.staffmod.gui;

import fr.corehost.staffmod.StaffModPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InvseeGUI {

    private final StaffModPlugin plugin;
    private final Player target;

    public InvseeGUI(StaffModPlugin plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void open(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Inventaire : " + target.getName(), NamedTextColor.RED, TextDecoration.BOLD)));

        // Copier l'inventaire principal (0 à 35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = target.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                inv.setItem(i, item.clone());
            }
        }

        // Ligne de séparation (36 à 44)
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, MenuUtils.getGrayFiller());
        }

        // Pièces d'armure (45 à 48)
        ItemStack helmet = target.getInventory().getHelmet();
        ItemStack chest = target.getInventory().getChestplate();
        ItemStack legs = target.getInventory().getLeggings();
        ItemStack boots = target.getInventory().getBoots();
        ItemStack offhand = target.getInventory().getItemInOffHand();

        inv.setItem(45, (helmet != null && helmet.getType() != Material.AIR) ? helmet.clone() : createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, "Casque : Aucun"));
        inv.setItem(46, (chest != null && chest.getType() != Material.AIR) ? chest.clone() : createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, "Plastron : Aucun"));
        inv.setItem(47, (legs != null && legs.getType() != Material.AIR) ? legs.clone() : createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, "Jambières : Aucune"));
        inv.setItem(48, (boots != null && boots.getType() != Material.AIR) ? boots.clone() : createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, "Bottes : Aucune"));

        inv.setItem(49, MenuUtils.getGrayFiller());

        // Seconde Main (50)
        inv.setItem(50, (offhand != null && offhand.getType() != Material.AIR) ? offhand.clone() : createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, "Seconde Main : Aucune"));

        inv.setItem(51, MenuUtils.getGrayFiller());
        inv.setItem(52, MenuUtils.getGrayFiller());

        // Bouton Fermer (53)
        inv.setItem(53, MenuUtils.getCloseButton());

        player.openInventory(inv);
    }

    private ItemStack createPlaceholder(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }
}
