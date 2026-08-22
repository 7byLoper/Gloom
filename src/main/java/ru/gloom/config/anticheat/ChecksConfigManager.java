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
    private int analysisSequence;
    private int analysisStep;
    private int targetAnalysisSequence;
    private int targetAnalysisStep;

    private long combatTimer;

    private String analyzeServer;
    private String targetAnalyzeServer;
    private WorldGuardRegionBypassService worldGuardRegionBypassService;
    private WorldGuardRegionBypassService targetWorldGuardRegionBypassService;

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

        analysisSequence = checksConfig.getInt("ml_check.sequence", 50);
        analysisStep = checksConfig.getInt("ml_check.step", 10);
        targetAnalysisSequence = checksConfig.getInt("target_ml_check.sequence", 50);
        targetAnalysisStep = checksConfig.getInt("target_ml_check.step", 10);
        combatTimer = checksConfig.getInt("combat", 5) * 50L;

        analyzeServer = checksConfig.getString("ml_check.analyze_server", "https://api.gloom.net/v1/inference");
        targetAnalyzeServer = checksConfig.getString("target_ml_check.analyze_server", analyzeServer);
        worldGuardRegionBypassService = new WorldGuardRegionBypassService(
                plugin, WorldGuardRegionBypassConfig.fromConfig(checksConfig, "ml_check.worldguard"));
        targetWorldGuardRegionBypassService = new WorldGuardRegionBypassService(
                plugin, WorldGuardRegionBypassConfig.fromConfig(checksConfig, "target_ml_check.worldguard"));
    }

    public CustomConfig getChecksConfig() {
        return super.getCustomConfig("anticheat/checks.yml");
    }

    public boolean isAimAiBypassedInRegion(Player player) {
        return worldGuardRegionBypassService != null && worldGuardRegionBypassService.isBypassed(player);
    }

    public boolean isTargetAimAiBypassedInRegion(Player player) {
        return targetWorldGuardRegionBypassService != null && targetWorldGuardRegionBypassService.isBypassed(player);
    }
}
