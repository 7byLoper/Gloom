package ru.gloom.api.model.frame;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import ru.gloom.api.model.data.DatasetFrame;
import ru.gloom.api.model.data.DatasetType;

@Value
public class RotationFrame implements DatasetFrame {
    float deltaYaw;
    float deltaPitch;
    float accelYaw;
    float accelPitch;
    float jerkYaw;
    float jerkPitch;
    float gcdErrorYaw;
    float gcdErrorPitch;

    @Override
    public DatasetType getDatasetType() {
        return DatasetType.ROTATION;
    }

    public static String csvHeader() {
        return "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch";
    }

    @Override
    public String toCsvRow(boolean cheater) {
        return Stream.of(
                        cheater ? 1 : 0,
                        deltaYaw,
                        deltaPitch,
                        accelYaw,
                        accelPitch,
                        jerkYaw,
                        jerkPitch,
                        gcdErrorYaw,
                        gcdErrorPitch)
                .map(value -> value instanceof Float ? String.format(Locale.US, "%.6f", value) : String.valueOf(value))
                .collect(Collectors.joining(","));
    }
}
