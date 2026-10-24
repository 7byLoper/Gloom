package ru.gloom.api.itemstack;

import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;

public record AttributeData(EquipmentSlot slot, Attribute attribute, double value) {
}
