package ru.gloom.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import ru.gloom.GloomAI;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.api.model.frame.RotationFrame;
import ru.gloom.checks.Check;
import ru.gloom.checks.CheckData;
import ru.gloom.checks.data.RotationData;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;
import ru.gloom.utils.math.RotationRingBuffer;

@Setter
@Getter
@CheckData(name = "AimAI", configName = "ml_check")
public final class AimAI extends Check implements PacketCheck {
    private static final double CHEAT_PROBABILITY = 0.90D;
    private static final double LEGIT_PROBABILITY = 0.10D;

    private final RotationRingBuffer<RotationFrame> rotationBuffer;

    private double bufferFlagThreshold = 50.0D;
    private double bufferResetOnFlag = 25.0D;
    private double bufferMultiplier = 100.0D;
    private double bufferDecrease = 0.25D;

    private List<RotationFrame> lastAnalyzedFrames;
    private double lastProbability;
    private double buffer = 0.0D;

    public AimAI(GloomPlayer player) {
        super(player);
        rotationBuffer = new RotationRingBuffer<>(
                GloomAI.INSTANCE.getChecksConfigManager().getAnalysisSequence());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled()) {
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

        if (GloomAI.INSTANCE.getChecksConfigManager().isAimAiBypassedInRegion(bukkitPlayer)) {
            rotationBuffer.clear();
            return;
        }

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

    public void handleAnalyzeResult(double chance) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline() || !Double.isFinite(chance)) {
            return;
        }

        if (GloomAI.INSTANCE.getChecksConfigManager().isAimAiBypassedInRegion(bukkitPlayer)) {
            rotationBuffer.clear();
            return;
        }

        lastProbability = Math.max(0.0D, Math.min(1.0D, chance));

        GloomAI.INSTANCE.getViolationManager().addAiProbability(bukkitPlayer.getUniqueId(), lastProbability, true);

        updateBuffer(lastProbability);
        GloomAI.INSTANCE
                .getViolationManager()
                .updateAnalysisSnapshot(bukkitPlayer.getUniqueId(), lastProbability, buffer);
        sendAiVerbose();

        GloomAI.INSTANCE.getMonitorManager().publish(bukkitPlayer, lastProbability, buffer);

        if (buffer <= bufferFlagThreshold || !canFlag()) {
            return;
        }

        double bufferAtFlag = buffer;

        registerViolation();
        player.getPunishmentManager()
                .handleViolation(
                        this,
                        "prob: %.4f, buffer: %.2f, check: AimAI".formatted(lastProbability, bufferAtFlag),
                        lastProbability);

        buffer = bufferResetOnFlag;
        GloomAI.INSTANCE
                .getViolationManager()
                .updateAnalysisSnapshot(bukkitPlayer.getUniqueId(), lastProbability, buffer);
    }

    private void updateBuffer(double probability) {
        if (probability > CHEAT_PROBABILITY) {
            buffer += (probability - CHEAT_PROBABILITY) * bufferMultiplier;
        } else if (probability < LEGIT_PROBABILITY) {
            buffer = Math.max(0.0D, buffer - bufferDecrease);
        }
    }

    private void collectFrame(RotationFrame rotationFrame) {
        if (player.getTrainData().isDatasetsCollecting()) {
            player.getTrainData().writeFrame(rotationFrame);
        }

        if (!player.isInCombat()) {
            return;
        }

        rotationBuffer.addFrame(rotationFrame);
        if (rotationBuffer.isFull()) {
            GloomAI.INSTANCE.getAnalyzeService().analyzePlayerFrames(player);
        }
    }

    public void clearFrames() {
        rotationBuffer.clear();
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

        String probability = GloomAI.INSTANCE.getMainConfigManager().getChanceString(lastProbability);
        String verboseTemplate = GloomAI.INSTANCE.getMainConfigManager().getAiVerboseMessage();
        String verboseMessage = verboseTemplate
                .replace("{player}", bukkitPlayer.getName())
                .replace("{check}", "AimAI")
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));
        if (!verboseTemplate.contains("{check}")) {
            verboseMessage += " &7[AimAI]";
        }

        String alertMessage = GloomAI.INSTANCE
                .getMainConfigManager()
                .getAiAlertMessage()
                .replace("{player}", bukkitPlayer.getName())
                .replace("{vl}", String.valueOf((int) getViolations()))
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));

        GloomAI.INSTANCE.getAlertManager().sendVerbose(verboseMessage);
        GloomAI.INSTANCE.getAlertManager().sendAlert(alertMessage);
        GloomAI.INSTANCE
                .getViolationManager()
                .logAlert(this, "prob: %.4f, buffer: %.2f".formatted(lastProbability, buffer));
        return true;
    }

    @Override
    public boolean alert(String verbose) {
        return alert();
    }

    private void sendAiVerbose() {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        String probability = GloomAI.INSTANCE.getMainConfigManager().getChanceString(lastProbability);
        String verboseTemplate = GloomAI.INSTANCE.getMainConfigManager().getAiVerboseMessage();
        String verboseMessage = verboseTemplate
                .replace("{player}", bukkitPlayer.getName())
                .replace("{check}", "AimAI")
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));
        if (!verboseTemplate.contains("{check}")) {
            verboseMessage += " &7[AimAI]";
        }

        GloomAI.INSTANCE.getAlertManager().sendVerbose(verboseMessage);
    }

    public void onReload(CustomConfig config) {
        String path = getConfigName();

        this.bufferFlagThreshold = Math.max(0.0D, config.getDouble(path + ".buffer.flag", 50.0D));
        this.bufferResetOnFlag =
                Math.max(0.0D, Math.min(bufferFlagThreshold, config.getDouble(path + ".buffer.reset_on_flag", 25.0D)));
        this.bufferMultiplier = Math.max(0.0D, config.getDouble(path + ".buffer.multiplier", 100.0D));
        this.bufferDecrease = Math.max(0.0D, config.getDouble(path + ".buffer.decrease", 0.25D));
    }
}
