package ru.gloom.service.train;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import ru.gloom.api.model.data.DatasetFrame;
import ru.gloom.api.model.data.DatasetType;
import ru.gloom.api.model.frame.RotationFrame;

@Getter
public final class UploadService {
    private final Plugin plugin;

    private final File datasetFile;
    private final DatasetType datasetType;

    private final Object fileLock = new Object();

    public UploadService(Plugin plugin, String fileName, String folderName, DatasetType datasetType) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.datasetType = Objects.requireNonNull(datasetType, "datasetType");

        File dataFolder = new File(plugin.getDataFolder(), folderName);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Не удалось создать директорию: " + dataFolder.getAbsolutePath());
        }

        this.datasetFile = new File(dataFolder, sanitizeFileName(fileName) + ".csv");
        ensureHeader();
    }

    public CompletableFuture<Boolean> uploadPlayerData(List<DatasetFrame> frames, boolean isCheater) {
        return CompletableFuture.supplyAsync(() -> {
            if (frames == null || frames.isEmpty()) {
                return false;
            }

            synchronized (fileLock) {
                ensureHeader();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(datasetFile, true))) {
                    for (DatasetFrame frame : frames) {
                        writer.newLine();
                        writer.write(frame.toCsvRow(isCheater));
                    }
                    writer.flush();
                    return true;
                } catch (IOException exception) {
                    plugin.getLogger().severe("Ошибка сохранения датасета: " + exception.getMessage());
                    return false;
                }
            }
        });
    }

    private void ensureHeader() {
        synchronized (fileLock) {
            if (datasetFile.exists() && datasetFile.length() > 0L) {
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(datasetFile, false))) {
                writer.write(RotationFrame.csvHeader());
                writer.flush();
            } catch (IOException exception) {
                plugin.getLogger().severe("Ошибка записи заголовка CSV: " + exception.getMessage());
            }
        }
    }

    private String sanitizeFileName(String value) {
        return value == null || value.isBlank() ? "dataset" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
