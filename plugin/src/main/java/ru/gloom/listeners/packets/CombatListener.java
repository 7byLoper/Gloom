package ru.gloom.listeners.packets;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.gloom.GloomAI;
import ru.gloom.player.GloomPlayer;

public final class CombatListener extends PacketListenerAbstract {
    public CombatListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity interactPacket = new WrapperPlayClientInteractEntity(event);
        if (interactPacket.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        UUID playerUuid = event.getUser().getUUID();
        if (playerUuid == null) {
            return;
        }

        int targetEntityId = interactPacket.getEntityId();
        Bukkit.getScheduler().runTask(GloomAI.INSTANCE, () -> tagCombat(playerUuid, targetEntityId));
    }

    private void tagCombat(UUID playerUuid, int targetEntityId) {
        GloomPlayer gloomPlayer = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(playerUuid);
        if (gloomPlayer == null) {
            return;
        }

        Entity target = GloomAI.INSTANCE.getTargetEntityIndex().getByEntityId(targetEntityId);
        if (target instanceof Player) {
            gloomPlayer.tagCombat();
        }
    }
}
