package ru.gloom.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class LocalOnlineCache {
    private final Map<String, PlayerOnlineInfo> localOnline = new ConcurrentHashMap<>();

    void heartbeat(String uuid, String nickname, String anarchyId, long now) {
        localOnline.put(uuid, new PlayerOnlineInfo(uuid, nickname == null ? "" : nickname, anarchyId, now));
    }

    void remove(String uuid) {
        localOnline.remove(uuid);
    }

    void cleanup(long cutoff) {
        localOnline.entrySet().removeIf(entry -> entry.getValue().lastSeen() < cutoff);
    }

    boolean isOnline(String uuid, long cutoff) {
        cleanup(cutoff);
        PlayerOnlineInfo info = localOnline.get(uuid);
        return info != null && info.lastSeen() >= cutoff;
    }

    Optional<PlayerOnlineInfo> getOnlineInfo(String uuid, long cutoff) {
        cleanup(cutoff);
        PlayerOnlineInfo info = localOnline.get(uuid);
        return info != null && info.lastSeen() >= cutoff ? Optional.of(info) : Optional.empty();
    }

    int getOnlineCount(long cutoff) {
        cleanup(cutoff);
        return localOnline.size();
    }

    int getServerOnlineCount(String anarchyId, long cutoff) {
        cleanup(cutoff);
        return (int) localOnline.values().stream()
                .filter(info -> anarchyId.equals(info.serverId()))
                .count();
    }

    Set<String> getOnlinePlayers(long cutoff) {
        cleanup(cutoff);
        return Collections.unmodifiableSet(new LinkedHashSet<>(localOnline.keySet()));
    }

    Set<String> getOnlinePlayersByServer(String anarchyId, long cutoff) {
        cleanup(cutoff);
        return localOnline.values().stream()
                .filter(info -> anarchyId.equals(info.serverId()))
                .map(PlayerOnlineInfo::uuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    Set<String> getOnlineServers(long cutoff) {
        cleanup(cutoff);
        return localOnline.values().stream()
                .map(PlayerOnlineInfo::serverId)
                .collect(Collectors.toSet());
    }
}
