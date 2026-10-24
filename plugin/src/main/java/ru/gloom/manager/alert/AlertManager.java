package ru.gloom.manager.alert;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.gloom.config.MainConfigManager;
import ru.gloom.utils.MessageUtils;

@RequiredArgsConstructor
public class AlertManager {
    private final MainConfigManager configManager;
    private final Set<UUID> alertPlayers = new CopyOnWriteArraySet<>();
    private final Set<UUID> verbosePlayers = new CopyOnWriteArraySet<>();

    public boolean hasAlertsEnabled(@NonNull UUID uniqueId) {
        return alertPlayers.contains(uniqueId);
    }

    public boolean hasVerboseEnabled(@NonNull UUID uniqueId) {
        return configManager.isTestMode() && verbosePlayers.contains(uniqueId);
    }

    public boolean toggleAlerts(@NonNull UUID uniqueId, boolean silent) {
        boolean newState = !hasAlertsEnabled(uniqueId);
        setAlertsEnabled(uniqueId, newState, silent);
        return newState;
    }

    public void setAlertsEnabled(@NonNull UUID uniqueId, boolean enabled, boolean silent) {
        if (enabled) {
            alertPlayers.add(uniqueId);
        } else {
            alertPlayers.remove(uniqueId);
        }

        if (!silent) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null && player.isOnline()) {
                MessageUtils.send(
                        player,
                        enabled ? configManager.getAlertsEnabledMessage() : configManager.getAlertsDisableMessage());
            }
        }
    }

    public boolean toggleVerbose(@NonNull UUID uniqueId, boolean silent) {
        if (!configManager.isTestMode()) {
            if (!silent) {
                Player player = Bukkit.getPlayer(uniqueId);
                if (player != null && player.isOnline()) {
                    MessageUtils.send(player, configManager.getVerboseTestModeOnlyMessage());
                }
            }
            return false;
        }

        boolean newState = !hasVerboseEnabled(uniqueId);
        setVerboseEnabled(uniqueId, newState, silent);
        return newState;
    }

    public void setVerboseEnabled(@NonNull UUID uniqueId, boolean enabled, boolean silent) {
        if (enabled) {
            if (!configManager.isTestMode()) {
                if (!silent) {
                    Player player = Bukkit.getPlayer(uniqueId);
                    if (player != null && player.isOnline()) {
                        MessageUtils.send(player, configManager.getVerboseTestModeOnlyMessage());
                    }
                }
                return;
            }
            verbosePlayers.add(uniqueId);
        } else {
            verbosePlayers.remove(uniqueId);
        }

        if (!silent) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null && player.isOnline()) {
                MessageUtils.send(
                        player,
                        enabled ? configManager.getVerboseEnabledMessage() : configManager.getVerboseDisableMessage());
            }
        }
    }

    public void sendAlert(@NonNull String message) {
        sendAlert(message, Collections.emptySet());
    }

    public void sendAlert(@NonNull String message, Set<UUID> excludedPlayers) {
        if (message.isBlank()) {
            return;
        }

        alertPlayers.stream()
                .filter(uuid -> excludedPlayers == null || !excludedPlayers.contains(uuid))
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> player.sendMessage(message));

        if (configManager.isPrintToConsole()) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    public Set<UUID> sendVerbose(@NonNull String message) {
        return sendVerbose(message, Collections.emptySet());
    }

    public Set<UUID> sendVerbose(@NonNull String message, Set<UUID> excludedPlayers) {
        if (!configManager.isTestMode() || message.isBlank()) {
            return Collections.emptySet();
        }

        Set<UUID> receivers = verbosePlayers.stream()
                .filter(uuid -> excludedPlayers == null || !excludedPlayers.contains(uuid))
                .collect(Collectors.toSet());

        receivers.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> player.sendMessage(message));

        return receivers;
    }
}
