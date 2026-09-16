package ru.gloom;

import java.lang.reflect.Constructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.gloom.api.itemstack.ItemStackServices;
import ru.gloom.api.model.analyze.AnalyzeService;
import ru.gloom.api.redis.RedisManager;
import ru.gloom.command.CommandManager;
import ru.gloom.config.*;
import ru.gloom.config.anticheat.ChecksConfigManager;
import ru.gloom.config.anticheat.HologramConfigManager;
import ru.gloom.config.anticheat.PunishmentConfigManager;
import ru.gloom.config.datacollect.DataCollectConfigManager;
import ru.gloom.integration.customplaceholder.PlaceholderIntegration;
import ru.gloom.integration.customplaceholder.impl.PluginPlaceholder;
import ru.gloom.listeners.bukkit.MenuListener;
import ru.gloom.manager.*;
import ru.gloom.manager.alert.AlertManager;
import ru.gloom.manager.alert.ViolationManager;
import ru.gloom.manager.analytic.MonitorManager;
import ru.gloom.manager.analytic.hologram.HologramManager;
import ru.gloom.manager.anticheat.PlayerDataManager;
import ru.gloom.service.PlayerOnlineService;
import ru.gloom.service.analyze.AnalyzeBatchDispatcher;
import ru.gloom.service.analyze.FlatBufferAnalyzeService;
import ru.gloom.utils.entity.TargetEntityIndex;
import ru.gloom.utils.entity.TargetEntityIndexListener;

@Getter
public class GloomAI extends JavaPlugin {

    public static GloomAI INSTANCE;

    private PlayerDataManager playerDataManager;
    private PlaceholderIntegration placeholderIntegration;

    private MainConfigManager mainConfigManager;
    private ChecksConfigManager checksConfigManager;
    private DataCollectConfigManager dataCollectConfigManager;
    private HologramConfigManager hologramConfigManager;
    private PunishmentConfigManager punishmentConfigManager;

    private AIResultManager aiResultManager;
    private AnalyzeService analyzeService;

    private ViolationManager violationManager;
    private AlertManager alertManager;

    private HologramManager hologramManager;
    private MonitorManager monitorManager;

    private AnalyzeBatchDispatcher analyzeBatchDispatcher;
    private CommandManager commandManager;

    private TargetEntityIndex targetEntityIndex;
    private PacketManager packetManager;

    private RedisManager redisManager;
    private PlayerOnlineService playerOnlineService;

    @Override
    public void onEnable() {
        INSTANCE = this;

        this.packetManager = new PacketManager();

        this.playerDataManager = new PlayerDataManager();

        this.placeholderIntegration = new PluginPlaceholder();
        placeholderIntegration.init(this);

        this.targetEntityIndex = new TargetEntityIndex();

        this.mainConfigManager = new MainConfigManager(this);
        this.checksConfigManager = new ChecksConfigManager(this);
        this.dataCollectConfigManager = new DataCollectConfigManager(this);
        this.hologramConfigManager = new HologramConfigManager(this);
        this.punishmentConfigManager = new PunishmentConfigManager(this);
        initRedis();

        this.aiResultManager = new AIResultManager();
        this.analyzeBatchDispatcher = new AnalyzeBatchDispatcher(
                this, checksConfigManager::getAnalyzeServer);
        this.analyzeBatchDispatcher.start();
        this.analyzeService = new FlatBufferAnalyzeService(checksConfigManager, analyzeBatchDispatcher);
        this.alertManager = new AlertManager(mainConfigManager);
        this.violationManager = new ViolationManager();

        getServer().getPluginManager().registerEvents(new TargetEntityIndexListener(targetEntityIndex), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        targetEntityIndex.initialize();

        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
        this.commandManager.registerWrappers();

        this.hologramManager = new HologramManager();
        this.hologramManager.start();

        this.monitorManager = new MonitorManager(this, mainConfigManager);

        this.playerDataManager.loadOnlinePlayers();

        ItemStackServices.setSkullHead(createSkullHead());

        initRedis();

        packetManager.register();
    }

    private void initRedis() {
        redisManager = new RedisManager(mainConfigManager.getRedisConfig(), null);
        playerOnlineService = new PlayerOnlineService(redisManager, 60000L);
        Bukkit.getScheduler()
                .runTaskTimerAsynchronously(
                        this,
                        () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                playerOnlineService.heartbeat(player.getUniqueId(), player.getName());
                            }

                            playerOnlineService.cleanup();
                        },
                        20L * 10L,
                        20L * 10L);
    }

    @Override
    public void onDisable() {
        if (packetManager != null) {
            packetManager.unregister();
        }
        if (analyzeBatchDispatcher != null) {
            analyzeBatchDispatcher.stop();
        }
        if (violationManager != null) {
            violationManager.shutdown();
        }
        if (hologramManager != null) {
            hologramManager.stop();
        }
        if (monitorManager != null) {
            monitorManager.shutdown();
        }
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        try {
            CommandMap commandMap = getServer().getCommandMap();
            Constructor<PluginCommand> constructor =
                    PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            PluginCommand command = constructor.newInstance(commandName, this);

            command.setExecutor(executor);
            commandMap.register(getDescription().getName(), command);
        } catch (Exception exception) {
            getLogger().severe("Unable to register command: " + commandName + ". Error: " + exception.getMessage());
        }
    }

    private ItemStack createSkullHead() {
        return new ItemStack(Material.PLAYER_HEAD, 1);
    }

    public static String serverId() {
        return INSTANCE.placeholderIntegration.getPlaceholder("server_id", "unknown");
    }

    public static String serverName() {
        return INSTANCE.placeholderIntegration.getPlaceholder("server_name", "unknown");
    }
}
