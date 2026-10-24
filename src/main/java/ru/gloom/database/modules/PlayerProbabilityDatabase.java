package ru.gloom.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import ru.gloom.GloomAI;
import ru.gloom.api.database.DatabaseManager;
import ru.gloom.api.model.data.PlayerData;
import ru.gloom.database.model.PlayerAIProbabilityData;
import ru.gloom.database.storage.PlayerProbabilityStorage;

@RequiredArgsConstructor
public class PlayerProbabilityDatabase implements PlayerProbabilityStorage {

    private final DatabaseManager databaseManager;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public CompletableFuture<Void> createTable() {
        return CompletableFuture.runAsync(
                () -> {
                    try (Connection connection = databaseManager.getConnection()) {
                        PlayerProbabilitySchema.createTables(connection);
                        PlayerProbabilitySchema.createIndexIfAbsent(
                                connection,
                                PlayerProbabilitySchema.PLAYER_TABLE,
                                PlayerProbabilitySchema.USERNAME_INDEX,
                                "username");
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Could not create probability tables", exception);
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<Boolean> savePlayerData(PlayerAIProbabilityData data) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (!isValid(data)) {
                        log(Level.WARNING, "Cannot save probability data without player UUID", null);
                        return false;
                    }

                    PlayerData playerData = data.getPlayerData();
                    PlayerProbabilityWriter.normalizePlayerData(data, playerData.getUniqueId(), playerData.getUsername());

                    try (Connection connection = databaseManager.getConnection()) {
                        connection.setAutoCommit(false);

                        try {
                            PlayerProbabilityWriter.upsertPlayer(connection, databaseManager, data);
                            PlayerProbabilityWriter.replaceSnapshots(connection, data);
                            connection.commit();
                            return true;
                        } catch (SQLException exception) {
                            rollback(connection);
                            log(
                                    Level.SEVERE,
                                    "Failed to save player data for: " + data.getPlayerData().getUsername(),
                                    exception);
                            return false;
                        }
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Failed to open connection while saving player data", exception);
                        return false;
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<PlayerAIProbabilityData> getPlayerDataByUUID(UUID uniqueId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String query = "SELECT uuid, username, last_server, created_at, updated_at FROM "
                            + PlayerProbabilitySchema.PLAYER_TABLE + " WHERE uuid = ?";

                    try (Connection connection = databaseManager.getConnection();
                            PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setString(1, uniqueId.toString());

                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                return null;
                            }

                            PlayerAIProbabilityData data = PlayerProbabilityRowMapper.mapPlayer(resultSet);
                            data.setProbSnapshots(PlayerProbabilityRowMapper.loadSnapshots(connection, uniqueId));
                            return data;
                        }
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Failed to get player data by UUID: " + uniqueId, exception);
                        return null;
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<PlayerAIProbabilityData> getPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String query = "SELECT uuid, username, last_server, created_at, updated_at FROM "
                            + PlayerProbabilitySchema.PLAYER_TABLE + " WHERE username = ?";

                    try (Connection connection = databaseManager.getConnection();
                            PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setString(1, playerName);

                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                return null;
                            }

                            PlayerAIProbabilityData data = PlayerProbabilityRowMapper.mapPlayer(resultSet);
                            data.setProbSnapshots(PlayerProbabilityRowMapper.loadSnapshots(
                                    connection, data.getPlayerData().getUniqueId()));
                            return data;
                        }
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Failed to get player data by name: " + playerName, exception);
                        return null;
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<PlayerAIProbabilityData> getOrCreatePlayerData(UUID uniqueId, String playerName) {
        return getPlayerDataByUUID(uniqueId).thenCompose(data -> {
            if (data != null) {
                PlayerProbabilityWriter.normalizePlayerData(data, uniqueId, playerName);
                return CompletableFuture.completedFuture(data);
            }

            PlayerAIProbabilityData createdData = PlayerProbabilityWriter.createDefaultPlayerData(uniqueId, playerName);
            return savePlayerData(createdData).thenApply(saved -> createdData);
        });
    }

    @Override
    public CompletableFuture<Boolean> addProbability(UUID uniqueId, double probability) {
        String username = PlayerProbabilityRowMapper.resolveUsername(uniqueId, null);
        String serverName = GloomAI.serverId();
        long receivedAt = System.currentTimeMillis();
        int maxEntries = Math.max(1, GloomAI.INSTANCE.getMainConfigManager().getMaxProbEntries());

        return CompletableFuture.supplyAsync(
                () -> addProbability(uniqueId, username, probability, serverName, receivedAt, maxEntries),
                executorService);
    }

    @Override
    public CompletableFuture<List<Double>> getProbabilities(UUID uniqueId) {
        return getPlayerDataByUUID(uniqueId)
                .thenApply(data -> data == null ? new ArrayList<>() : data.getProbabilities());
    }

    @Override
    public CompletableFuture<Boolean> deletePlayerData(UUID uniqueId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try (Connection connection = databaseManager.getConnection()) {
                        connection.setAutoCommit(false);

                        try {
                            PlayerProbabilityWriter.deleteSnapshotData(connection, uniqueId);
                            int affectedRows = PlayerProbabilityWriter.deletePlayer(connection, uniqueId);
                            connection.commit();
                            return affectedRows > 0;
                        } catch (SQLException exception) {
                            rollback(connection);
                            log(Level.SEVERE, "Failed to delete player data for UUID: " + uniqueId, exception);
                            return false;
                        }
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Failed to open connection while deleting player data", exception);
                        return false;
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<List<PlayerAIProbabilityData>> getAllPlayersData() {
        return CompletableFuture.supplyAsync(
                () -> {
                    String query = "SELECT "
                            + "p.uuid, p.username, p.last_server, p.created_at AS player_created_at, p.updated_at, "
                            + "s.snapshot_index, s.created_at AS snapshot_created_at, "
                            + "e.entry_index, e.chance, e.server_name, e.received_at "
                            + "FROM " + PlayerProbabilitySchema.PLAYER_TABLE + " p "
                            + "LEFT JOIN " + PlayerProbabilitySchema.SNAPSHOT_TABLE + " s ON s.player_uuid = p.uuid "
                            + "LEFT JOIN " + PlayerProbabilitySchema.ENTRY_TABLE + " e ON e.player_uuid = s.player_uuid "
                            + "AND e.snapshot_index = s.snapshot_index "
                            + "ORDER BY p.uuid, s.snapshot_index, e.entry_index";

                    try (Connection connection = databaseManager.getConnection();
                            PreparedStatement statement = connection.prepareStatement(query);
                            ResultSet resultSet = statement.executeQuery()) {
                        return PlayerProbabilityRowMapper.mapAllPlayers(resultSet);
                    } catch (SQLException exception) {
                        log(Level.SEVERE, "Failed to load all players data", exception);
                        return new ArrayList<>();
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(executorService::shutdown);
    }

    private boolean addProbability(
            UUID uniqueId, String username, double probability, String serverName, long receivedAt, int maxEntries) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                PlayerAIProbabilityData playerData = new PlayerAIProbabilityData(
                        new PlayerData(username, uniqueId), serverName, new ArrayList<>(), receivedAt, receivedAt);

                PlayerProbabilityWriter.upsertPlayer(connection, databaseManager, playerData);
                PlayerProbabilityWriter.touchPlayer(connection, uniqueId, serverName, receivedAt);

                PlayerProbabilityRowMapper.SnapshotPosition position =
                        PlayerProbabilityRowMapper.findSnapshotPosition(connection, uniqueId, maxEntries);

                if (position.createSnapshot()) {
                    PlayerProbabilityWriter.insertSnapshot(connection, uniqueId, position.snapshotIndex(), receivedAt);
                }

                PlayerProbabilityWriter.insertEntry(
                        connection,
                        uniqueId,
                        position.snapshotIndex(),
                        position.entryIndex(),
                        probability,
                        serverName,
                        receivedAt);

                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection);
                log(Level.SEVERE, "Failed to add probability for UUID: " + uniqueId, exception);
                return false;
            }
        } catch (SQLException exception) {
            log(Level.SEVERE, "Failed to open connection while adding probability", exception);
            return false;
        }
    }

    private boolean isValid(PlayerAIProbabilityData data) {
        return data != null
                && data.getPlayerData() != null
                && data.getPlayerData().getUniqueId() != null;
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            log(Level.SEVERE, "Failed to rollback probability transaction", rollbackException);
        }
    }

    private void log(Level level, String message, Throwable throwable) {
        if (throwable == null) {
            GloomAI.INSTANCE.getLogger().log(level, message);
            return;
        }

        GloomAI.INSTANCE.getLogger().log(level, message, throwable);
    }
}
