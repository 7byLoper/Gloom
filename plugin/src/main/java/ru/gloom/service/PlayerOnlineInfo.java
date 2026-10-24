package ru.gloom.service;

import java.util.Map;

public record PlayerOnlineInfo(
        String uuid,
        String nickname,
        String serverId,
        long lastSeen) {
    public static PlayerOnlineInfo fromMap(Map<String, String> data) {
        String uuid = data.getOrDefault("uuid", "");
        String nickname = data.getOrDefault("nickname", "");
        String anarchyId = data.getOrDefault("anarchy", "");

        long lastSeen;
        try {
            lastSeen = Long.parseLong(data.getOrDefault("lastSeen", "0"));
        } catch (NumberFormatException exception) {
            lastSeen = 0L;
        }

        return new PlayerOnlineInfo(uuid, nickname, anarchyId, lastSeen);
    }

    public String getUuid() {
        return uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public String getServerId() {
        return serverId;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
