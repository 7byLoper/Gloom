package ru.gloom.api.itemstack;

import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.gloom.utils.StringColorize;

@SuppressWarnings("ALL")
public class ItemBuilder {

    private static final Enchantment GLOW_ENCHANTMENT = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));

    private final ItemStack item;

    public ItemBuilder(ItemStack item) {
        this.item = item != null ? item.clone() : new ItemStack(Material.AIR);
    }

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material != null ? material : Material.AIR);
    }

    public static ItemBuilder fromConfig(ConfigurationSection section) {
        return ItemConfigSerializer.fromConfig(section);
    }


    public ItemBuilder potionEffects(List<String> effects) {
        ItemConfigSerializer.applyPotionEffects(this, effects);
        return this;
    }

    public ItemBuilder enchantments(List<String> enchantments) {
        ItemConfigSerializer.applyEnchantments(this, enchantments);
        return this;
    }

    public ItemBuilder attributes(List<String> attributes) {
        ItemConfigSerializer.applyAttributes(this, attributes);
        return this;
    }

    public ItemBuilder addAttributes(List<AttributeData> attributes) {
        ItemConfigSerializer.addAttributes(this, attributes);
        return this;
    }

    public ItemMeta meta() {
        return item.getItemMeta();
    }

    public ItemBuilder meta(ItemMeta meta) {
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder model(int model) {
        ItemMeta meta = meta();
        if (meta == null) return this;
        meta.setCustomModelData(model);
        return meta(meta);
    }

    public ItemBuilder flags(ItemFlag... itemFlags) {
        ItemMeta meta = meta();
        if (meta == null || itemFlags == null) return this;
        meta.addItemFlags(itemFlags);
        return meta(meta);
    }

    public ItemBuilder flags(String... itemFlags) {
        if (itemFlags == null) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;
        for (String flagName : itemFlags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Неизвестный флаг предмета: " + flagName);
            }
        }
        return meta(meta);
    }

    public ItemBuilder enchantment(Enchantment enchantment, int level) {
        if (enchantment == null) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;
        meta.addEnchant(enchantment, level, true);
        return meta(meta);
    }

    public String name() {
        ItemMeta meta = meta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "";
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = meta();
        if (meta == null || name == null) return this;
        meta.setDisplayName(StringColorize.parse(name));
        return meta(meta);
    }

    public Component componentName() {
        ItemMeta meta = meta();
        if (meta == null || !meta.hasDisplayName() || meta.displayName() == null) {
            return Component.empty();
        }

        return meta.displayName();
    }

    public ItemBuilder name(Component name) {
        ItemMeta meta = meta();
        if (meta == null || name == null) return this;
        meta.displayName(name);
        return meta(meta);
    }

    public List<String> lore() {
        ItemMeta meta = meta();
        return meta != null && meta.getLore() != null ? meta.getLore() : new ArrayList<>();
    }

    public List<Component> componentLore() {
        ItemMeta meta = meta();
        if (meta == null || meta.lore() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(meta.lore());
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = meta();
        if (meta == null || lore == null || lore.isEmpty()) {
            return this;
        }

        meta.setLore(StringColorize.parse(lore));
        return meta(meta);
    }

    public ItemBuilder lore(Component... lore) {
        if (lore == null) {
            return this;
        }

        return componentLore(Arrays.asList(lore));
    }

    public ItemBuilder componentLore(List<Component> lore) {
        ItemMeta meta = meta();
        if (meta == null || lore == null) {
            return this;
        }

        meta.lore(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return meta(meta);
    }

    public ItemBuilder addLore(String... lore) {
        if (lore == null) return this;
        return addLore(Arrays.asList(lore));
    }

    public ItemBuilder addLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;

        List<String> currentLore = lore();
        currentLore.addAll(StringColorize.parse(lore));
        meta.setLore(currentLore);
        return meta(meta);
    }

    public ItemBuilder addLore(Component... lore) {
        if (lore == null) return this;
        return addComponentLore(Arrays.asList(lore));
    }

    public ItemBuilder addComponentLore(List<Component> lore) {
        if (lore == null || lore.isEmpty()) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;

        List<Component> currentLore = componentLore();
        currentLore.addAll(lore.stream().filter(Objects::nonNull).toList());
        meta.lore(currentLore);
        return meta(meta);
    }

    public ItemBuilder addLoreAbove(String... lore) {
        if (lore == null) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;

        List<String> currentLore = lore();
        currentLore.addAll(0, StringColorize.parse(Arrays.asList(lore)));
        meta.setLore(currentLore);
        return meta(meta);
    }

    public ItemBuilder addLoreAbove(Component... lore) {
        if (lore == null) return this;
        return addComponentLoreAbove(Arrays.asList(lore));
    }

    public ItemBuilder addComponentLoreAbove(List<Component> lore) {
        if (lore == null || lore.isEmpty()) return this;
        ItemMeta meta = meta();
        if (meta == null) return this;

        List<Component> currentLore = componentLore();
        currentLore.addAll(0, lore.stream().filter(Objects::nonNull).toList());
        meta.lore(currentLore);
        return meta(meta);
    }

    public ItemBuilder color(Color color) {
        ItemMeta meta = meta();
        if (meta == null || color == null) return this;
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        } else if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(color);
        }
        return meta(meta);
    }

    public ItemBuilder hideAttributes() {
        return flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_POTION_EFFECTS);
    }

    public ItemBuilder hideEnchants() {
        return flags(ItemFlag.HIDE_ENCHANTS);
    }

    public ItemBuilder hideEffects() {
        return flags(ItemFlag.HIDE_POTION_EFFECTS);
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        ItemMeta meta = meta();
        if (meta == null) return this;
        meta.setUnbreakable(unbreakable);
        return meta(meta);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return this;
    }

    public ItemBuilder material(Material material) {
        item.setType(material != null ? material : Material.AIR);
        return this;
    }

    public Material material() {
        return item.getType();
    }

    public int amount() {
        return item.getAmount();
    }

    public ItemBuilder glow(boolean isGlow) {
        ItemMeta meta = meta();
        if (meta == null) {
            return this;
        }

        if (GLOW_ENCHANTMENT == null) {
            return this;
        }

        if (isGlow) {
            meta.addEnchant(GLOW_ENCHANTMENT, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(GLOW_ENCHANTMENT);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta(meta);
        return hideEnchants();
    }

    public <T, Z> ItemBuilder namespacedKey(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = meta();
        if (meta == null || key == null || type == null || value == null) return this;
        meta.getPersistentDataContainer().set(key, type, value);
        return meta(meta);
    }

    public <T, Z> Z getNamespacedKey(NamespacedKey key, PersistentDataType<T, Z> type) {
        ItemMeta meta = meta();
        if (meta == null || key == null || type == null) return null;
        return meta.getPersistentDataContainer().get(key, type);
    }

    public <T, Z> boolean hasNamespacedKey(NamespacedKey key, PersistentDataType<T, Z> type) {
        ItemMeta meta = meta();
        if (meta == null || key == null || type == null) return false;
        return meta.getPersistentDataContainer().has(key, type);
    }

    public ItemBuilder removeNamespacedKey(NamespacedKey key) {
        ItemMeta meta = meta();
        if (meta == null || key == null) return this;
        meta.getPersistentDataContainer().remove(key);
        return meta(meta);
    }

    public ItemBuilder save(ConfigurationSection section) {
        ItemConfigSerializer.save(this, section);
        return this;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack build() {
        return item.clone();
    }
}
