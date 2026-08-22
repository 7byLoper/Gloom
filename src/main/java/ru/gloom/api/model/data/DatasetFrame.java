package ru.gloom.api.model.data;

public interface DatasetFrame {
    DatasetType getDatasetType();

    String toCsvRow(boolean cheater);
}
