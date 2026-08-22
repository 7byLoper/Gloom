package ru.gloom.service.analyze;

import com.google.flatbuffers.FlatBufferBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.gloom.api.model.frame.TargetAimFrame;
import ru.gloom.checks.impl.ai.TargetAimAI;
import ru.gloom.config.anticheat.ChecksConfigManager;
import ru.gloom.player.GloomPlayer;
import ru.gloom.protocol.flatbuffers.AnalyzeRequest;

@RequiredArgsConstructor
public final class TargetFlatBufferAnalyzeService {
    private static final int FEATURES_PER_FRAME = 10;

    private final ChecksConfigManager configManager;
    private final AnalyzeBatchDispatcher dispatcher;

    public void analyzePlayerFrames(GloomPlayer gloomPlayer) {
        TargetAimAI targetAimAI = gloomPlayer.getCheckManager().getTargetAimAI();
        if (!targetAimAI.getTargetAimBuffer().isFull()) {
            return;
        }

        Player bukkitPlayer = gloomPlayer.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        List<TargetAimFrame> frames =
                targetAimAI.getTargetAimBuffer().pollSnapshot(configManager.getTargetAnalysisStep());
        if (frames == null || frames.isEmpty()) {
            return;
        }
        targetAimAI.setLastAnalyzedFrames(frames);

        try {
            dispatcher.enqueue(encodeRequest(bukkitPlayer.getName(), frames), gloomPlayer);
        } catch (RuntimeException exception) {
            Bukkit.getLogger().warning("[GloomAI] Target FlatBuffers request build failed: " + exception.getMessage());
        }
    }

    private byte[] encodeRequest(String playerName, List<TargetAimFrame> frames) {
        FlatBufferBuilder builder = new FlatBufferBuilder(64 + frames.size() * FEATURES_PER_FRAME * Float.BYTES);
        int nameOffset = builder.createString(playerName);
        int featuresOffset = AnalyzeRequest.createFeaturesVector(builder, flattenFeatures(frames));
        int requestOffset = AnalyzeRequest.createAnalyzeRequest(builder, nameOffset, frames.size(), featuresOffset);
        AnalyzeRequest.finishAnalyzeRequestBuffer(builder, requestOffset);
        return builder.sizedByteArray();
    }

    private float[] flattenFeatures(List<TargetAimFrame> frames) {
        float[] features = new float[frames.size() * FEATURES_PER_FRAME];
        for (int index = 0; index < frames.size(); index++) {
            TargetAimFrame frame = frames.get(index);
            int base = index * FEATURES_PER_FRAME;

            features[base] = frame.getDeltaYaw();
            features[base + 1] = frame.getDeltaPitch();
            features[base + 2] = frame.getAccelYaw();
            features[base + 3] = frame.getAccelPitch();
            features[base + 4] = frame.getTargetErrorYawNorm();
            features[base + 5] = frame.getTargetErrorPitchNorm();
            features[base + 6] = frame.getTargetErrorDeltaYaw();
            features[base + 7] = frame.getTargetErrorDeltaPitch();
            features[base + 8] = frame.getTargetDistance();
            features[base + 9] = frame.getTicksSinceAttack();
        }
        return features;
    }
}
