package ru.gloom.manager.analytic.hologram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ru.gloom.GloomAI;
import ru.gloom.config.anticheat.HologramConfigManager;
import ru.gloom.manager.anticheat.PlayerAnalysisSnapshot;

@UtilityClass
public class HologramLineFormatter {
    private static final DecimalFormat PROB_FORMAT = new DecimalFormat("0.0000");
    private static final DecimalFormat BUFFER_FORMAT = new DecimalFormat("0.00");

    public List<String> buildLinesForTarget(@NonNull UUID targetId, @NonNull HologramConfigManager config) {
        Deque<Double> probabilities = GloomAI.INSTANCE.getViolationManager().getLocalProbabilities(targetId);
        PlayerAnalysisSnapshot snapshot = GloomAI.INSTANCE.getViolationManager().getAnalysisSnapshot(targetId);

        if (probabilities == null || probabilities.isEmpty()) {
            return buildFormattedLines(config, List.of(), 0.0D, snapshot.buffer());
        }

        List<Double> history = new ArrayList<>(probabilities);
        Collections.reverse(history);

        double sum = 0.0D;
        for (double probability : history) {
            sum += probability;
        }

        double averageProbability = sum / history.size();
        return buildFormattedLines(config, history, averageProbability, snapshot.buffer());
    }

    private List<String> buildFormattedLines(
            @NonNull HologramConfigManager config,
            @NonNull List<Double> history,
            double averageProbability,
            double buffer) {
        String formattedHistory = buildHistoryText(config, history);
        List<String> result = new ArrayList<>();

        for (String template : config.getLines()) {
            if (!template.contains("{history}")) {
                result.add(applyPlaceholders(template, formattedHistory, averageProbability, buffer));
                continue;
            }

            String[] historyLines = formattedHistory.split("\n", -1);
            for (String historyLine : historyLines) {
                result.add(applyPlaceholders(template, historyLine, averageProbability, buffer));
            }
        }

        return result;
    }

    private String buildHistoryText(@NonNull HologramConfigManager config, @NonNull List<Double> history) {
        int linesCount = Math.max(1, config.getHistoryLines());
        int valuesPerLine = Math.max(1, config.getHistoryProbsPerLine());
        int maxValues = linesCount * valuesPerLine;

        if (history.isEmpty()) {
            return "\n".repeat(Math.max(0, linesCount - 1));
        }

        int limit = Math.min(history.size(), maxValues);
        StringBuilder result = new StringBuilder(limit * 12);

        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                result.append(index % valuesPerLine == 0 ? '\n' : ' ');
            }

            result.append(formatProbabilityWithColor(history.get(index)));
        }

        return result.toString();
    }

    private String applyPlaceholders(
            @NonNull String lineTemplate, @NonNull String historyText, double averageProbability, double buffer) {
        return lineTemplate
                .replace("{history}", historyText)
                .replace("{avg_color}", GloomAI.INSTANCE.getMainConfigManager().getChanceColor(averageProbability))
                .replace("{avg}", formatProbability(averageProbability))
                .replace("{buffer}", formatBuffer(buffer));
    }

    private String formatProbabilityWithColor(double probability) {
        return GloomAI.INSTANCE.getMainConfigManager().getChanceColor(probability) + formatProbability(probability);
    }

    public String formatProbability(double probability) {
        return PROB_FORMAT.format(probability);
    }

    public String formatBuffer(double buffer) {
        return BUFFER_FORMAT.format(buffer);
    }
}
