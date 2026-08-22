package ru.gloom.menu.history.buttons;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.gloom.api.menu.MenuItem;
import ru.gloom.api.menu.button.Button;
import ru.gloom.menu.history.HistoryMenu;
import ru.gloom.utils.ItemPlaceholderUtils;

public class NextPageButton extends Button {
    private final HistoryMenu menu;

    public NextPageButton(MenuItem menuItem, HistoryMenu menu) {
        super(ItemPlaceholderUtils.buildMenuItem(menuItem, menu.getMenuPlaceholders()), menuItem.getSlot());
        this.menu = menu;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!menu.hasNextPage()) {
            return;
        }

        menu.nextPage();
        menu.show(player);
    }
}
