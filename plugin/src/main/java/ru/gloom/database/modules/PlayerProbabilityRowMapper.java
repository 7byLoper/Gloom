package ru.gloom.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import ru.gloom.api.model.data.PlayerData;
import ru.gloom.database.model.PlayerAIProbabilityData;
import ru.gloom.database.model.PlayerAIProbabilityData.Prob;
import ru.gloom.database.model.PlayerAIProbabilityData.ProbSnapshot;

@UtilityClass
public class PlayerProbabilityRowMapper {

    public List<ProbSnapshot> loadSnapshots(Connection connection, UUID uniqueId) throws SQLException {
        String query = "SELECT s.snapshot_index, s.created_at AS snapshot_created_at, "
                + "e.entry_index, e.chance, e.server_name, e.received_at "
                + "FROM " + PlayerProbabilitySchema.SNAPSHOT_TABLE + " s "
                + "LEFT JOIN " + PlayerProbabilitySchema.ENTRY_TABLE + " e ON e.player_uuid = s.player_uuid "
                + "AND e.snapshot_index = s.snapshot_index "
                + "WHERE s.player_uuid = ? "
                + "ORDER BY s.snapshot_index, e.entry_index";

        List<ProbSnapshot> snapshots = new ArrayList<>();
        int currentSnapshotIndex = -1;
        ProbSnapshot currentSnapshot = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int snapshotIndex = resultSet.getInt("snapshot_index");
                    if (currentSnapshot == null || snapshotIndex != currentSnapshotIndex) {
                        currentSnapshot = new ProbSnapshot(resultSet.getLong("snapshot_created_at"), new ArrayList<>());
                        snapshots.add(currentSnapshot);
                        currentSnapshotIndex = snapshotIndex;
                    }

                    if (resultSet.getObject("entry_index") != null) {
                        currentSnapshot.getProbabilities().add(mapProbability(resultSet));
                    }
                }
            }
        }

        return snapshots;
    }

    public List<PlayerAIProbabilityData> mapAllPlayers(ResultSet resultSet) throws SQLException {
        List<PlayerAIProbabilityData> players = new ArrayList<>();
        UUID currentUuid = null;
        PlayerAIProbabilityData currentPlayer = null;
        ProbSnapshot currentSnapshot = null;
        int currentSnapshotIndex = -1;

        while (resultSet.next()) {
            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
            if (!uuid.equals(currentUuid)) {
                currentPlayer = mapJoinedPlayer(resultSet, uuid);
                players.add(currentPlayer);
                currentUuid = uuid;
                currentSnapshot = null;
                currentSnapshotIndex = -1;
            }

            Object snapshotIndexValue = resultSet.getObject("snapshot_index");
            if (snapshotIndexValue == null) {
                continue;
            }

            int snapshotIndex = ((Number) snapshotIndexValue).intValue();
            if (currentSnapshot == null || snapshotIndex != currentSnapshotIndex) {
                currentSnapshot = new ProbSnapshot(resultSet.getLong("snapshot_created_at"), new ArrayList<>());
                currentPlayer.getProbSnapshots().add(currentSnapshot);
                currentSnapshotIndex = snapshotIndex;
            }

            if (resultSet.getObject("entry_index") != null) {
                currentSnapshot.getProbabilities().add(mapProbability(resultSet));
            }
        }

        return players;
    }

    public PlayerAIProbabilityData mapPlayer(ResultSet resultSet) throws SQLException {
        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        String username = resolveUsername(uuid, resultSet.getString("username"));

        return new PlayerAIProbabilityData(
                new PlayerData(username, uuid),
                resultSet.getString("last_server"),
                new ArrayList<>(),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"));
    }

    public PlayerAIProbabilityData mapJoinedPlayer(ResultSet resultSet, UUID uuid) throws SQLException {
        String username = resolveUsername(uuid, resultSet.getString("username"));

        return new PlayerAIProbabilityData(
                new PlayerData(username, uuid),
                resultSet.getString("last_server"),
                new ArrayList<>(),
                resultSet.getLong("player_created_at"),
                resultSet.getLong("updated_at"));
    }

    public Prob mapProbability(ResultSet resultSet) throws SQLException {
        return new Prob(
                resultSet.getDouble("chance"), resultSet.getString("server_name"), resultSet.getLong("received_at"));
    }

    public SnapshotPosition findSnapshotPosition(Connection connection, UUID uniqueId, int maxEntries)
            throws SQLException {
        String query = "SELECT s.snapshot_index, COUNT(e.entry_index) AS entry_count, "
                + "COALESCE(MAX(e.entry_index), -1) AS max_entry_index "
                + "FROM " + PlayerProbabilitySchema.SNAPSHOT_TABLE + " s "
                + "LEFT JOIN " + PlayerProbabilitySchema.ENTRY_TABLE + " e ON e.player_uuid = s.player_uuid "
                + "AND e.snapshot_index = s.snapshot_index "
                + "WHERE s.player_uuid = ? "
                + "GROUP BY s.snapshot_index "
                + "ORDER BY s.snapshot_index DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new SnapshotPosition(0, 0, true);
                }

                int snapshotIndex = resultSet.getInt("snapshot_index");
                int entryCount = resultSet.getInt("entry_count");
                int nextEntryIndex = resultSet.getInt("max_entry_index") + 1;

                if (entryCount >= maxEntries) {
                    return new SnapshotPosition(snapshotIndex + 1, 0, true);
                }

                return new SnapshotPosition(snapshotIndex, nextEntryIndex, false);
            }
        }
    }

    public String resolveUsername(UUID uniqueId, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            return playerName;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
        String offlinePlayerName = offlinePlayer.getName();
        if (offlinePlayerName != null && !offlinePlayerName.isBlank()) {
            return offlinePlayerName;
        }

        return uniqueId.toString().replace("-", "").substring(0, 16);
    }

    public record SnapshotPosition(int snapshotIndex, int entryIndex, boolean createSnapshot) {}
}
