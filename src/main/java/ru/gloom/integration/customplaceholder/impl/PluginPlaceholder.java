package ru.gloom.integration.customplaceholder.impl;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import ru.gloom.GloomAI;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.config.MainConfigManager;
import ru.gloom.integration.customplaceholder.PlaceholderIntegration;
import ru.gloom.utils.StringColorize;

public class PluginPlaceholder implements PlaceholderIntegration {
    private Plugin plugin;

    @Override
    public void init(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlaceholder(String path) {
        FileConfiguration settings = settings();
        return StringColorize.parse(settings == null ? null : settings.getString("placeholder." + path));
    }

    @Override
    public String getPlaceholder(String path, String def) {
        FileConfiguration settings = settings();
        return StringColorize.parse(settings == null ? def : settings.getString("placeholder." + path, def));
    }

    private FileConfiguration settings() {
        if (!(plugin instanceof GloomAI gloomAI)) {
            return null;
        }

        MainConfigManager configManager = gloomAI.getMainConfigManager();
        if (configManager == null) {
            return null;
        }

        CustomConfig config = configManager.getCustomConfig("settings.yml");
        return config == null ? null : config.getConfig();
    }
}
