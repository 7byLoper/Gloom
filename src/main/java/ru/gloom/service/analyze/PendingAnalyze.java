package ru.gloom.service.analyze;

import java.util.function.BooleanSupplier;

record PendingAnalyze(
        byte[] payload,
        AnalyzeBatchDispatcher.AnalyzeResultConsumer resultConsumer,
        long createdAtNanos,
        BooleanSupplier valid) {
    boolean isCurrent() {
        return System.nanoTime() - createdAtNanos <= 5_000_000_000L && valid.getAsBoolean();
    }
}
