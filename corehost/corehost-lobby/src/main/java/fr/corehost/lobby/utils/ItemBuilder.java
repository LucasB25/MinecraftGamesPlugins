package fr.corehost.lobby.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = this.itemStack.getItemMeta();
    }
    
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (this.itemMeta != null) {
            this.itemMeta.setDisplayName(name);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        if (this.itemMeta != null) {
            this.itemMeta.setLore(Arrays.asList(lore));
        }
        return this;
    }
    
    public ItemBuilder setLore(List<String> lore) {
        if (this.itemMeta != null) {
            this.itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (this.itemMeta instanceof SkullMeta) {
            ((SkullMeta) this.itemMeta).setOwner(owner);
        }
        return this;
    }

    public <T, Z> ItemBuilder addPersistentData(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        if (this.itemMeta != null) {
            this.itemMeta.getPersistentDataContainer().set(key, type, value);
        }
        return this;
    }

    public ItemStack build() {
        if (this.itemMeta != null) {
            this.itemStack.setItemMeta(this.itemMeta);
        }
        return this.itemStack;
    }
}
