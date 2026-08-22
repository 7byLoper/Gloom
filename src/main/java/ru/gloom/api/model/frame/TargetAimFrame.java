package ru.gloom.api.model.frame;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import ru.gloom.api.model.data.DatasetFrame;
import ru.gloom.api.model.data.DatasetType;

@Value
public class TargetAimFrame implements DatasetFrame {
    float deltaYaw;
    float deltaPitch;
    float accelYaw;
    float accelPitch;
    float targetErrorYawNorm;
    float targetErrorPitchNorm;
    float targetErrorDeltaYaw;
    float targetErrorDeltaPitch;
    float targetDistance;
    float ticksSinceAttack;

    @Override
    public DatasetType getDatasetType() {
        return DatasetType.TARGET;
    }

    public static String csvHeader() {
        return String.join(
                ",",
                "is_cheating",
                "delta_yaw",
                "delta_pitch",
                "accel_yaw",
                "accel_pitch",
                "target_error_yaw_norm",
                "target_error_pitch_norm",
                "target_error_delta_yaw",
                "target_error_delta_pitch",
                "target_distance",
                "ticks_since_attack");
    }

    @Override
    public String toCsvRow(boolean cheater) {
        return Stream.of(
                        cheater ? 1 : 0,
                        deltaYaw,
                        deltaPitch,
                        accelYaw,
                        accelPitch,
                        targetErrorYawNorm,
                        targetErrorPitchNorm,
                        targetErrorDeltaYaw,
                        targetErrorDeltaPitch,
                        targetDistance,
                        ticksSinceAttack)
                .map(value -> value instanceof Float ? String.format(Locale.US, "%.6f", value) : String.valueOf(value))
                .collect(Collectors.joining(","));
    }
}
