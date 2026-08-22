package ru.gloom.listeners.packets;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.gloom.GloomAI;

public final class HologramListener extends PacketListenerAbstract implements Listener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!isPositionPacket(event)) {
            return;
        }

        WrapperPlayClientPlayerFlying movementPacket = new WrapperPlayClientPlayerFlying(event);
        if (!movementPacket.hasPositionChanged()) {
            return;
        }

        runSync(() -> {
            if (player.isOnline()) {
                GloomAI.INSTANCE.getHologramManager().handleMovement(player);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        handleLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        GloomAI.INSTANCE.getHologramManager().removeViewer(player);
        GloomAI.INSTANCE.getHologramManager().handleMovement(player);
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        GloomAI.INSTANCE.getHologramManager().handleWorldChange(event.getPlayer());
    }

    private void handleLeave(Player player) {
        GloomAI.INSTANCE.getHologramManager().handleLeave(player);
    }

    private boolean isPositionPacket(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        Bukkit.getScheduler().runTask(GloomAI.INSTANCE, task);
    }
}
