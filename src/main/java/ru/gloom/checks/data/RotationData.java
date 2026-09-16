package ru.gloom.checks.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import ru.gloom.checks.Check;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;
import ru.gloom.utils.StatisticsUtils;
import ru.gloom.utils.math.GraphUtil;
import ru.gloom.utils.math.MouseCalculator;
import ru.gloom.utils.packet.PacketUtil;

@Getter
public class RotationData extends Check implements PacketCheck {

    private final List<Double> yawSamples = new ArrayList<>();
    private final List<Double> pitchSamples = new ArrayList<>();
    private float yaw;
    private float pitch;

    private float lastYaw;
    private float lastPitch;

    private float deltaYaw;
    private float deltaPitch;

    private float lastDeltaYaw;
    private float lastDeltaPitch;

    private float lastLastDeltaYaw;
    private float lastLastDeltaPitch;

    private float accelYaw;
    private float accelPitch;

    private float lastAccelYaw;
    private float lastAccelPitch;

    private float jerkYaw;
    private float jerkPitch;

    private float gcdErrorYaw;
    private float gcdErrorPitch;

    private long lastSmooth = 0L;
    private long lastHighRate = 0L;

    private double lastDeltaXRot = 0.0;
    private double lastDeltaYRot = 0.0;

    private int isTotallyNotCinematic = 0;
    private boolean cinematicRotation = false;
    private boolean updated;

    public RotationData(GloomPlayer gloomPlayer) {
        super(gloomPlayer);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        updated = false;
        if (PacketUtil.isRotation(event)) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            if (!Float.isFinite(wrapper.getYaw()) || !Float.isFinite(wrapper.getPitch())) {
                return;
            }
            updateRotation(wrapper.getYaw(), wrapper.getPitch());
        }
    }

    private void updateRotation(float newYaw, float newPitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw = MouseCalculator.normalizeAngle(newYaw);
        pitch = Math.max(-90.0F, Math.min(90.0F, newPitch));

        float newDeltaYaw = MouseCalculator.normalizeAngle(yaw - lastYaw);
        float newDeltaPitch = pitch - lastPitch;

        lastLastDeltaYaw = lastDeltaYaw;
        lastLastDeltaPitch = lastDeltaPitch;

        lastDeltaYaw = deltaYaw;
        lastDeltaPitch = deltaPitch;

        deltaYaw = newDeltaYaw;
        deltaPitch = newDeltaPitch;

        lastAccelYaw = accelYaw;
        lastAccelPitch = accelPitch;
        accelYaw = MouseCalculator.calculateAcceleration(deltaYaw, lastDeltaYaw);
        accelPitch = MouseCalculator.calculateAcceleration(deltaPitch, lastDeltaPitch);
        jerkYaw = MouseCalculator.calculateJerk(accelYaw, lastAccelYaw);
        jerkPitch = MouseCalculator.calculateJerk(accelPitch, lastAccelPitch);
        gcdErrorYaw = MouseCalculator.calculateGCDError(deltaYaw, lastDeltaYaw);
        gcdErrorPitch = MouseCalculator.calculateGCDError(deltaPitch, lastDeltaPitch);

        processCinematic();
        updated = true;
    }

    private void processCinematic() {
        long now = System.currentTimeMillis();

        double differenceYaw = Math.abs(deltaYaw - lastDeltaXRot);
        double differencePitch = Math.abs(deltaPitch - lastDeltaYRot);

        double joltYaw = Math.abs(differenceYaw - deltaYaw);
        double joltPitch = Math.abs(differencePitch - deltaPitch);

        boolean cinematic = (now - lastHighRate > 250L) || (now - lastSmooth < 9000L);

        if (joltYaw > 1.0 && joltPitch > 1.0) {
            lastHighRate = now;
        }

        yawSamples.add((double) deltaYaw);
        pitchSamples.add((double) deltaPitch);

        if (yawSamples.size() >= 20 && pitchSamples.size() >= 20) {
            Set<Double> shannonYaw = new HashSet<>();
            Set<Double> shannonPitch = new HashSet<>();
            List<Double> stackYaw = new ArrayList<>();
            List<Double> stackPitch = new ArrayList<>();

            for (Double yawSample : yawSamples) {
                stackYaw.add(yawSample);
                if (stackYaw.size() >= 10) {
                    shannonYaw.add(StatisticsUtils.getShannonEntropy(stackYaw));
                    stackYaw.clear();
                }
            }

            for (Double pitchSample : pitchSamples) {
                stackPitch.add(pitchSample);
                if (stackPitch.size() >= 10) {
                    shannonPitch.add(StatisticsUtils.getShannonEntropy(stackPitch));
                    stackPitch.clear();
                }
            }

            if (shannonYaw.size() != 1
                    || shannonPitch.size() != 1
                    || !shannonYaw.toArray()[0].equals(shannonPitch.toArray()[0])) {
                isTotallyNotCinematic = 20;
            }

            var resultsYaw = GraphUtil.getGraph(yawSamples);
            var resultsPitch = GraphUtil.getGraph(pitchSamples);

            int negativesYaw = resultsYaw.getNegatives();
            int negativesPitch = resultsPitch.getNegatives();
            int positivesYaw = resultsYaw.getPositives();
            int positivesPitch = resultsPitch.getPositives();

            if (positivesYaw > negativesYaw || positivesPitch > negativesPitch) {
                lastSmooth = now;
            }

            yawSamples.clear();
            pitchSamples.clear();
        }

        if (isTotallyNotCinematic > 0) {
            isTotallyNotCinematic--;
            cinematicRotation = false;
        } else {
            cinematicRotation = cinematic;
        }

        lastDeltaXRot = deltaYaw;
        lastDeltaYRot = deltaPitch;
    }

    @Override
    public void reload() {}
}
