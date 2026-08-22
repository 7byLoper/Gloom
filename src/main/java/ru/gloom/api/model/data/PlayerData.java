package ru.gloom.api.model.data;

import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Data
public class PlayerData {
    private final String username;
    private final UUID uniqueId;

    public static PlayerData fromPlayer(@NotNull Player player) {
        final String username = player.getName();
        final UUID uniqueId = player.getUniqueId();

        return new PlayerData(username, uniqueId);
    }

    public static PlayerData fromPlayer(@NotNull OfflinePlayer player) {
        final String username = player.getName();
        final UUID uniqueId = player.getUniqueId();

        return new PlayerData(username, uniqueId);
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(uniqueId);
    }

    public Player getOnlinePlayer() {
        return Bukkit.getPlayer(uniqueId);
    }
}
