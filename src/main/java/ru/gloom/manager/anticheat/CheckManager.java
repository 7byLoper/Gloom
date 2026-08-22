package ru.gloom.manager.anticheat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.List;
import lombok.Getter;
import ru.gloom.api.model.AbstractCheck;
import ru.gloom.checks.data.RotationData;
import ru.gloom.checks.impl.ai.AimAI;
import ru.gloom.checks.impl.ai.TargetAimAI;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;

@Getter
public class CheckManager {
    private final RotationData rotationData;
    private final AimAI aimAI;
    private final TargetAimAI targetAimAI;

    private final ClassToInstanceMap<AbstractCheck> allChecks;
    private final ClassToInstanceMap<PacketCheck> packetChecks;

    private final List<PacketCheck> packetChecksValues;

    public CheckManager(GloomPlayer player) {
        this.rotationData = new RotationData(player);
        this.aimAI = new AimAI(player);
        this.targetAimAI = new TargetAimAI(player);

        this.packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(RotationData.class, rotationData)
                .put(AimAI.class, aimAI)
                .put(TargetAimAI.class, targetAimAI)
                .build();

        this.allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .build();

        this.packetChecksValues = List.of(aimAI, targetAimAI);
    }

    public void onPacketSend(PacketSendEvent packet) {
        for (PacketCheck check : packetChecksValues) {
            check.onPacketSend(packet);
        }
    }

    public void onPacketReceive(PacketReceiveEvent packet) {
        rotationData.onPacketReceive(packet);
        for (PacketCheck check : packetChecksValues) {
            check.onPacketReceive(packet);
        }
    }

    public void reload() {
        rotationData.reload();
        aimAI.reload();
        targetAimAI.reload();
    }

    public void clearFrames() {
        aimAI.clearFrames();
        targetAimAI.clearFrames();
    }
}
