package ru.gloom.api.model.data;

import java.util.Arrays;

public enum DatasetType {
    ROTATION;

    public static DatasetType fromName(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
