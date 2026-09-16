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
        if (section == null) {
            return new ItemBuilder(Material.AIR);
        }

        String materialStr = section.getString("material", "AIR");
        ItemBuilder builder = materialStr.startsWith("basehead-")
                ? new ItemBuilder(SkullUtils.getSkullByBase64EncodedTextureUrl(materialStr.replace("basehead-", "")))
                : new ItemBuilder(parseMaterial(materialStr));

        builder.name(section.getString("display_name", ""))
                .lore(section.getStringList("lore"))
                .unbreakable(section.getBoolean("unbreakable"));

        if (section.getBoolean("glow")) {
            builder.glow(true);
        }

        if (section.contains("model_data")) {
            builder.model(section.getInt("model_data"));
        }

        if (section.getBoolean("hide_attributes")) {
            builder.hideAttributes();
        }

        if (section.getBoolean("hide_enchantments")) {
            builder.hideEnchants();
        }

        if (section.getBoolean("hide_effects")) {
            builder.hideEffects();
        }

        if (section.contains("color")) {
            String colorStr = section.getString("color");
            if (colorStr != null && !colorStr.isEmpty()) {
                try {
                    if (colorStr.startsWith("#")) {
                        int hex = Integer.parseInt(colorStr.substring(1), 16);
                        builder.color(Color.fromRGB(hex));
                    } else {
                        String[] rgb = colorStr.split(",");
                        if (rgb.length == 3) {
                            int r = Integer.parseInt(rgb[0].trim());
                            int g = Integer.parseInt(rgb[1].trim());
                            int b = Integer.parseInt(rgb[2].trim());
                            builder.color(Color.fromRGB(r, g, b));
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        List<String> attributes = section.getStringList("attributes");
        if (!attributes.isEmpty()) {
            builder.attributes(attributes);
        }

        List<String> enchantments = section.getStringList("enchantments");
        if (!enchantments.isEmpty()) {
            builder.enchantments(enchantments);
        }

        List<String> effects = section.getStringList("effects");
        if (!effects.isEmpty()) {
            builder.potionEffects(effects);
        }

        if (section.contains("amount")) {
            builder.amount(section.getInt("amount", 1));
        }

        return builder;
    }

    private static Material parseMaterial(String materialStr) {
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Неверный материал: " + materialStr);
            return Material.AIR;
        }
    }

    public static List<AttributeData> parseAttributes(List<String> attributeStrings) {
        return attributeStrings.stream()
                .map(ItemBuilder::parseAttributeString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static AttributeData parseAttributeString(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length != 3) return null;

            EquipmentSlot slot = parseSlot(parts[0]);
            Attribute attribute = parseAttribute(parts[1]);
            double value = Double.parseDouble(parts[2]);

            if (slot != null && attribute != null) {
                return new AttributeData(slot, attribute, value);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Неверный формат атрибута: " + str);
        }
        return null;
    }

    private static EquipmentSlot parseSlot(String slotStr) {
        return switch (slotStr.toLowerCase()) {
            case "hand", "mainhand" -> EquipmentSlot.HAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFF_HAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static Attribute parseAttribute(String attrStr) {
        try {
            return Attribute.valueOf(attrStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public ItemBuilder potionEffects(List<String> effects) {
        if (effects == null || effects.isEmpty()) return this;

        ItemMeta meta = meta();
        if (!(meta instanceof PotionMeta potionMeta)) return this;

        for (String effectStr : effects) {
            try {
                String[] parts = effectStr.split(":");
                if (parts.length != 3) {
                    Bukkit.getLogger().warning("Неверный формат эффекта: " + effectStr);
                    continue;
                }

                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                if (effectType == null) {
                    Bukkit.getLogger().warning("Неизвестный тип эффекта: " + parts[0]);
                    continue;
                }

                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);

                PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true, true);

                potionMeta.addCustomEffect(effect, true);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Неверные числовые значения в эффекте: " + effectStr);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при разборе эффекта: " + effectStr);
            }
        }

        return meta(potionMeta);
    }

    public ItemBuilder enchantments(List<String> enchantments) {
        if (enchantments == null) return this;
        enchantments.forEach(this::applyEnchantment);
        return this;
    }

    private void applyEnchantment(String enchantmentStr) {
        try {
            String[] parts = enchantmentStr.split(":");
            if (parts.length < 2) {
                Bukkit.getLogger().warning("Неверный формат зачарования: " + enchantmentStr);
                return;
            }

            String enchantName;
            int level;
            if (parts.length == 3) {
                enchantName = parts[0] + ":" + parts[1];
                level = Integer.parseInt(parts[2]);
            } else {
                enchantName = parts[0];
                level = Integer.parseInt(parts[1]);
            }

            Enchantment enchantment = getEnchantmentByName(enchantName);
            if (enchantment == null) {
                Bukkit.getLogger().warning("Неизвестное зачарование: " + enchantName);
                return;
            }

            enchantment(enchantment, level);
        } catch (NumberFormatException e) {
            Bukkit.getLogger().warning("Неверный уровень зачарования в: " + enchantmentStr);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка при разборе зачарования: " + enchantmentStr);
        }
    }

    private Enchantment getEnchantmentByName(String name) {
        if (name == null) return null;

        if (name.contains(":")) {
            try {
                NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
                if (key != null) {
                    Enchantment enchantment = Enchantment.getByKey(key);
                    if (enchantment != null) return enchantment;
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при разборе кастомного ключа зачарования: " + name);
            }
        }

        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        if (enchantment != null) return enchantment;

        try {
            return Enchantment.getByName(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    public ItemBuilder attributes(List<String> attributes) {
        if (attributes == null) return this;
        return addAttributes(parseAttributes(attributes));
    }

    public ItemBuilder addAttributes(List<AttributeData> attributes) {
        if (attributes == null || attributes.isEmpty()) return this;

        ItemMeta meta = meta();
        if (meta == null) return this;

        attributes.forEach(data -> {
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_attribute_" + data.attribute.name(),
                    data.value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    data.slot);
            meta.addAttributeModifier(data.attribute, modifier);
        });

        return meta(meta);
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
        if (lore == null) {
            return this;
        }

        ItemMeta meta = meta();
        if (meta == null) {
            return this;
        }

        List<String> currentLore = lore();
        currentLore.addAll(StringColorize.parse(Arrays.asList(lore)));
        meta.setLore(currentLore);

        return meta(meta);
    }

    public ItemBuilder addLore(List<String> lore) {
        if (lore == null) {
            return this;
        }

        ItemMeta meta = meta();
        if (meta == null) {
            return this;
        }

        List<String> currentLore = lore();
        currentLore.addAll(StringColorize.parse(lore));
        meta.setLore(currentLore);

        return meta(meta);
    }

    public ItemBuilder addLore(Component... lore) {
        if (lore == null) {
            return this;
        }

        return addComponentLore(Arrays.asList(lore));
    }

    public ItemBuilder addComponentLore(List<Component> lore) {
        ItemMeta meta = meta();
        if (meta == null || lore == null) {
            return this;
        }

        List<Component> currentLore = componentLore();
        currentLore.addAll(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        meta.lore(currentLore);

        return meta(meta);
    }

    public ItemBuilder addLoreAbove(String... lore) {
        if (lore == null) {
            return this;
        }

        ItemMeta meta = meta();
        if (meta == null) {
            return this;
        }

        List<String> currentLore = lore();
        List<String> toAdd = StringColorize.parse(Arrays.asList(lore));
        currentLore.addAll(0, toAdd);
        meta.setLore(currentLore);

        return meta(meta);
    }

    public ItemBuilder addLoreAbove(Component... lore) {
        if (lore == null) {
            return this;
        }

        return addComponentLoreAbove(Arrays.asList(lore));
    }

    public ItemBuilder addComponentLoreAbove(List<Component> lore) {
        ItemMeta meta = meta();
        if (meta == null || lore == null) {
            return this;
        }

        List<Component> currentLore = componentLore();
        currentLore.addAll(0, lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
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
        if (section == null) return this;

        section.set("material", item.getType().name());
        section.set("amount", item.getAmount());

        ItemMeta meta = meta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                section.set("display_name", StringColorize.convertMinecraftColorCodes(name()));
            }

            if (meta.hasLore()) {
                section.set(
                        "lore",
                        lore().stream()
                                .map(StringColorize::convertMinecraftColorCodes)
                                .collect(Collectors.toList()));
            }

            if (meta.hasCustomModelData()) {
                section.set("model_data", meta.getCustomModelData());
            }

            Set<ItemFlag> flags = meta.getItemFlags();
            if (!flags.isEmpty()) {
                section.set("flags", flags.stream().map(ItemFlag::name).collect(Collectors.toList()));
            }

            if (!meta.getEnchants().isEmpty()) {
                List<String> enchantList = meta.getEnchants().entrySet().stream()
                        .map(entry -> {
                            NamespacedKey key = entry.getKey().getKey();
                            return key.getNamespace() + ":" + key.getKey() + ":" + entry.getValue();
                        })
                        .collect(Collectors.toList());
                section.set("enchantments", enchantList);
            }

            if (meta instanceof LeatherArmorMeta leatherMeta) {
                Color color = leatherMeta.getColor();
                section.set("color", String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue()));
            } else if (meta instanceof PotionMeta potionMeta && potionMeta.getColor() != null) {
                Color color = potionMeta.getColor();
                section.set("color", String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue()));
            }

            section.set(
                    "glow",
                    GLOW_ENCHANTMENT != null
                            && meta.hasEnchant(GLOW_ENCHANTMENT)
                            && meta.getEnchantLevel(GLOW_ENCHANTMENT) == 1
                            && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));

            if (meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) {
                section.set("hide_attributes", true);
            }

            if (meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                section.set("hide_enchantments", true);
            }

            if (meta.isUnbreakable()) {
                section.set("unbreakable", true);
            }
        }

        return this;
    }

    public ItemStack build() {
        return item.clone();
    }

    public static record AttributeData(EquipmentSlot slot, Attribute attribute, double value) {}
}
