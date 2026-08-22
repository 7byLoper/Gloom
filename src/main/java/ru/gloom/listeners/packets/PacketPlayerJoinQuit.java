package ru.gloom.listeners.packets;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import java.util.UUID;
import org.bukkit.entity.Player;
import ru.gloom.GloomAI;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            event.getTasksAfterSend()
                    .add(() -> GloomAI.INSTANCE.getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        if (event.getUser().getConnectionState() == ConnectionState.PLAY
                && !GloomAI.INSTANCE.getPlayerDataManager().getExemptUsers().contains(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GloomAI.INSTANCE.getPlayerDataManager().onDisconnect(event.getUser());
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (player.hasPermission("gloom.command.alert") && player.hasPermission("gloom.command.alert.enable-on-join")) {

            if (!GloomAI.INSTANCE.getAlertManager().hasAlertsEnabled(uuid)) {
                GloomAI.INSTANCE.getAlertManager().setAlertsEnabled(uuid, true, true);
            }
        }

        if (player.hasPermission("gloom.command.verbose")
                && player.hasPermission("gloom.command.verbose.enable-on-join")) {

            if (!GloomAI.INSTANCE.getAlertManager().hasVerboseEnabled(uuid)) {
                GloomAI.INSTANCE.getAlertManager().setVerboseEnabled(uuid, true, true);
            }
        }

        if (player.hasPermission("gloom.command.hologram")
                && player.hasPermission("gloom.command.hologram.enable-on-join")) {

            if (!GloomAI.INSTANCE.getHologramManager().hasHologramsEnabled(uuid)) {
                GloomAI.INSTANCE.getHologramManager().setHologramsEnabled(uuid, true, true);
            }
        }
    }
}
