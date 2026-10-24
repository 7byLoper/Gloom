package ru.gloom.api.itemstack;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.gloom.utils.StringColorize;

@UtilityClass
public class ItemConfigSerializer {
    private static final Enchantment GLOW_ENCHANTMENT = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));

    public ItemBuilder fromConfig(ConfigurationSection section) {
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

    public Material parseMaterial(String materialStr) {
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Неверный материал: " + materialStr);
            return Material.AIR;
        }
    }

    public List<AttributeData> parseAttributes(List<String> attributeStrings) {
        return attributeStrings.stream()
                .map(ItemConfigSerializer::parseAttributeString)
                .filter(Objects::nonNull)
                .toList();
    }

    public AttributeData parseAttributeString(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length != 3) {
                return null;
            }

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

    public EquipmentSlot parseSlot(String slotStr) {
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

    public Attribute parseAttribute(String attrStr) {
        try {
            return Attribute.valueOf(attrStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void applyPotionEffects(ItemBuilder builder, List<String> effects) {
        if (effects == null || effects.isEmpty() || builder == null) {
            return;
        }

        ItemMeta meta = builder.meta();
        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        for (String effectStr : effects) {
            try {
                String[] parts = effectStr.split(":");
                if (parts.length != 3) {
                    continue;
                }

                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                if (effectType == null) {
                    continue;
                }

                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amplifier, true, true, true), true);
            } catch (Exception ignored) {
            }
        }

        builder.meta(potionMeta);
    }

    public void applyEnchantments(ItemBuilder builder, List<String> enchantments) {
        if (enchantments == null || builder == null) {
            return;
        }

        for (String enchantmentStr : enchantments) {
            try {
                String[] parts = enchantmentStr.split(":");
                if (parts.length < 2) {
                    continue;
                }

                String enchantName = parts.length == 3 ? parts[0] + ":" + parts[1] : parts[0];
                int level = Integer.parseInt(parts.length == 3 ? parts[2] : parts[1]);
                Enchantment enchantment = getEnchantmentByName(enchantName);
                if (enchantment != null) {
                    builder.enchantment(enchantment, level);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public Enchantment getEnchantmentByName(String name) {
        if (name == null) {
            return null;
        }

        if (name.contains(":")) {
            try {
                NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
                if (key != null) {
                    Enchantment enchantment = Enchantment.getByKey(key);
                    if (enchantment != null) {
                        return enchantment;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        if (enchantment != null) {
            return enchantment;
        }

        try {
            return Enchantment.getByName(name.toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    public void applyAttributes(ItemBuilder builder, List<String> attributes) {
        if (attributes == null || builder == null) {
            return;
        }
        addAttributes(builder, parseAttributes(attributes));
    }

    public void addAttributes(ItemBuilder builder, List<AttributeData> attributes) {
        if (attributes == null || attributes.isEmpty() || builder == null) {
            return;
        }

        ItemMeta meta = builder.meta();
        if (meta == null) {
            return;
        }

        attributes.forEach(data -> {
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_attribute_" + data.attribute().name(),
                    data.value(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    data.slot());
            meta.addAttributeModifier(data.attribute(), modifier);
        });

        builder.meta(meta);
    }

    public void save(ItemBuilder builder, ConfigurationSection section) {
        if (builder == null || section == null) {
            return;
        }

        section.set("material", builder.getItem().getType().name());
        section.set("amount", builder.getItem().getAmount());

        ItemMeta meta = builder.getItem().getItemMeta();
        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            section.set("display_name", StringColorize.convertMinecraftColorCodes(builder.name()));
        }

        if (meta.hasLore()) {
            section.set(
                    "lore",
                    builder.lore().stream()
                            .map(StringColorize::convertMinecraftColorCodes)
                            .toList());
        }

        if (meta.hasCustomModelData()) {
            section.set("model_data", meta.getCustomModelData());
        }

        Set<ItemFlag> flags = meta.getItemFlags();
        if (!flags.isEmpty()) {
            section.set("flags", flags.stream().map(ItemFlag::name).toList());
        }

        if (!meta.getEnchants().isEmpty()) {
            List<String> enchantList = meta.getEnchants().entrySet().stream()
                    .map(entry -> {
                        NamespacedKey key = entry.getKey().getKey();
                        return key.getNamespace() + ":" + key.getKey() + ":" + entry.getValue();
                    })
                    .toList();
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
}
