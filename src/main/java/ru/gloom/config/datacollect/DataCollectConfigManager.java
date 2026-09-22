package ru.gloom.config.datacollect;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import ru.gloom.api.configuration.ConfigManager;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.api.model.TrainStatisticInfoType;

@Getter
public class DataCollectConfigManager extends ConfigManager {
    private int framesToCollect;
    private long combatTimer;
    private boolean clearBufferOnCombatTimeout;

    private boolean autosaveStatus;
    private int autosaveValue;

    private TrainStatisticInfoType infoType;
    private String infoMessage;

    private String messageStartHelp;
    private String messageStartNoneSelected;
    private String messageStopHelp;
    private String messageStopNoneCollecting;
    private String messageAllOffline;
    private String messageAllAlreadyCollecting;
    private String messageAllNoneType;
    private String messageAllStart;
    private String messageAllStop;
    private String messageAllStopAuto;

    public DataCollectConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("features/datacollect.yml", plugin));
    }

    @Override
    public void loadValues() {
        CustomConfig config = getCustomConfig("features/datacollect.yml");
        framesToCollect = config.getInt("frames", 40);
        combatTimer = config.getInt("combat_timer", 20) * 50L;
        clearBufferOnCombatTimeout = config.getBoolean("clear_buffer_on_combat_timeout", false);
        autosaveStatus = config.getBoolean("auto_save.enable", true);
        autosaveValue = config.getInt("auto_save.value", 40);

        String infoTypeStr = config.getString("info_statistic.type", "BOSSBAR");
        infoType = TrainStatisticInfoType.getByName(infoTypeStr);
        infoMessage = config.configMessage("info_statistic.line");

        messageStartHelp = config.configMessage("messages.start.help");
        messageStartNoneSelected = config.configMessage("messages.start.none_selected");

        messageStopHelp = config.configMessage("messages.stop.help");
        messageStopNoneCollecting = config.configMessage("messages.stop.no_collecting");

        messageAllOffline = config.configMessage("messages.all.offline");
        messageAllAlreadyCollecting = config.configMessage("messages.all.already_collecting");
        messageAllNoneType = config.configMessage("messages.all.none_type");
        messageAllStart = config.configMessage("messages.start.start");
        messageAllStop = config.configMessage("messages.stop.stop");
        messageAllStopAuto = config.configMessage("messages.stop.auto_stop");
    }
}
