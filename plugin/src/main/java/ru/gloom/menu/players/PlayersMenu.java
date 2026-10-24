package ru.gloom.menu.players;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.gloom.GloomAI;
import ru.gloom.api.menu.MenuItem;
import ru.gloom.api.menu.button.Button;
import ru.gloom.api.menu.impl.ActionMenu;
import ru.gloom.database.model.PlayerAIProbabilityData;
import ru.gloom.menu.players.buttons.NextPageButton;
import ru.gloom.menu.players.buttons.PlayerButton;
import ru.gloom.menu.players.buttons.PreviousPageButton;
import ru.gloom.utils.ItemPlaceholderUtils;

@Getter
public class PlayersMenu extends ActionMenu {
    private final PlayersMenuSettings playersMenuSettings;

    private int page;
    private int maxPages = 1;
    private int playersCount;

    public PlayersMenu(PlayersMenuSettings menuSettings) {
        super(menuSettings);
        this.playersMenuSettings = menuSettings;
    }

    public PlayersMenu() {
        this(GloomAI.INSTANCE.getMainConfigManager().getMenuSettings());
    }

    @Override
    public @Nullable String getTitle() {
        return ItemPlaceholderUtils.apply(
                playersMenuSettings.getTitle(), PlayersMenuPlaceholders.menuPlaceholders(page, maxPages, playersCount));
    }

    @Override
    protected Button createButton(MenuItem menuItem) {
        return switch (menuItem.getActionName()) {
            case "previous_page", "previous-page", "prev_page", "prev-page" -> new PreviousPageButton(menuItem, this);
            case "next_page", "next-page" -> new NextPageButton(menuItem, this);
            case "refresh", "reload" ->
                Button.builder()
                        .itemStack(ItemPlaceholderUtils.buildMenuItem(menuItem, getMenuPlaceholders()))
                        .slots(menuItem.getSlot())
                        .build(event -> {
                            if (event.getWhoClicked() instanceof Player player) {
                                show(player);
                            }
                        });
            default ->
                Button.builder()
                        .itemStack(ItemPlaceholderUtils.buildMenuItem(menuItem, getMenuPlaceholders()))
                        .slots(menuItem.getSlot())
                        .build(event -> {});
        };
    }

    @Override
    protected void onLoad() {
        if (playersMenuSettings.getPlayerForm() == null
                || playersMenuSettings.getPlayerSlots().isEmpty()) {
            return;
        }

        List<PlayerAIProbabilityData> playersData = loadPlayersData();
        playersCount = playersData.size();
        maxPages = Math.max(1, (int) Math.ceil(
                playersCount / (double) playersMenuSettings.getPlayerSlots().size()));
        page = Math.max(0, Math.min(page, maxPages - 1));

        int fromIndex = page * playersMenuSettings.getPlayerSlots().size();
        int toIndex = Math.min(fromIndex + playersMenuSettings.getPlayerSlots().size(), playersData.size());

        if (fromIndex >= toIndex) {
            return;
        }

        List<PlayerAIProbabilityData> pagePlayers = playersData.subList(fromIndex, toIndex);
        for (int index = 0; index < pagePlayers.size(); index++) {
            menuButtons.add(new PlayerButton(
                    playersMenuSettings.getPlayerForm(),
                    pagePlayers.get(index),
                    playersMenuSettings.getPlayerSlots().get(index),
                    this));
        }
    }

    public Map<String, String> getMenuPlaceholders() {
        return PlayersMenuPlaceholders.menuPlaceholders(page, maxPages, playersCount);
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return page + 1 < maxPages;
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            page--;
        }
    }

    public void nextPage() {
        if (hasNextPage()) {
            page++;
        }
    }

    private List<PlayerAIProbabilityData> loadPlayersData() {
        try {
            List<PlayerAIProbabilityData> playersData =
                    GloomAI.INSTANCE.getPlayerOnlineService().getOnlinePlayers().stream()
                            .map(playerName -> GloomAI.INSTANCE
                                    .getViolationManager()
                                    .getProbabilityStorage()
                                    .getPlayerDataByName(playerName)
                                    .join())
                            .filter(Objects::nonNull)
                            .toList();

            if (playersData.isEmpty()) {
                return new ArrayList<>();
            }

            int checksLimit = playersMenuSettings.getChecksLimit();
            playersData = new ArrayList<>(playersData);
            playersData.sort(Comparator.comparingDouble((PlayerAIProbabilityData data) ->
                            PlayersMenuPlaceholders.averageProbability(data, checksLimit))
                    .reversed());
            return playersData;
        } catch (CompletionException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Не удалось загрузить игроков для меню античита", exception);
            return new ArrayList<>();
        }
    }
}
