package ru.gloom.manager.analytic.hologram;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.gloom.GloomAI;
import ru.gloom.utils.MessageUtils;
import ru.gloom.config.anticheat.HologramConfigManager;

@Getter
public final class HologramManager {

    private final Map<UUID, List<String>> hologramLinesByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, TrackedPlayerHologram> hologramsByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewersByTarget = new ConcurrentHashMap<>();
    private final Set<UUID> enabledViewers = ConcurrentHashMap.newKeySet();

    private BukkitTask updateTask;

    public void start() {
        stop();
        updateTask = Bukkit.getScheduler().runTaskTimer(GloomAI.INSTANCE, this::updateVisibleHolograms, 10L, 10L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        clearAllHolograms();
        hologramLinesByTarget.clear();
    }

    public boolean hasHologramsEnabled(@NonNull UUID viewerId) {
        return enabledViewers.contains(viewerId);
    }

    public boolean toggleHolograms(@NonNull UUID viewerId, boolean silent) {
        boolean enabled = !hasHologramsEnabled(viewerId);
        setHologramsEnabled(viewerId, enabled, silent);
        return enabled;
    }

    public void setHologramsEnabled(@NonNull UUID viewerId, boolean enabled) {
        setHologramsEnabled(viewerId, enabled, false);
    }

    public void setHologramsEnabled(@NonNull UUID viewerId, boolean enabled, boolean silent) {
        Player viewer = Bukkit.getPlayer(viewerId);

        if (enabled) {
            enabledViewers.add(viewerId);
        } else {
            enabledViewers.remove(viewerId);

            if (viewer != null && viewer.isOnline()) {
                removeViewer(viewer);
            }
        }

        if (!silent && viewer != null && viewer.isOnline()) {
            MessageUtils.send(
                    viewer,
                    enabled
                            ? GloomAI.INSTANCE.getMainConfigManager().getHologramsEnabledMessage()
                            : GloomAI.INSTANCE.getMainConfigManager().getHologramsDisableMessage());
        }
    }

    public void handleLeave(@NonNull Player player) {
        UUID playerId = player.getUniqueId();

        hideTarget(playerId);
        removeViewer(player);

        hologramLinesByTarget.remove(playerId);
    }

    public void handleQuit(@NonNull UUID targetId) {
        hideTarget(targetId);
        hologramLinesByTarget.remove(targetId);
    }

    public void handleWorldChange(@NonNull Player player) {
        removeViewer(player);
        hideTarget(player.getUniqueId());
    }

    public void handleMovement(@NonNull Player target) {
        if (!target.isOnline()) {
            return;
        }

        HologramConfigManager config = GloomAI.INSTANCE.getHologramConfigManager();
        if (config == null || !config.isEnabled()) {
            clearAllHolograms();
            return;
        }

        UUID targetId = target.getUniqueId();
        TrackedPlayerHologram hologram = hologramsByTarget.get(targetId);
        Set<UUID> currentViewers = viewersByTarget.get(targetId);

        if (hologram == null || currentViewers == null || currentViewers.isEmpty()) {
            return;
        }

        Location baseLocation = createBaseLocation(target);
        double spacing = config.getLineSpacing();

        for (UUID viewerId : new HashSet<>(currentViewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);

            if (!canViewerSeeTarget(viewer, target)) {
                if (viewer != null && viewer.isOnline()) {
                    hologram.destroyForViewer(viewer);
                }

                currentViewers.remove(viewerId);
                continue;
            }

            hologram.teleportForViewer(viewer, baseLocation, spacing);
        }

        cleanupEmptyTarget(targetId, currentViewers);
    }

    public void removeViewer(@NonNull Player viewer) {
        UUID viewerId = viewer.getUniqueId();

        for (Map.Entry<UUID, Set<UUID>> entry : new ArrayList<>(viewersByTarget.entrySet())) {
            UUID targetId = entry.getKey();
            Set<UUID> viewers = entry.getValue();

            if (!viewers.remove(viewerId)) {
                continue;
            }

            TrackedPlayerHologram hologram = hologramsByTarget.get(targetId);
            if (hologram != null) {
                hologram.destroyForViewer(viewer);
            }

            cleanupEmptyTarget(targetId, viewers);
        }
    }

    public void refreshCachedLines(@NonNull UUID targetId) {
        HologramConfigManager config = GloomAI.INSTANCE.getHologramConfigManager();

        if (config == null || !config.isEnabled()) {
            hologramLinesByTarget.remove(targetId);
            hideTarget(targetId);
            return;
        }

        List<String> updatedLines = buildLinesForTarget(targetId, config);
        List<String> previousLines = hologramLinesByTarget.get(targetId);

        if (updatedLines.equals(previousLines)) {
            return;
        }

        hologramLinesByTarget.put(targetId, updatedLines);
    }

    private void updateVisibleHolograms() {
        HologramConfigManager config = GloomAI.INSTANCE.getHologramConfigManager();

        if (config == null || !config.isEnabled()) {
            clearAllHolograms();
            hologramLinesByTarget.clear();
            return;
        }

        cleanupOfflineTargets();

        for (Player target : Bukkit.getOnlinePlayers()) {
            refreshCachedLines(target.getUniqueId());
            refreshTargetViewers(target, config);
        }
    }

    private void refreshTargetViewers(@NonNull Player target, @NonNull HologramConfigManager config) {
        UUID targetId = target.getUniqueId();
        List<String> lines = hologramLinesByTarget.get(targetId);

        if (lines == null || lines.isEmpty()) {
            hideTarget(targetId);
            return;
        }

        TrackedPlayerHologram hologram =
                hologramsByTarget.computeIfAbsent(targetId, ignored -> new TrackedPlayerHologram());

        Set<UUID> currentViewers = viewersByTarget.computeIfAbsent(targetId, ignored -> ConcurrentHashMap.newKeySet());

        Location baseLocation = createBaseLocation(target);
        double spacing = config.getLineSpacing();

        for (UUID viewerId : new HashSet<>(currentViewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);

            if (!canViewerSeeTarget(viewer, target)) {
                if (viewer != null && viewer.isOnline()) {
                    hologram.destroyForViewer(viewer);
                }

                currentViewers.remove(viewerId);
                continue;
            }

            hologram.updateForViewer(viewer, baseLocation, lines, spacing);
        }

        for (UUID viewerId : enabledViewers) {
            if (currentViewers.contains(viewerId)) {
                continue;
            }

            Player viewer = Bukkit.getPlayer(viewerId);
            if (!canViewerSeeTarget(viewer, target)) {
                continue;
            }

            currentViewers.add(viewerId);
            hologram.updateForViewer(viewer, baseLocation, lines, spacing);
        }

        cleanupEmptyTarget(targetId, currentViewers);
    }

    private void hideTarget(@NonNull UUID targetId) {
        Set<UUID> viewerIds = viewersByTarget.remove(targetId);
        TrackedPlayerHologram hologram = hologramsByTarget.remove(targetId);

        if (viewerIds == null || hologram == null) {
            return;
        }

        for (UUID viewerId : viewerIds) {
            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer != null && viewer.isOnline()) {
                hologram.destroyForViewer(viewer);
            }
        }
    }

    private void clearAllHolograms() {
        for (Map.Entry<UUID, Set<UUID>> entry : new ArrayList<>(viewersByTarget.entrySet())) {
            TrackedPlayerHologram hologram = hologramsByTarget.get(entry.getKey());
            if (hologram == null) {
                continue;
            }

            for (UUID viewerId : new HashSet<>(entry.getValue())) {
                Player viewer = Bukkit.getPlayer(viewerId);

                if (viewer != null && viewer.isOnline()) {
                    hologram.destroyForViewer(viewer);
                }
            }
        }

        viewersByTarget.clear();
        hologramsByTarget.clear();
    }

    private void cleanupOfflineTargets() {
        Set<UUID> targetIds = new HashSet<>();
        targetIds.addAll(hologramsByTarget.keySet());
        targetIds.addAll(hologramLinesByTarget.keySet());

        for (UUID targetId : targetIds) {
            Player target = Bukkit.getPlayer(targetId);

            if (target != null && target.isOnline()) {
                continue;
            }

            hideTarget(targetId);
            hologramLinesByTarget.remove(targetId);
        }
    }

    private void cleanupEmptyTarget(UUID targetId, Set<UUID> viewers) {
        if (!viewers.isEmpty()) {
            return;
        }

        viewersByTarget.remove(targetId);
        hologramsByTarget.remove(targetId);
    }

    private boolean canViewerSeeTarget(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }

        if (!viewer.isOnline() || !target.isOnline()) {
            return false;
        }

        if (!hasHologramsEnabled(viewer.getUniqueId())) {
            return false;
        }

        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return false;
        }

        if (!viewer.getWorld().equals(target.getWorld())) {
            return false;
        }

        return viewer.getLocation().distanceSquared(target.getLocation()) <= 900.0D;
    }

    private Location createBaseLocation(@NonNull Player target) {
        return target.getLocation()
                .clone()
                .add(0.0D, GloomAI.INSTANCE.getHologramConfigManager().getOffset(), 0.0D);
    }

    private List<String> buildLinesForTarget(@NonNull UUID targetId, @NonNull HologramConfigManager config) {
        return HologramLineFormatter.buildLinesForTarget(targetId, config);
    }
}
