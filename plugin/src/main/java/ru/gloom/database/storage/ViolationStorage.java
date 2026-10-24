package ru.gloom.database.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import ru.gloom.api.database.DatabaseStorage;
import ru.gloom.database.model.ViolationRecord;

public interface ViolationStorage extends DatabaseStorage {
    CompletableFuture<Void> logAlert(UUID uniqueId, String acVersion, String verbose, String checkName, int vls);

    CompletableFuture<Integer> getLogCount(UUID uniqueId);

    CompletableFuture<List<ViolationRecord>> getViolations(UUID uniqueId, int page, int limit);

    CompletableFuture<List<ViolationRecord>> getAllViolations();
}
