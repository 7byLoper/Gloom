package ru.gloom.database.modules;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlayerProbabilitySchema {
    public static final String PLAYER_TABLE = "player_probabilities";
    public static final String SNAPSHOT_TABLE = "player_probability_snapshots";
    public static final String ENTRY_TABLE = "player_probability_entries";
    public static final String USERNAME_INDEX = "idx_player_probabilities_username";

    public void createTables(Connection connection) throws SQLException {
        List<String> queries = List.of(
                "CREATE TABLE IF NOT EXISTS " + PLAYER_TABLE + " ("
                        + "uuid VARCHAR(36) PRIMARY KEY, "
                        + "username VARCHAR(16) NOT NULL, "
                        + "last_server VARCHAR(64) NOT NULL, "
                        + "created_at BIGINT NOT NULL, "
                        + "updated_at BIGINT NOT NULL"
                        + ")",
                "CREATE TABLE IF NOT EXISTS " + SNAPSHOT_TABLE + " ("
                        + "player_uuid VARCHAR(36) NOT NULL, "
                        + "snapshot_index INTEGER NOT NULL, "
                        + "created_at BIGINT NOT NULL, "
                        + "PRIMARY KEY (player_uuid, snapshot_index), "
                        + "FOREIGN KEY (player_uuid) REFERENCES " + PLAYER_TABLE + "(uuid) ON DELETE CASCADE"
                        + ")",
                "CREATE TABLE IF NOT EXISTS " + ENTRY_TABLE + " ("
                        + "player_uuid VARCHAR(36) NOT NULL, "
                        + "snapshot_index INTEGER NOT NULL, "
                        + "entry_index INTEGER NOT NULL, "
                        + "chance DOUBLE NOT NULL, "
                        + "server_name VARCHAR(64) NOT NULL, "
                        + "received_at BIGINT NOT NULL, "
                        + "PRIMARY KEY (player_uuid, snapshot_index, entry_index), "
                        + "FOREIGN KEY (player_uuid, snapshot_index) REFERENCES "
                        + SNAPSHOT_TABLE + "(player_uuid, snapshot_index) ON DELETE CASCADE"
                        + ")");

        try (Statement statement = connection.createStatement()) {
            for (String query : queries) {
                statement.execute(query);
            }
        }
    }

    public void createIndexIfAbsent(Connection connection, String tableName, String indexName, String columnName)
            throws SQLException {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }

        String query = "CREATE INDEX " + indexName + " ON " + tableName + " (" + columnName + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(query);
        }
    }

    public boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();

        try (ResultSet indexes = metadata.getIndexInfo(catalog, null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndex = indexes.getString("INDEX_NAME");
                if (existingIndex != null
                        && existingIndex.toLowerCase(Locale.ROOT).equals(indexName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        return false;
    }
}
