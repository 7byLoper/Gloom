package ru.gloom.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import ru.gloom.GloomAI;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.api.model.data.DatasetType;
import ru.gloom.api.model.frame.RotationFrame;
import ru.gloom.checks.Check;
import ru.gloom.checks.CheckData;
import ru.gloom.checks.data.RotationData;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;
import ru.gloom.utils.math.RotationRingBuffer;

@Setter
@Getter
@CheckData(name = "RotationAimCheck", configName = "ml_check")
public final class RotationAimCheck extends Check implements PacketCheck {
    private static final double CHEAT_PROBABILITY = 0.90D;

    private final RotationRingBuffer<RotationFrame> rotationBuffer;

    private double bufferFlagThreshold = 50.0D;
    private double bufferResetOnFlag = 25.0D;
    private double bufferMultiplier = 100.0D;
    private double bufferDecrease = 8.0D;

    private volatile long sequenceId;
    private boolean combatGateClosed;
    private List<RotationFrame> lastAnalyzedFrames;
    private double lastProbability;
    private double buffer = 0.0D;
    private String lastModel;

    public RotationAimCheck(GloomPlayer player) {
        super(player);
        rotationBuffer = new RotationRingBuffer<>(
                GloomAI.INSTANCE.getChecksConfigManager().getAnalysisSequence());
    }

    @Override
    public boolean isEnabled() {
        return GloomAI.INSTANCE.getChecksConfigManager().isRotationAiEnabled();
    }

    @Override
    public synchronized void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() && !isCollectingDataset()) {
            return;
        }

        if (player.getTrainData().isDatasetsCollecting() && !isCollectingDataset()) {
            clearFrames();
            return;
        }
        RotationData rotationData = player.getCheckManager().getRotationData();
        if (!rotationData.isUpdated()) {
            return;
        }

        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        if (isBypassed() || GloomAI.INSTANCE.getChecksConfigManager().isAimAiBypassedInRegion(bukkitPlayer)) {
            clearFrames();
            return;
        }

        if (!isCollectingDataset() && !player.isCombatActive()) {
            if (!combatGateClosed) {
                combatGateClosed = true;
                clearSequence();
            }
            return;
        }
        combatGateClosed = false;

        RotationFrame rotationFrame = new RotationFrame(
                rotationData.getDeltaYaw(),
                rotationData.getDeltaPitch(),
                rotationData.getAccelYaw(),
                rotationData.getAccelPitch(),
                rotationData.getJerkYaw(),
                rotationData.getJerkPitch(),
                rotationData.getGcdErrorYaw(),
                rotationData.getGcdErrorPitch());

        collectFrame(rotationFrame);
    }

    public synchronized void handleAnalyzeResult(double chance) {
        if (!isRequestCurrent(sequenceId)) {
            return;
        }
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline() || !Double.isFinite(chance)) {
            return;
        }

        if (isCollectingDataset()) {
            return;
        }

        if (isBypassed() || GloomAI.INSTANCE.getChecksConfigManager().isAimAiBypassedInRegion(bukkitPlayer)) {
            clearFrames();
            return;
        }

        lastProbability = Math.max(0.0D, Math.min(1.0D, chance));

        GloomAI.INSTANCE.getViolationManager().addAiProbability(bukkitPlayer.getUniqueId(), lastProbability, true);

        updateBuffer(lastProbability);
        GloomAI.INSTANCE
                .getViolationManager()
                .updateAnalysisSnapshot(bukkitPlayer.getUniqueId(), lastProbability, buffer);
        sendVerbose();

        GloomAI.INSTANCE.getMonitorManager().publish(bukkitPlayer, lastProbability, buffer);

        if (buffer <= bufferFlagThreshold || !canFlag()) {
            return;
        }

        double bufferAtFlag = buffer;

        registerViolation();
        player.getPunishmentManager()
                .handleViolation(
                        this,
                        "prob: %.4f, buffer: %.2f, model: %s".formatted(lastProbability, bufferAtFlag, getEffectiveModel()),
                        lastProbability);

        buffer = bufferResetOnFlag;
        GloomAI.INSTANCE
                .getViolationManager()
                .updateAnalysisSnapshot(bukkitPlayer.getUniqueId(), lastProbability, buffer);
    }

    private void updateBuffer(double probability) {
        if (probability > CHEAT_PROBABILITY) {
            buffer += (probability - CHEAT_PROBABILITY) * bufferMultiplier;
            return;
        }

        buffer = Math.max(0.0D, buffer - (CHEAT_PROBABILITY - probability) * bufferDecrease);
    }

    private void collectFrame(RotationFrame rotationFrame) {
        if (isCollectingDataset()) {
            player.getTrainData().writeFrame(rotationFrame);
            if (rotationBuffer.size() > 0) {
                clearFrames();
            }
            return;
        }

        rotationBuffer.addFrame(rotationFrame);
        if (rotationBuffer.isFull()) {
            GloomAI.INSTANCE.getAnalyzeService().analyzePlayerFrames(player);
        }
    }

    private boolean isCollectingDataset() {
        return player.getTrainData().isCollecting(DatasetType.ROTATION);
    }

    public synchronized void clearFrames() {
        clearSequence();
        buffer = 0.0D;
    }

    private synchronized void clearSequence() {
        sequenceId++;
        rotationBuffer.clear();
    }

    public boolean isRequestCurrent(long sequence) {
        return sequence == sequenceId && isEnabled() && !player.getTrainData().isDatasetsCollecting();
    }

    public synchronized void handleAnalyzeResult(double chance, long sequence) {
        if (isRequestCurrent(sequence)) {
            handleAnalyzeResult(chance);
        }
    }

    public synchronized void handleAnalyzeResult(double chance, long sequence, String model) {
        if (isRequestCurrent(sequence)) {
            if (model != null && !model.isBlank()) {
                this.lastModel = model;
            }
            handleAnalyzeResult(chance);
        }
    }

    public String getEffectiveModel() {
        if (lastModel != null && !lastModel.isBlank()) {
            return lastModel;
        }
        String dispatcherModel = GloomAI.INSTANCE.getAnalyzeBatchDispatcher().getLastModelName();
        if (dispatcherModel != null && !dispatcherModel.isBlank()) {
            return dispatcherModel;
        }
        return "TCN";
    }

    @Override
    public boolean alert() {
        if (!canAlert()) {
            return false;
        }

        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return false;
        }

        String modelName = getEffectiveModel();
        String probability = GloomAI.INSTANCE.getMainConfigManager().getChanceString(lastProbability);
        String verboseTemplate = GloomAI.INSTANCE.getMainConfigManager().getVerboseMessage();
        String verboseMessage = verboseTemplate
                .replace("{player}", bukkitPlayer.getName())
                .replace("{model}", modelName)
                .replace("{check}", modelName)
                .replace("RotationAimCheck", modelName)
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));

        String alertMessage = GloomAI.INSTANCE
                .getMainConfigManager()
                .getAlertMessage()
                .replace("{player}", bukkitPlayer.getName())
                .replace("{model}", modelName)
                .replace("{check}", modelName)
                .replace("RotationAimCheck", modelName)
                .replace("{vl}", String.valueOf((int) getViolations()))
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));

        GloomAI.INSTANCE.getAlertManager().sendVerbose(verboseMessage);
        GloomAI.INSTANCE.getAlertManager().sendAlert(alertMessage);
        GloomAI.INSTANCE
                .getViolationManager()
                .logAlert(this, "prob: %.4f, buffer: %.2f, model: %s".formatted(lastProbability, buffer, modelName));
        return true;
    }

    @Override
    public boolean alert(String verbose) {
        return alert();
    }

    private void sendVerbose() {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        String modelName = getEffectiveModel();
        String probability = GloomAI.INSTANCE.getMainConfigManager().getChanceString(lastProbability);
        String verboseTemplate = GloomAI.INSTANCE.getMainConfigManager().getVerboseMessage();
        String verboseMessage = verboseTemplate
                .replace("{player}", bukkitPlayer.getName())
                .replace("{model}", modelName)
                .replace("{check}", modelName)
                .replace("RotationAimCheck", modelName)
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));

        GloomAI.INSTANCE.getAlertManager().sendVerbose(verboseMessage);
    }

    public void onReload(CustomConfig config) {
        String path = getConfigName();
        if (rotationBuffer != null) {
            clearFrames();
        }

        this.bufferFlagThreshold = Math.max(0.0D, config.getDouble(path + ".buffer.flag", 50.0D));
        this.bufferResetOnFlag =
                Math.max(0.0D, Math.min(bufferFlagThreshold, config.getDouble(path + ".buffer.reset_on_flag", 25.0D)));
        this.bufferMultiplier = Math.max(0.0D, config.getDouble(path + ".buffer.multiplier", 100.0D));
        this.bufferDecrease = Math.max(0.0D, config.getDouble(path + ".buffer.decrease", 8.0D));
    }
}
