package ru.gloom.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import ru.gloom.GloomAI;
import ru.gloom.api.database.DatabaseManager;
import ru.gloom.api.model.data.PlayerData;
import ru.gloom.database.model.PlayerAIProbabilityData;
import ru.gloom.database.model.PlayerAIProbabilityData.Prob;
import ru.gloom.database.model.PlayerAIProbabilityData.ProbSnapshot;

@UtilityClass
public class PlayerProbabilityWriter {

    public void upsertPlayer(Connection connection, DatabaseManager databaseManager, PlayerAIProbabilityData data)
            throws SQLException {
        String query = databaseManager.getSqlByDataType(
                "INSERT INTO " + PlayerProbabilitySchema.PLAYER_TABLE
                        + " (uuid, username, last_server, created_at, updated_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE username = VALUES(username), "
                        + "last_server = VALUES(last_server), updated_at = VALUES(updated_at)",
                "INSERT INTO " + PlayerProbabilitySchema.PLAYER_TABLE
                        + " (uuid, username, last_server, created_at, updated_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT(uuid) DO UPDATE SET username = excluded.username, "
                        + "last_server = excluded.last_server, updated_at = excluded.updated_at");

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, data.getPlayerData().getUniqueId().toString());
            statement.setString(2, data.getPlayerData().getUsername());
            statement.setString(3, data.getLastServerName());
            statement.setLong(4, data.getCreatedAt());
            statement.setLong(5, data.getUpdatedAt());
            statement.executeUpdate();
        }
    }

    public void touchPlayer(Connection connection, UUID uniqueId, String serverName, long updatedAt)
            throws SQLException {
        String query = "UPDATE " + PlayerProbabilitySchema.PLAYER_TABLE
                + " SET last_server = ?, updated_at = ? WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, serverName);
            statement.setLong(2, updatedAt);
            statement.setString(3, uniqueId.toString());
            statement.executeUpdate();
        }
    }

    public void replaceSnapshots(Connection connection, PlayerAIProbabilityData data) throws SQLException {
        UUID uniqueId = data.getPlayerData().getUniqueId();
        deleteSnapshotData(connection, uniqueId);

        List<ProbSnapshot> snapshots = data.getProbSnapshots();
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (int snapshotIndex = 0; snapshotIndex < snapshots.size(); snapshotIndex++) {
            ProbSnapshot snapshot = snapshots.get(snapshotIndex);
            if (snapshot == null) {
                continue;
            }

            insertSnapshot(connection, uniqueId, snapshotIndex, snapshot.getCreatedAt());
            insertSnapshotEntries(connection, uniqueId, snapshotIndex, snapshot);
        }
    }

    public void insertSnapshotEntries(
            Connection connection, UUID uniqueId, int snapshotIndex, ProbSnapshot snapshot)
            throws SQLException {
        List<Prob> probabilities = snapshot.getProbabilities();
        if (probabilities == null || probabilities.isEmpty()) {
            return;
        }

        String query = "INSERT INTO " + PlayerProbabilitySchema.ENTRY_TABLE
                + " (player_uuid, snapshot_index, entry_index, chance, server_name, received_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int entryIndex = 0; entryIndex < probabilities.size(); entryIndex++) {
                Prob probability = probabilities.get(entryIndex);
                if (probability == null) {
                    continue;
                }

                statement.setString(1, uniqueId.toString());
                statement.setInt(2, snapshotIndex);
                statement.setInt(3, entryIndex);
                statement.setDouble(4, probability.getChance());
                statement.setString(5, probability.getServerName());
                statement.setLong(6, probability.getReceivedAt());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    public void insertSnapshot(Connection connection, UUID uniqueId, int snapshotIndex, long createdAt)
            throws SQLException {
        String query = "INSERT INTO " + PlayerProbabilitySchema.SNAPSHOT_TABLE
                + " (player_uuid, snapshot_index, created_at) VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            statement.setInt(2, snapshotIndex);
            statement.setLong(3, createdAt);
            statement.executeUpdate();
        }
    }

    public void insertEntry(
            Connection connection,
            UUID uniqueId,
            int snapshotIndex,
            int entryIndex,
            double probability,
            String serverName,
            long receivedAt)
            throws SQLException {
        String query = "INSERT INTO " + PlayerProbabilitySchema.ENTRY_TABLE
                + " (player_uuid, snapshot_index, entry_index, chance, server_name, received_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            statement.setInt(2, snapshotIndex);
            statement.setInt(3, entryIndex);
            statement.setDouble(4, probability);
            statement.setString(5, serverName);
            statement.setLong(6, receivedAt);
            statement.executeUpdate();
        }
    }

    public void deleteSnapshotData(Connection connection, UUID uniqueId) throws SQLException {
        executeDelete(connection, PlayerProbabilitySchema.ENTRY_TABLE, "player_uuid", uniqueId.toString());
        executeDelete(connection, PlayerProbabilitySchema.SNAPSHOT_TABLE, "player_uuid", uniqueId.toString());
    }

    public int deletePlayer(Connection connection, UUID uniqueId) throws SQLException {
        String query = "DELETE FROM " + PlayerProbabilitySchema.PLAYER_TABLE + " WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            return statement.executeUpdate();
        }
    }

    public void executeDelete(Connection connection, String tableName, String columnName, String value)
            throws SQLException {
        String query = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, value);
            statement.executeUpdate();
        }
    }

    public void normalizePlayerData(PlayerAIProbabilityData data, UUID uniqueId, String playerName) {
        if (data.getPlayerData() == null) {
            data.setPlayerData(new PlayerData(PlayerProbabilityRowMapper.resolveUsername(uniqueId, playerName), uniqueId));
        }

        if (data.getPlayerData().getUsername() == null
                || data.getPlayerData().getUsername().isBlank()) {
            data.setPlayerData(new PlayerData(PlayerProbabilityRowMapper.resolveUsername(uniqueId, playerName), uniqueId));
        }

        if (data.getLastServerName() == null || data.getLastServerName().isBlank()) {
            data.setLastServerName(GloomAI.serverId());
        }

        if (data.getProbSnapshots() == null) {
            data.setProbSnapshots(new ArrayList<>());
        }

        long now = System.currentTimeMillis();
        if (data.getCreatedAt() <= 0L) {
            data.setCreatedAt(now);
        }

        if (data.getUpdatedAt() <= 0L) {
            data.setUpdatedAt(now);
        }
    }

    public PlayerAIProbabilityData createDefaultPlayerData(UUID uniqueId, String playerName) {
        long now = System.currentTimeMillis();

        return new PlayerAIProbabilityData(
                new PlayerData(PlayerProbabilityRowMapper.resolveUsername(uniqueId, playerName), uniqueId),
                GloomAI.serverId(),
                new ArrayList<>(),
                now,
                now);
    }
}
