package ru.gloom.menu.players.buttons;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.gloom.api.menu.MenuItem;
import ru.gloom.api.menu.button.Button;
import ru.gloom.menu.players.PlayersMenu;
import ru.gloom.utils.ItemPlaceholderUtils;

public class NextPageButton extends Button {
    private final PlayersMenu menu;

    public NextPageButton(MenuItem menuItem, PlayersMenu menu) {
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
