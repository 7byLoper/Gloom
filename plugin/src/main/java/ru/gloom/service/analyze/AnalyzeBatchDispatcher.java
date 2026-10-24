package ru.gloom.service.analyze;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class AnalyzeBatchDispatcher {
    @FunctionalInterface
    public interface AnalyzeResultConsumer {
        void accept(double probability, String modelName);
    }

    private static final int MAX_BATCH_SIZE = 32;
    private static final long FLUSH_PERIOD_MS = 50L;
    private static final int BATCH_COUNT_SIZE = 2;
    private static final int BATCH_ITEM_HEADER_SIZE = 4;
    private static final byte[] RESPONSE_MAGIC = {'G', 'A', 'I', 'B'};

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long RETRY_DELAY_MS = TimeUnit.MINUTES.toMillis(10);

    private final Plugin plugin;
    private final Supplier<String> endpointSupplier;
    private final Supplier<String> licenseKeySupplier;
    private final HttpClient httpClient;

    private final ConcurrentLinkedQueue<PendingAnalyze> queue = new ConcurrentLinkedQueue<>();

    private final AtomicInteger queueSize = new AtomicInteger();
    private final AtomicBoolean unavailableReported = new AtomicBoolean();
    private final AtomicBoolean apiAvailable = new AtomicBoolean();
    private final AtomicBoolean probeInFlight = new AtomicBoolean();
    private final AtomicLong retryAfterMillis = new AtomicLong();

    private final ScheduledExecutorService flusher;
    private volatile boolean stopped;
    private volatile String lastModelName = "TCN";

    public AnalyzeBatchDispatcher(
            Plugin plugin, Supplier<String> endpointSupplier, Supplier<String> licenseKeySupplier) {
        this.plugin = plugin;
        this.endpointSupplier = endpointSupplier;
        this.licenseKeySupplier = licenseKeySupplier;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "GloomAI-analyze-flusher");
            thread.setDaemon(true);
            return thread;
        };
        this.flusher = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public void start() {
        flusher.scheduleAtFixedRate(this::safeFlush, FLUSH_PERIOD_MS, FLUSH_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stopped = true;
        flusher.shutdownNow();
        queue.clear();
        queueSize.set(0);
    }

    public void retryNow() {
        retryAfterMillis.set(0);
        unavailableReported.set(false);
    }

    public boolean isApiAvailable() {
        return apiAvailable.get();
    }

    public String getLastModelName() {
        return lastModelName;
    }

    public void setLastModelName(String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            this.lastModelName = modelName;
        }
    }

    public void enqueue(byte[] payload, DoubleConsumer resultConsumer, BooleanSupplier valid) {
        enqueue(payload, (probability, model) -> resultConsumer.accept(probability), valid);
    }

    public void enqueue(byte[] payload, AnalyzeResultConsumer resultConsumer, BooleanSupplier valid) {
        if (stopped || !valid.getAsBoolean()) {
            return;
        }
        int pending = queueSize.incrementAndGet();
        if (pending > 512) {
            queueSize.decrementAndGet();
            return;
        }
        queue.add(new PendingAnalyze(payload, resultConsumer, System.nanoTime(), valid));
        if (pending >= MAX_BATCH_SIZE && !stopped) {
            try {
                flusher.execute(this::safeFlush);
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    private void safeFlush() {
        try {
            flush();
        } catch (Throwable throwable) {
            markUnavailable("batch dispatch failed: " + exceptionMessage(throwable));
        }
    }

    private void flush() {
        if (!canAttemptRequest()) {
            return;
        }

        final URI endpoint;
        try {
            endpoint = URI.create(endpointSupplier.get());
        } catch (RuntimeException exception) {
            markUnavailable("invalid analyze_server URL: " + exceptionMessage(exception));
            return;
        }

        boolean probe = !apiAvailable.get();
        if (probe && !probeInFlight.compareAndSet(false, true)) {
            return;
        }

        List<PendingAnalyze> batch = drainUpTo();
        if (batch.isEmpty()) {
            if (probe) {
                probeInFlight.set(false);
            }
            return;
        }

        sendBatch(endpoint, batch);
        if (probe) {
            return;
        }

        while (true) {
            batch = drainUpTo();
            if (batch.isEmpty()) {
                return;
            }
            sendBatch(endpoint, batch);
        }
    }

    private List<PendingAnalyze> drainUpTo() {
        List<PendingAnalyze> items =
                new ArrayList<>(Math.min(AnalyzeBatchDispatcher.MAX_BATCH_SIZE, Math.max(queueSize.get(), 1)));
        for (int i = 0; i < AnalyzeBatchDispatcher.MAX_BATCH_SIZE; i++) {
            PendingAnalyze item = queue.poll();
            if (item == null) {
                break;
            }
            queueSize.updateAndGet(size -> Math.max(0, size - 1));
            if (item.isCurrent()) {
                items.add(item);
            }
        }
        return items;
    }

    private void sendBatch(URI endpoint, List<PendingAnalyze> queued) {
        List<PendingAnalyze> batch = queued.stream().filter(PendingAnalyze::isCurrent).toList();
        if (batch.isEmpty()) {
            probeInFlight.set(false);
            return;
        }
        byte[] body = encodeFraming(batch);
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/x-flatbuffers")
                .header("Accept", "application/x-flatbuffers")
                .header("X-Batch", "1")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        String licenseKey = licenseKeySupplier == null ? null : licenseKeySupplier.get();
        if (licenseKey != null && !licenseKey.isBlank()) {
            builder.header("X-License-Key", licenseKey.strip());
        }
        HttpRequest request = builder.build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, throwable) -> {
            try {
                if (throwable != null) {
                    requeue(batch);
                    markUnavailable("cannot connect to AI API: " + exceptionMessage(throwable));
                    return;
                }
                handleResponse(batch, response);
            } catch (Throwable unexpected) {
                markUnavailable("AI API response handling failed: " + exceptionMessage(unexpected));
            }
        });
    }

    private byte[] encodeFraming(List<PendingAnalyze> batch) {
        int totalSize = RESPONSE_MAGIC.length + BATCH_COUNT_SIZE;
        for (PendingAnalyze item : batch) {
            totalSize += BATCH_ITEM_HEADER_SIZE + item.payload().length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(RESPONSE_MAGIC);
        buffer.putShort((short) batch.size());
        for (PendingAnalyze item : batch) {
            buffer.putInt(item.payload().length);
            buffer.put(item.payload());
        }
        return buffer.array();
    }

    private void handleResponse(List<PendingAnalyze> batch, HttpResponse<byte[]> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if (response.statusCode() >= 500) {
                requeue(batch);
            }
            markUnavailable(httpFailureMessage(response.statusCode()));
            return;
        }

        final double[] probabilities;
        try {
            probabilities = decodeResponse(response.body(), batch.size());
        } catch (RuntimeException exception) {
            markUnavailable("invalid AI API response: " + exceptionMessage(exception));
            return;
        }

        if (stopped || !plugin.isEnabled()) {
            return;
        }
        markAvailable();

        response.headers().firstValue("X-Model-Name").ifPresent(raw -> {
            try {
                String decoded = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
                if (!decoded.isBlank()) {
                    this.lastModelName = decoded;
                }
            } catch (Exception ignored) {
                if (!raw.isBlank()) {
                    this.lastModelName = raw;
                }
            }
        });

        final String currentModel = (lastModelName != null && !lastModelName.isBlank()) ? lastModelName : "TCN";

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < batch.size(); i++) {
                PendingAnalyze item = batch.get(i);
                if (item.isCurrent() && Double.isFinite(probabilities[i])) {
                    item.resultConsumer().accept(probabilities[i], currentModel);
                }
            }
        });
    }

    static double[] decodeResponse(byte[] body, int expectedCount) {
        return AnalyzeResponseDecoder.decodeResponse(body, expectedCount);
    }

    private boolean canAttemptRequest() {
        return apiAvailable.get() || System.currentTimeMillis() >= retryAfterMillis.get();
    }

    private void requeue(List<PendingAnalyze> batch) {
        if (stopped) {
            return;
        }

        for (PendingAnalyze item : batch) {
            if (item.isCurrent()) {
                int pending = queueSize.incrementAndGet();
                if (pending <= 512) {
                    queue.add(item);
                } else {
                    queueSize.decrementAndGet();
                }
            }
        }
    }

    private void markUnavailable(String reason) {
        apiAvailable.set(false);
        probeInFlight.set(false);
        retryAfterMillis.set(System.currentTimeMillis() + RETRY_DELAY_MS);
        if (unavailableReported.compareAndSet(false, true)) {
            Bukkit.getLogger()
                    .warning(
                            "[GloomAI] AI API unavailable: " + reason
                                    + ". Requests are paused for 10 minutes. Further errors are suppressed until the connection is restored.");
        }
    }

    private void markAvailable() {
        apiAvailable.set(true);
        probeInFlight.set(false);
        retryAfterMillis.set(0L);
        if (unavailableReported.compareAndSet(true, false)) {
            Bukkit.getLogger().info("[GloomAI] Connection to the AI API has been restored.");
        }
    }

    private static String httpFailureMessage(int statusCode) {
        if (statusCode == 403) {
            return "HTTP 403: license is not found, inactive, or expired";
        }
        if (statusCode == 422) {
            return "HTTP 422: AI API rejected the FlatBuffers request";
        }
        if (statusCode >= 500) {
            return "HTTP " + statusCode + ": AI API is unavailable";
        }
        return "AI API returned HTTP " + statusCode;
    }

    private static String exceptionMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
