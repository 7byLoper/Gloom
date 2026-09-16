package ru.gloom.manager.anticheat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.List;
import lombok.Getter;
import ru.gloom.api.model.AbstractCheck;
import ru.gloom.checks.data.RotationData;
import ru.gloom.checks.impl.ai.RotationAimCheck;
import ru.gloom.checks.type.PacketCheck;
import ru.gloom.player.GloomPlayer;

@Getter
public class CheckManager {
    private final RotationData rotationData;
    private final RotationAimCheck rotationAimCheck;

    private final ClassToInstanceMap<AbstractCheck> allChecks;
    private final ClassToInstanceMap<PacketCheck> packetChecks;

    private final List<PacketCheck> packetChecksValues;

    public CheckManager(GloomPlayer player) {
        this.rotationData = new RotationData(player);
        this.rotationAimCheck = new RotationAimCheck(player);

        this.packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(RotationData.class, rotationData)
                .put(RotationAimCheck.class, rotationAimCheck)
                .build();

        this.allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .build();

        this.packetChecksValues = List.of(rotationAimCheck);
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
        rotationAimCheck.reload();
    }

    public void clearFrames() {
        rotationAimCheck.clearFrames();
    }
}
