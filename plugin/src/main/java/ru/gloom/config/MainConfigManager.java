package ru.gloom.config;

import java.util.TreeMap;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import ru.gloom.api.configuration.ConfigManager;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.api.configuration.RedisConfig;
import ru.gloom.api.database.DatabaseManager;
import ru.gloom.menu.history.HistoryMenuSettings;
import ru.gloom.menu.players.PlayersMenuSettings;
import ru.gloom.utils.StringColorize;

@Getter
public class MainConfigManager extends ConfigManager {
    private DatabaseManager databaseManager;
    private RedisConfig redisConfig;

    private boolean printToConsole;
    private boolean testMode;
    private int historyEntryPerPage;
    private int maxProbEntries;

    private String prefix;

    private String alertsEnabledMessage;
    private String alertsDisableMessage;
    private String verboseEnabledMessage;
    private String verboseDisableMessage;
    private String hologramsEnabledMessage;
    private String hologramsDisableMessage;

    private String helpHeaderMessage;
    private String helpFormatMessage;
    private String helpFooterMessage;

    private String statusHeaderMessage;
    private String statusBackendMessage;
    private String statusModelMessage;
    private String statusRequestsTodayMessage;
    private String statusRequestsRemainingMessage;
    private String statusPlanMessage;
    private String statusCheckingMessage;
    private String statusConnectedText;
    private String statusNotConnectedText;
    private String statusNoLicenseMessage;
    private String statusErrorMessage;
    private String statusUnlimitedText;

    private String alertMessage;
    private String verboseMessage;
    private String verboseTestModeOnlyMessage;

    private String historyHeaderMessage;
    private String historyEntryMessage;
    private String historyOnlyPlayerMessage;
    private String historyNoDataMessage;

    private String reloadingMessage;
    private String reloadedMessage;

    private String connectStatusMissingMessage;
    private String connectStatusSavedMessage;
    private String connectUsageMessage;
    private String connectInvalidPanelUrlMessage;
    private String connectAlreadyConnectingMessage;
    private String connectConnectingMessage;
    private String connectNetworkErrorMessage;
    private String connectRateLimitedMessage;
    private String connectRejectedMessage;
    private String connectSuccessMessage;
    private String connectSaveErrorMessage;
    private String connectInvalidResponseMessage;

    private String monitorActionBarFormat;
    private String monitorWaitingFormat;
    private String monitorTrendUpFormat;
    private String monitorTrendDownFormat;
    private String monitorTrendEqualFormat;

    private String monitorOnlyPlayerMessage;
    private String monitorEnabledMessage;
    private String monitorDisabledMessage;
    private String monitorNotRunningMessage;
    private String monitorSwitchedMessage;
    private String monitorPlayerNotFoundMessage;
    private String monitorTargetLeftMessage;

    private int maxLocalEntries;
    private TreeMap<Double, String> aiChanceColors;

    private PlayersMenuSettings menuSettings;
    private HistoryMenuSettings historySettings;

    public MainConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("settings.yml", plugin));
        addCustomConfig(new CustomConfig("translation.yml", plugin));

        addCustomConfig(new CustomConfig("menu/players.yml", plugin));
        addCustomConfig(new CustomConfig("menu/history.yml", plugin));
    }

    @Override
    public void loadValues() {
        ConfigurationSection settings = getCustomConfig("settings.yml").getConfig();
        databaseManager = DatabaseManager.fromSection(settings.getConfigurationSection("database"), plugin);
        redisConfig = RedisConfig.fromSection(settings.getConfigurationSection("redis"));

        printToConsole = settings.getBoolean("print_to_console", false);
        testMode = settings.getBoolean("test_mode", false);
        historyEntryPerPage = settings.getInt("history_entries_per_page", 15);

        maxLocalEntries = settings.getInt("max_local_entries", 20);
        maxProbEntries = settings.getInt("max_prob_entries", 21);

        aiChanceColors = new TreeMap<>();
        ConfigurationSection colorSection = settings.getConfigurationSection("ai_colors");

        if (colorSection != null) {
            for (String key : colorSection.getKeys(false)) {
                double chance = Double.parseDouble(key.replace(",", "."));
                String color = StringColorize.parse(colorSection.getString(key, "§f"));
                aiChanceColors.put(chance, color);
            }
        }

        CustomConfig messagesConfig = getCustomConfig("translation.yml");
        prefix = messagesConfig.configMessage("prefix");

        alertsEnabledMessage = message(messagesConfig, "alerts-enabled");
        alertsDisableMessage = message(messagesConfig, "alerts-disabled");
        verboseEnabledMessage = message(messagesConfig, "verbose-enabled");
        verboseDisableMessage = message(messagesConfig, "verbose-disabled");
        verboseTestModeOnlyMessage = message(messagesConfig, "verbose-test-mode-only");
        hologramsEnabledMessage = message(messagesConfig, "holograms-enabled");
        hologramsDisableMessage = message(messagesConfig, "holograms-disabled");

        alertMessage = message(messagesConfig, "alert");
        verboseMessage = message(messagesConfig, "verbose");

        historyHeaderMessage = message(messagesConfig, "history-header");
        historyEntryMessage = message(messagesConfig, "history-entry");
        historyOnlyPlayerMessage = message(messagesConfig, "history-only-player");
        historyNoDataMessage = message(messagesConfig, "history-no-data");

        reloadingMessage = message(messagesConfig, "reloading");
        reloadedMessage = message(messagesConfig, "reloaded");

        helpHeaderMessage = message(messagesConfig, "help.header");
        helpFormatMessage = message(messagesConfig, "help.format");
        helpFooterMessage = message(messagesConfig, "help.footer");

        statusHeaderMessage = message(messagesConfig, "status.header");
        statusBackendMessage = message(messagesConfig, "status.backend");
        statusModelMessage = message(messagesConfig, "status.model");
        statusRequestsTodayMessage = message(messagesConfig, "status.requests_today");
        statusRequestsRemainingMessage = message(messagesConfig, "status.requests_remaining");
        statusPlanMessage = message(messagesConfig, "status.plan");
        statusCheckingMessage = message(messagesConfig, "status.checking");
        statusConnectedText = message(messagesConfig, "status.connected");
        statusNotConnectedText = message(messagesConfig, "status.not_connected");
        statusNoLicenseMessage = message(messagesConfig, "status.no_license");
        statusErrorMessage = message(messagesConfig, "status.error");
        statusUnlimitedText = message(messagesConfig, "status.unlimited");

        connectStatusMissingMessage = message(messagesConfig, "connect.status_missing");
        connectStatusSavedMessage = message(messagesConfig, "connect.status_saved");
        connectUsageMessage = message(messagesConfig, "connect.usage");
        connectInvalidPanelUrlMessage = message(messagesConfig, "connect.invalid_panel_url");
        connectAlreadyConnectingMessage = message(messagesConfig, "connect.already_connecting");
        connectConnectingMessage = message(messagesConfig, "connect.connecting");
        connectNetworkErrorMessage = message(messagesConfig, "connect.network_error");
        connectRateLimitedMessage = message(messagesConfig, "connect.rate_limited");
        connectRejectedMessage = message(messagesConfig, "connect.rejected");
        connectSuccessMessage = message(messagesConfig, "connect.success");
        connectSaveErrorMessage = message(messagesConfig, "connect.save_error");
        connectInvalidResponseMessage = message(messagesConfig, "connect.invalid_response");

        monitorActionBarFormat = message(messagesConfig, "monitor.action_bar.format");
        monitorWaitingFormat = message(messagesConfig, "monitor.action_bar.waiting");
        monitorTrendUpFormat = message(messagesConfig, "monitor.action_bar.trend.up");
        monitorTrendDownFormat = message(messagesConfig, "monitor.action_bar.trend.down");
        monitorTrendEqualFormat = message(messagesConfig, "monitor.action_bar.trend.equal");

        monitorOnlyPlayerMessage = message(messagesConfig, "monitor.only_player");
        monitorEnabledMessage = message(messagesConfig, "monitor.enabled");
        monitorDisabledMessage = message(messagesConfig, "monitor.disabled");
        monitorNotRunningMessage = message(messagesConfig, "monitor.not_running");
        monitorSwitchedMessage = message(messagesConfig, "monitor.switched");
        monitorPlayerNotFoundMessage = message(messagesConfig, "monitor.player_not_found");
        monitorTargetLeftMessage = message(messagesConfig, "monitor.target_left");

        menuSettings = PlayersMenuSettings.fromSection(
                getCustomConfig("menu/players.yml").getConfig());
        historySettings = HistoryMenuSettings.fromSection(
                getCustomConfig("menu/history.yml").getConfig());
    }

    public String getChanceColor(double chance) {
        var entry = aiChanceColors.floorEntry(chance);
        return entry != null ? entry.getValue() : "§f";
    }

    public String getChanceString(double chance) {
        String color = getChanceColor(chance);
        return "%s%.4f".formatted(color, chance);
    }

    public String getPercentString(double percent) {
        String color = getChanceColor(percent);
        return "%s%.2f".formatted(color, percent * 100);
    }

    private String message(CustomConfig config, String path) {
        return config.configMessage(path).replace("{prefix}", prefix);
    }
}
