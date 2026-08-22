package ru.gloom.api.itemstack;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class ItemStackServices {
    @Setter
    private volatile ItemStack skullHead;

    public ItemStack skullHead() {
        ItemStack h = skullHead;
        if (h == null) {
            throw new IllegalStateException("head item stack is not set");
        }

        return h.clone();
    }
}
