package ru.gloom.checks.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import ru.gloom.api.model.AbstractCheck;

public interface PacketCheck extends AbstractCheck {
    default void onPacketReceive(final PacketReceiveEvent event) {}

    default void onPacketSend(final PacketSendEvent event) {}
}
