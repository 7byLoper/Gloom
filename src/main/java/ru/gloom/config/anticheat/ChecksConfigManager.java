package ru.gloom.config.anticheat;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.gloom.api.configuration.ConfigManager;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.integration.worldguard.WorldGuardRegionBypassConfig;
import ru.gloom.integration.worldguard.WorldGuardRegionBypassService;

@Getter
public class ChecksConfigManager extends ConfigManager {
    private volatile boolean rotationAiEnabled;
    private int analysisSequence;
    private int analysisStep;

    private long combatTimer;

    private String analyzeServer;
    private WorldGuardRegionBypassService worldGuardRegionBypassService;

    public ChecksConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("anticheat/checks.yml", plugin));
    }

    @Override
    public void loadValues() {
        CustomConfig checksConfig = getCustomConfig("anticheat/checks.yml");
        rotationAiEnabled = checksConfig.getBoolean("ml_check.enable", true);

        analysisSequence = checksConfig.getInt("ml_check.sequence", 50);
        analysisStep = checksConfig.getInt("ml_check.step", 10);
        combatTimer = checksConfig.getInt("combat", 5) * 50L;

        analyzeServer = checksConfig.getString("ml_check.analyze_server", "https://api.gloom.net/v1/inference");
        worldGuardRegionBypassService = new WorldGuardRegionBypassService(
                plugin, WorldGuardRegionBypassConfig.fromConfig(checksConfig, "ml_check.worldguard"));
    }

    public CustomConfig getChecksConfig() {
        return super.getCustomConfig("anticheat/checks.yml");
    }

    public boolean isAimAiBypassedInRegion(Player player) {
        return worldGuardRegionBypassService != null && worldGuardRegionBypassService.isBypassed(player);
    }

    public boolean isRotationAiEnabled() {
        return rotationAiEnabled;
    }
}
