package ru.gloom.player;

import com.github.retrooper.packetevents.protocol.player.User;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.gloom.GloomAI;
import ru.gloom.api.events.StartTrainEvent;
import ru.gloom.api.model.data.DatasetType;
import ru.gloom.api.model.data.TrainData;
import ru.gloom.manager.anticheat.CheckManager;
import ru.gloom.manager.anticheat.PunishmentManager;
import ru.gloom.service.train.UploadService;

@Getter
@Setter
public class GloomPlayer {
    private final UUID uuid;
    private final String name;
    private final User user;

    private final CheckManager checkManager;
    private final PunishmentManager punishmentManager;

    private Player bukkitPlayer;

    private volatile long lastCombatTime = 0L;
    private boolean isInCombat = false;

    public GloomPlayer(User user) {
        this.user = user;
        this.name = user.getName();
        this.uuid = user.getUUID();
        this.bukkitPlayer = Bukkit.getPlayer(uuid);

        this.checkManager = new CheckManager(this);
        this.punishmentManager = new PunishmentManager(GloomAI.INSTANCE, GloomAI.INSTANCE.getPunishmentConfigManager());
    }

    public void startDatasetsCollection(String name, boolean cheater, DatasetType datasetType) {
        Player target = getBukkitPlayer();
        if (target == null) {
            return;
        }

        TrainData trainData = getTrainData();
        if (trainData.isDatasetsCollecting()) {
            return;
        }

        String datasetName = name + "_" + UUID.randomUUID().toString().substring(0, 6);
        UploadService uploadService =
                new UploadService(GloomAI.INSTANCE, datasetName, "datasets/" + this.name, datasetType);

        trainData.setUploadService(uploadService);
        trainData.setDatasetType(datasetType);
        trainData.setDatasetsCollecting(true);
        trainData.setMarkedCheater(cheater);

        checkManager.clearFrames();

        Player owner = trainData.getDatasetsOwner() == null ? null : Bukkit.getPlayer(trainData.getDatasetsOwner());

        Bukkit.getPluginManager().callEvent(new StartTrainEvent(target, trainData, owner, cheater, datasetName));
    }

    public void stopDatasetsCollection() {
        TrainData trainData = getTrainData();
        trainData.writeDatasetFrames();
        trainData.stopCollecting();
    }

    public void tagCombat() {
        getTrainData().tagCombat();

        this.lastCombatTime = System.currentTimeMillis()
                + GloomAI.INSTANCE.getChecksConfigManager().getCombatTimer();
        this.isInCombat = true;
    }

    public boolean isCombatActive() {
        return System.currentTimeMillis() <= lastCombatTime;
    }

    public TrainData getTrainData() {
        return GloomAI.INSTANCE.getPlayerDataManager().getOrCreateTrainData(uuid, name);
    }

    public boolean isInCombat() {
        boolean wasInCombat = this.isInCombat;

        if (System.currentTimeMillis() > this.lastCombatTime) {
            this.isInCombat = false;
        }

        if (wasInCombat && !this.isInCombat) {
            checkManager.clearFrames();
        }

        return this.isInCombat;
    }

    public Player getBukkitPlayer() {
        if (bukkitPlayer != null) {
            return bukkitPlayer;
        }

        return bukkitPlayer = Bukkit.getPlayer(uuid);
    }

    public void reload() {
        checkManager.reload();
        punishmentManager.reload();
    }
}
