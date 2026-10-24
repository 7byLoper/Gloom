package ru.gloom.listeners.packets;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import ru.gloom.GloomAI;
import ru.gloom.player.GloomPlayer;

public final class CheckManagerListener extends PacketListenerAbstract {
    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        GloomPlayer player = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) {
            return;
        }

        if (event.getConnectionState() == ConnectionState.CONFIGURATION
                || event.getConnectionState() == ConnectionState.PLAY) {
            player.getCheckManager().onPacketReceive(event);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        GloomPlayer player = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) {
            return;
        }

        player.getCheckManager().onPacketSend(event);
    }
}
