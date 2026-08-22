package ru.gloom.manager;

import lombok.RequiredArgsConstructor;
import ru.gloom.GloomAI;
import ru.gloom.checks.impl.ai.AimAI;
import ru.gloom.player.GloomPlayer;

@RequiredArgsConstructor
public class AIResultManager {
    public void handleAnalyzeResult(GloomPlayer gloomPlayer, double chance) {
        if (gloomPlayer == null) {
            return;
        }

        AimAI check = gloomPlayer.getCheckManager().getAimAI();
        if (check == null) {
            return;
        }

        check.handleAnalyzeResult(chance);
    }

    public void handleAnalyzeResult(String username, double chance) {
        GloomPlayer gloomPlayer = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(username);
        if (gloomPlayer == null) {
            return;
        }

        AimAI check = gloomPlayer.getCheckManager().getAimAI();
        if (check == null) {
            return;
        }

        check.handleAnalyzeResult(chance);
    }
}
