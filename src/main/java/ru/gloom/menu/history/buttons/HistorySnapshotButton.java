package ru.gloom.menu.history.buttons;

import org.bukkit.event.inventory.InventoryClickEvent;
import ru.gloom.api.menu.MenuItem;
import ru.gloom.api.menu.button.Button;
import ru.gloom.database.model.PlayerAIProbabilityData.ProbSnapshot;
import ru.gloom.menu.history.HistoryMenu;
import ru.gloom.menu.history.HistoryMenuPlaceholders;

public class HistorySnapshotButton extends Button {
    public HistorySnapshotButton(
            MenuItem menuItem, ProbSnapshot snapshot, int snapshotNumber, int slot, HistoryMenu menu) {
        super(HistoryMenuPlaceholders.buildSnapshotItem(menuItem, snapshot, snapshotNumber, menu), slot);
    }

    @Override
    public void onClick(InventoryClickEvent event) {}
}
