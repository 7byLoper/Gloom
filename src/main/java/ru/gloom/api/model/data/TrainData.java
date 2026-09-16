package ru.gloom.api.model.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import lombok.Data;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.events.EndTrainEvent;
import ru.gloom.api.model.TrainStatisticInfoType;
import ru.gloom.service.train.UploadService;

@Data
public class TrainData {
    private final List<DatasetFrame> datasetFrames;
    private final Object lock = new Object();

    private final UUID playerUuid;
    private final String playerName;
    private UploadService uploadService;
    private volatile DatasetType datasetType;

    private volatile boolean isDatasetsCollecting = false;
    private boolean isMarkedCheater = false;

    private int totalFramesCollected = 0;
    private int datasetsUploaded = 0;

    private long lastCombatTime = 0L;
    private boolean isInCombat = false;

    private UUID datasetsOwner;
    private BossBar trainBar;

    public TrainData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.datasetFrames =
                new ArrayList<>(GloomAI.INSTANCE.getDataCollectConfigManager().getFramesToCollect());
    }

    public boolean isCollecting(DatasetType type) {
        return isDatasetsCollecting && datasetType == type;
    }

    public void writeFrame(DatasetFrame datasetFrame) {
        if (!isInCombat()) {
            return;
        }

        if (datasetFrame.getDatasetType() != datasetType) {
            return;
        }

        boolean shouldWrite;
        synchronized (lock) {
            datasetFrames.add(datasetFrame);
            shouldWrite = datasetFrames.size()
                    >= GloomAI.INSTANCE.getDataCollectConfigManager().getFramesToCollect();
        }

        if (shouldWrite) {
            writeDatasetFrames();
        } else {
            Player owner = Bukkit.getPlayer(this.datasetsOwner);
            if (owner != null) {
                sendTrainStatistic(owner);
            }
        }
    }

    private void sendTrainStatistic(Player owner) {
        if (GloomAI.INSTANCE.getDataCollectConfigManager().getInfoType() == TrainStatisticInfoType.ACTIONBAR) {
            owner.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(generateInfoMessage()));
            return;
        }

        if (trainBar == null) {
            trainBar = Bukkit.createBossBar(
                    generateInfoMessage(),
                    GloomAI.INSTANCE.getDataCollectConfigManager().isAutosaveStatus() ? BarColor.RED : BarColor.GREEN,
                    BarStyle.SEGMENTED_20);
            if (GloomAI.INSTANCE.getDataCollectConfigManager().isAutosaveStatus()) {
                trainBar.setProgress(0);
            }
        }

        if (!trainBar.getPlayers().contains(owner)) {
            trainBar.addPlayer(owner);
        }

        trainBar.setTitle(generateInfoMessage());
        if (!GloomAI.INSTANCE.getDataCollectConfigManager().isAutosaveStatus()) {
            return;
        }

        if (datasetsUploaded <= 0) {
            trainBar.setProgress(0);
            trainBar.setColor(BarColor.RED);
            return;
        }

        double progress = Math.min(
                1.0,
                (double) datasetsUploaded
                        / GloomAI.INSTANCE.getDataCollectConfigManager().getAutosaveValue());
        trainBar.setProgress(progress);

        if (progress >= 0.8) {
            trainBar.setColor(BarColor.GREEN);
        } else if (progress >= 0.4) {
            trainBar.setColor(BarColor.YELLOW);
        } else {
            trainBar.setColor(BarColor.RED);
        }
    }

    private @NotNull String generateInfoMessage() {
        int currentSize;
        synchronized (lock) {
            currentSize = datasetFrames.size();
        }
        return GloomAI.INSTANCE
                .getDataCollectConfigManager()
                .getInfoMessage()
                .replace("{player}", playerName)
                .replace("{frames_collecting}", String.valueOf(totalFramesCollected))
                .replace("{frames_size}", String.valueOf(currentSize))
                .replace("{sending}", String.valueOf(datasetsUploaded));
    }

    public void writeDatasetFrames() {
        List<DatasetFrame> framesCopy;
        synchronized (lock) {
            if (datasetFrames.isEmpty()) {
                return;
            }
            framesCopy = new ArrayList<>(datasetFrames);
            datasetFrames.clear();
        }

        totalFramesCollected += framesCopy.size();

        uploadService
                .uploadPlayerData(framesCopy, isMarkedCheater)
                .whenComplete((success, ex) -> {
                    if (success) {
                        incrementDatasetsUploaded();
                        return;
                    }

                    GloomAI.INSTANCE.getLogger().warning("Не удалось сохранить данные для " + playerName);
                })
                .exceptionally(ex -> {
                    GloomAI.INSTANCE.getLogger().log(Level.SEVERE, "Ошибка сохранения фреймов", ex);
                    return null;
                });
    }

    public void incrementDatasetsUploaded() {
        ++this.datasetsUploaded;
        if (!GloomAI.INSTANCE.getDataCollectConfigManager().isAutosaveStatus()) {
            return;
        }

        if (datasetsUploaded >= GloomAI.INSTANCE.getDataCollectConfigManager().getAutosaveValue()) {
            stopCollecting();
        }
    }

    public void stopCollecting() {
        if (!isDatasetsCollecting) {
            return;
        }

        Player target = Bukkit.getPlayer(this.playerUuid);
        Player owner = this.datasetsOwner == null ? null : Bukkit.getPlayer(this.datasetsOwner);

        int collectedFrames = this.totalFramesCollected;
        int uploadedDatasets = this.datasetsUploaded;
        boolean markedCheater = this.isMarkedCheater;

        setDatasetsCollecting(false);
        setDatasetsUploaded(0);
        setTotalFramesCollected(0);

        synchronized (lock) {
            datasetFrames.clear();
        }

        if (trainBar != null) {
            trainBar.removeAll();
            trainBar.setVisible(false);
            trainBar = null;
        }

        if (owner != null) {
            owner.sendMessage(GloomAI.INSTANCE
                    .getDataCollectConfigManager()
                    .getMessageAllStopAuto()
                    .replace("{target}", playerName));
        }

        if (target != null) {
            Bukkit.getScheduler()
                    .runTask(
                            GloomAI.INSTANCE,
                            () -> new EndTrainEvent(
                                            target, this, owner, markedCheater, collectedFrames, uploadedDatasets)
                                    .callEvent());
        }
    }

    public void tagCombat() {
        this.lastCombatTime = System.currentTimeMillis()
                + GloomAI.INSTANCE.getDataCollectConfigManager().getCombatTimer();
        this.isInCombat = true;
    }

    public boolean isInCombat() {
        boolean wasInCombat = this.isInCombat;

        if (System.currentTimeMillis() > this.lastCombatTime) {
            this.isInCombat = false;
        }

        if (wasInCombat
                && !this.isInCombat
                && GloomAI.INSTANCE.getDataCollectConfigManager().isClearBufferOnCombatTimeout()) {
            synchronized (lock) {
                datasetFrames.clear();
            }
        }

        return this.isInCombat;
    }
}
