package ru.gloom.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import ru.gloom.GloomAI;
import ru.gloom.api.configuration.CustomConfig;
import ru.gloom.api.model.frame.TargetAimFrame;
import ru.gloom.checks.Check;
import ru.gloom.checks.CheckData;
import ru.gloom.checks.data.RotationData;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;
import ru.gloom.utils.math.MouseCalculator;
import ru.gloom.utils.math.RotationRingBuffer;

@Setter
@Getter
@CheckData(name = "TargetAimAI", configName = "target_ml_check")
public final class TargetAimAI extends Check implements PacketCheck {
    private static final double CHEAT_PROBABILITY = 0.90D;
    private static final double LEGIT_PROBABILITY = 0.10D;

    private static final float MIN_ANGULAR_SPAN = 0.10F;
    private static final float MAX_NORMALIZED_ERROR = 8.0F;

    private final RotationRingBuffer<TargetAimFrame> targetAimBuffer;

    private UUID trackedTargetUuid;
    private boolean hasTargetHistory;

    private float lastTargetYawErrorNorm;
    private float lastTargetPitchErrorNorm;

    private double bufferFlagThreshold = 50.0D;
    private double bufferResetOnFlag = 25.0D;
    private double bufferMultiplier = 100.0D;
    private double bufferDecrease = 0.25D;

    private List<TargetAimFrame> lastAnalyzedFrames;
    private double lastProbability;
    private double buffer;

    public TargetAimAI(GloomPlayer player) {
        super(player);
        targetAimBuffer = new RotationRingBuffer<>(
                GloomAI.INSTANCE.getChecksConfigManager().getTargetAnalysisSequence());
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

        if (GloomAI.INSTANCE.getChecksConfigManager().isTargetAimAiBypassedInRegion(bukkitPlayer)) {
            resetTargetState();
            targetAimBuffer.clear();
            return;
        }

        UUID lastTargetUuid = player.getLastTargetUuid();
        if (lastTargetUuid == null) {
            resetTargetState();
            targetAimBuffer.clear();
            return;
        }

        Entity target = GloomAI.INSTANCE.getTargetEntityIndex().getByUniqueId(lastTargetUuid);
        if (target == null || !target.isValid() || !target.getWorld().equals(bukkitPlayer.getWorld())) {
            resetTargetState();
            targetAimBuffer.clear();
            return;
        }

        UUID currentTargetUuid = target.getUniqueId();
        if (!currentTargetUuid.equals(trackedTargetUuid)) {
            resetTargetState();
            trackedTargetUuid = currentTargetUuid;
        }

        float currentYaw = rotationData.getYaw();
        float currentPitch = rotationData.getPitch();

        Location eye = bukkitPlayer.getEyeLocation();
        BoundingBox box = target.getBoundingBox();

        double centerX = (box.getMinX() + box.getMaxX()) * 0.5D;
        double centerY = (box.getMinY() + box.getMaxY()) * 0.5D;
        double centerZ = (box.getMinZ() + box.getMaxZ()) * 0.5D;

        float targetYawError = signedDelta(targetYaw(eye, centerX, centerZ), currentYaw);
        float targetPitchError = targetPitch(eye, centerX, centerY, centerZ) - currentPitch;

        HitboxFeatures hitbox =
                calculateHitboxFeatures(eye, box, currentYaw, currentPitch, targetYawError, targetPitchError);

        float targetErrorDeltaYaw = hasTargetHistory ? hitbox.normalizedYawError() - lastTargetYawErrorNorm : 0.0F;
        float targetErrorDeltaPitch =
                hasTargetHistory ? hitbox.normalizedPitchError() - lastTargetPitchErrorNorm : 0.0F;
        float targetDistance = (float)
                Math.sqrt(square(centerX - eye.getX()) + square(centerY - eye.getY()) + square(centerZ - eye.getZ()));

        TargetAimFrame frame = new TargetAimFrame(
                rotationData.getDeltaYaw(),
                rotationData.getDeltaPitch(),
                rotationData.getAccelYaw(),
                rotationData.getAccelPitch(),
                hitbox.normalizedYawError(),
                hitbox.normalizedPitchError(),
                targetErrorDeltaYaw,
                targetErrorDeltaPitch,
                targetDistance,
                player.getTicksSinceAttackFeature());

        updateTargetState(currentTargetUuid, hitbox.normalizedYawError(), hitbox.normalizedPitchError());
        collectFrame(frame);
    }

    public void handleAnalyzeResult(double probability) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline() || !Double.isFinite(probability)) {
            return;
        }

        if (GloomAI.INSTANCE.getChecksConfigManager().isTargetAimAiBypassedInRegion(bukkitPlayer)) {
            targetAimBuffer.clear();
            resetTargetState();
            return;
        }

        lastProbability = Math.max(0.0D, Math.min(1.0D, probability));
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

        registerViolation();
        player.getPunishmentManager()
                .handleViolation(
                        this,
                        "prob: %.4f, buffer: %.2f, check: TargetAimAI".formatted(lastProbability, buffer),
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

    private void sendAiVerbose() {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        String probability = GloomAI.INSTANCE.getMainConfigManager().getChanceString(lastProbability);
        String verboseTemplate = GloomAI.INSTANCE.getMainConfigManager().getAiVerboseMessage();
        String verboseMessage = verboseTemplate
                .replace("{player}", bukkitPlayer.getName())
                .replace("{check}", "TargetAimAI")
                .replace("{probability}", probability)
                .replace("{buffer}", "%.2f".formatted(buffer));
        if (!verboseTemplate.contains("{check}")) {
            verboseMessage += " &7[TargetAimAI]";
        }

        GloomAI.INSTANCE.getAlertManager().sendVerbose(verboseMessage);
    }

    private void collectFrame(TargetAimFrame targetAimFrame) {
        if (player.getTrainData().isDatasetsCollecting()) {
            player.getTrainData().writeFrame(targetAimFrame);
        }

        if (!player.isInCombat()) {
            return;
        }

        targetAimBuffer.addFrame(targetAimFrame);
        if (targetAimBuffer.isFull()) {
            GloomAI.INSTANCE.getTargetAnalyzeService().analyzePlayerFrames(player);
        }
    }

    public void clearFrames() {
        targetAimBuffer.clear();
        resetTargetState();
    }

    private HitboxFeatures calculateHitboxFeatures(
            Location eye,
            BoundingBox box,
            float currentYaw,
            float currentPitch,
            float centerYawError,
            float centerPitchError) {
        double[] xs = {box.getMinX(), box.getMaxX()};
        double[] ys = {box.getMinY(), box.getMaxY()};
        double[] zs = {box.getMinZ(), box.getMaxZ()};

        float minYaw = Float.POSITIVE_INFINITY;
        float maxYaw = Float.NEGATIVE_INFINITY;
        float minPitch = Float.POSITIVE_INFINITY;
        float maxPitch = Float.NEGATIVE_INFINITY;

        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    float yawError = signedDelta(targetYaw(eye, x, z), currentYaw);
                    yawError = alignAround(yawError, centerYawError);
                    float pitchError = targetPitch(eye, x, y, z) - currentPitch;

                    minYaw = Math.min(minYaw, yawError);
                    maxYaw = Math.max(maxYaw, yawError);
                    minPitch = Math.min(minPitch, pitchError);
                    maxPitch = Math.max(maxPitch, pitchError);
                }
            }
        }

        if (!Float.isFinite(minYaw)
                || !Float.isFinite(maxYaw)
                || !Float.isFinite(minPitch)
                || !Float.isFinite(maxPitch)) {
            minYaw = centerYawError;
            maxYaw = centerYawError;
            minPitch = centerPitchError;
            maxPitch = centerPitchError;
        }

        float yawSpan = Math.max(0.0F, maxYaw - minYaw);
        float pitchSpan = Math.max(0.0F, maxPitch - minPitch);

        float normalizedYawError =
                clampNormalized(centerYawError / Math.max(Math.abs(yawSpan) * 0.5F, MIN_ANGULAR_SPAN));
        float normalizedPitchError =
                clampNormalized(centerPitchError / Math.max(Math.abs(pitchSpan) * 0.5F, MIN_ANGULAR_SPAN));

        return new HitboxFeatures(normalizedYawError, normalizedPitchError);
    }

    private float clampNormalized(float value) {
        return Math.max(-MAX_NORMALIZED_ERROR, Math.min(MAX_NORMALIZED_ERROR, value));
    }

    private float alignAround(float value, float reference) {
        float result = value;
        while (result - reference > 180.0F) {
            result -= 360.0F;
        }
        while (result - reference < -180.0F) {
            result += 360.0F;
        }
        return result;
    }

    private double square(double value) {
        return value * value;
    }

    private void updateTargetState(UUID targetUuid, float targetYawErrorNorm, float targetPitchErrorNorm) {
        trackedTargetUuid = targetUuid;
        lastTargetYawErrorNorm = targetYawErrorNorm;
        lastTargetPitchErrorNorm = targetPitchErrorNorm;
        hasTargetHistory = true;
    }

    private void resetTargetState() {
        trackedTargetUuid = null;
        hasTargetHistory = false;
        lastTargetYawErrorNorm = 0.0F;
        lastTargetPitchErrorNorm = 0.0F;
    }

    private float targetYaw(Location playerLocation, double targetX, double targetZ) {
        return normalizeYaw(
                (float) Math.toDegrees(Math.atan2(playerLocation.getX() - targetX, targetZ - playerLocation.getZ())));
    }

    private float targetPitch(Location playerLocation, double targetX, double targetY, double targetZ) {
        double x = targetX - playerLocation.getX();
        double y = targetY - playerLocation.getY();
        double z = targetZ - playerLocation.getZ();
        return clampPitch((float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z))));
    }

    private float normalizeYaw(float value) {
        return MouseCalculator.normalizeAngle(value);
    }

    private float clampPitch(float value) {
        return Math.max(-90.0F, Math.min(90.0F, value));
    }

    private float signedDelta(float current, float previous) {
        return MouseCalculator.normalizeAngle(current - previous);
    }

    public void onReload(CustomConfig config) {
        String path = getConfigName();
        bufferFlagThreshold = Math.max(0.0D, config.getDouble(path + ".buffer.flag", 50.0D));
        bufferResetOnFlag =
                Math.max(0.0D, Math.min(bufferFlagThreshold, config.getDouble(path + ".buffer.reset_on_flag", 25.0D)));
        bufferMultiplier = Math.max(0.0D, config.getDouble(path + ".buffer.multiplier", 100.0D));
        bufferDecrease = Math.max(0.0D, config.getDouble(path + ".buffer.decrease", 0.25D));
    }

    private record HitboxFeatures(float normalizedYawError, float normalizedPitchError) {}
}
