package ru.gloom.manager.analytic.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PacketHologramLine {

    private static final AtomicInteger ENTITY_ID_ALLOCATOR = new AtomicInteger(Integer.MAX_VALUE);

    private static final byte INVISIBLE_FLAG = 0x20;
    private static final byte ARMOR_STAND_MARKER_FLAG = 0x10;

    private final int entityId;
    private final UUID entityUuid;

    private final Set<UUID> spawnedViewers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> lastTextByViewer = new ConcurrentHashMap<>();

    public PacketHologramLine() {
        this(ENTITY_ID_ALLOCATOR.getAndDecrement());
    }

    public PacketHologramLine(int entityId) {
        this.entityId = entityId;
        this.entityUuid = UUID.randomUUID();
    }

    public void spawn(Player viewer, Location location, String text) {
        UUID viewerId = viewer.getUniqueId();

        if (!spawnedViewers.add(viewerId)) {
            teleport(viewer, location);
            updateText(viewer, text);
            return;
        }

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(entityUuid),
                EntityTypes.ARMOR_STAND,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                0.0F,
                0.0F,
                0.0F,
                0,
                Optional.of(Vector3d.zero()));

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawnPacket);

        lastTextByViewer.remove(viewerId);

        updateText(viewer, text);
    }

    public void teleport(Player viewer, Location location) {
        if (!isSpawnedFor(viewer.getUniqueId())) {
            return;
        }

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityId, new Vector3d(location.getX(), location.getY(), location.getZ()), 0.0F, 0.0F, false);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleportPacket);
    }

    public void updateText(Player viewer, String text) {
        UUID viewerId = viewer.getUniqueId();

        if (!isSpawnedFor(viewerId)) {
            return;
        }

        String previousText = lastTextByViewer.get(viewerId);
        if (text.equals(previousText)) {
            return;
        }

        lastTextByViewer.put(viewerId, text);

        WrapperPlayServerEntityMetadata metadataPacket =
                new WrapperPlayServerEntityMetadata(entityId, createMetadata(text));

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadataPacket);
    }

    public void destroy(Player viewer) {
        UUID viewerId = viewer.getUniqueId();

        if (!spawnedViewers.remove(viewerId)) {
            return;
        }

        lastTextByViewer.remove(viewerId);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    private boolean isSpawnedFor(UUID viewerId) {
        return spawnedViewers.contains(viewerId);
    }

    private List<EntityData<?>> createMetadata(String text) {
        List<EntityData<?>> metadata = new ArrayList<>(5);

        String coloredText = ChatColor.translateAlternateColorCodes('&', text);
        Component component = LegacyComponentSerializer.legacySection().deserialize(coloredText);

        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, INVISIBLE_FLAG));
        metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(component)));
        metadata.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(15, EntityDataTypes.BYTE, ARMOR_STAND_MARKER_FLAG));

        return metadata;
    }
}
