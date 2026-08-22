package ru.gloom.api.model.analyze;

import java.util.List;
import ru.gloom.api.model.frame.RotationFrame;
import ru.gloom.player.GloomPlayer;

public record AnalyticData(String username, List<RotationFrame> rotationFrames) {

    public static AnalyticData createData(GloomPlayer gloomPlayer) {
        final List<RotationFrame> rotationFrames =
                gloomPlayer.getCheckManager().getAimAI().getRotationBuffer().getSnapshot();

        gloomPlayer.getCheckManager().getAimAI().setLastAnalyzedFrames(rotationFrames);

        return new AnalyticData(gloomPlayer.getName(), rotationFrames);
    }
}
