package ru.gloom.manager.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.gloom.GloomAI;
import ru.gloom.api.model.data.TrainData;
import ru.gloom.player.GloomPlayer;
import ru.gloom.utils.reflections.GeyserUtil;

public class PlayerDataManager {
    @Getter
    private final Collection<User> exemptUsers = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<UUID, GloomPlayer> playerDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TrainData> trainDataMap = new ConcurrentHashMap<>();

    @Nullable
    public GloomPlayer getPlayer(@NotNull UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            addUser(user);
        }
    }

    @Nullable
    public GloomPlayer getPlayer(String username) {
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            return null;
        }

        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(player.getUniqueId());
        User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        return getPlayer(user);
    }

    @Nullable
    public GloomPlayer getPlayer(@NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) {
            // UUID ещё не назначен на ранних стадиях соединения (HANDSHAKING/LOGIN).
            return null;
        }
        return playerDataMap.get(uuid);
    }

    public TrainData getOrCreateTrainData(UUID uuid, String name) {
        return trainDataMap.computeIfAbsent(uuid, k -> new TrainData(uuid, name));
    }

    public boolean exemptCheck(@NotNull User user) {
        if (exemptUsers.contains(user)) {
            return true;
        }

        if (!ChannelHelper.isOpen(user.getChannel())) {
            return true;
        }

        if (GeyserUtil.isBedrockPlayer(user.getUUID())) {
            exemptUsers.add(user);
            return true;
        }

        if (user.getUUID().toString().startsWith("00000000-0000-0000-0009")) {
            exemptUsers.add(user);
            return true;
        }

        return false;
    }

    public void addUser(@NotNull User user) {
        if (exemptCheck(user)) {
            return;
        }

        GloomAI.INSTANCE.getPlayerOnlineService().heartbeat(user.getUUID(), user.getName());
        GloomAI.INSTANCE
                .getViolationManager()
                .getProbabilityStorage()
                .getOrCreatePlayerData(user.getUUID(), user.getName());

        GloomPlayer player = new GloomPlayer(user);
        playerDataMap.put(user.getUUID(), player);
    }

    public GloomPlayer remove(final @NotNull User user) {
        return playerDataMap.remove(user.getUUID());
    }

    public void onDisconnect(User user) {
        if (user.getUUID() == null) {
            // Соединение оборвалось на ранней стадии (HANDSHAKING/STATUS/LOGIN):
            // пользователь никогда не регистрировался, чистить нечего.
            return;
        }

        GloomAI.INSTANCE.getPlayerOnlineService().quit(user.getUUID());

        exemptUsers.remove(user);
        GloomPlayer player = remove(user);

        if (player != null) {
            GloomAI.INSTANCE.getAlertManager().setAlertsEnabled(player.getUuid(), false, true);
            GloomAI.INSTANCE.getAlertManager().setVerboseEnabled(player.getUuid(), false, true);
        }
    }

    public Collection<GloomPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
