package ru.gloom.command.commands.gloom.subcommands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.command.BuildableCommand;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.config.MainConfigManager;
import ru.gloom.config.anticheat.ChecksConfigManager;
import ru.gloom.utils.MessageUtils;

@SubCommandRegister(permission = "gloom.command.status", aliases = "status")
public final class StatusSubCommand implements BuildableCommand {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Gson gson = new Gson();

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        GloomAI plugin = GloomAI.INSTANCE;
        MainConfigManager mainConfig = plugin.getMainConfigManager();
        ChecksConfigManager checksConfig = plugin.getChecksConfigManager();

        String licenseKey = checksConfig.getLicenseKey();
        if (licenseKey == null || licenseKey.isBlank()) {
            MessageUtils.send(sender, mainConfig.getStatusHeaderMessage());
            MessageUtils.send(sender, mainConfig.getStatusBackendMessage()
                    .replace("{status}", mainConfig.getStatusNotConnectedText()));
            MessageUtils.send(sender, mainConfig.getStatusNoLicenseMessage());
            return;
        }

        MessageUtils.send(sender, mainConfig.getStatusCheckingMessage());

        final HttpRequest request;
        try {
            String panelUrl = checksConfig.getPanelUrl();
            if (panelUrl == null || panelUrl.isBlank()) {
                panelUrl = "https://gloomai.pro";
            }
            URI panel = URI.create(panelUrl.replaceAll("/+$", "") + "/api/studio/integration/status");
            request = HttpRequest.newBuilder(panel)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("X-License-Key", licenseKey.strip())
                    .GET()
                    .build();
        } catch (IllegalArgumentException exception) {
            MessageUtils.send(sender, mainConfig.getStatusErrorMessage()
                    .replace("{error}", "Некорректный адрес панели: " + exception.getMessage()));
            return;
        }

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, failure) -> {
            if (!plugin.isEnabled()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (failure != null) {
                    MessageUtils.send(sender, mainConfig.getStatusHeaderMessage());
                    MessageUtils.send(sender, mainConfig.getStatusBackendMessage()
                            .replace("{status}", mainConfig.getStatusNotConnectedText()));
                    MessageUtils.send(sender, mainConfig.getStatusErrorMessage()
                            .replace("{error}", failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName()));
                    return;
                }

                if (response.statusCode() != 200) {
                    MessageUtils.send(sender, mainConfig.getStatusHeaderMessage());
                    MessageUtils.send(sender, mainConfig.getStatusBackendMessage()
                            .replace("{status}", mainConfig.getStatusNotConnectedText()));
                    MessageUtils.send(sender, mainConfig.getStatusErrorMessage()
                            .replace("{error}", "HTTP " + response.statusCode()));
                    return;
                }

                try {
                    JsonObject body = gson.fromJson(response.body(), JsonObject.class);
                    boolean connected = body.has("connected") && body.get("connected").getAsBoolean();
                    String modelName = body.has("model_name") && !body.get("model_name").isJsonNull()
                            ? body.get("model_name").getAsString()
                            : plugin.getAnalyzeBatchDispatcher().getLastModelName();
                    if (modelName != null && !modelName.isBlank()) {
                        plugin.getAnalyzeBatchDispatcher().setLastModelName(modelName);
                    }

                    int requestsToday = body.has("requests_today") ? body.get("requests_today").getAsInt() : 0;
                    int remaining = body.has("requests_remaining") ? body.get("requests_remaining").getAsInt() : -1;
                    String remainingStr = (remaining < 0)
                            ? mainConfig.getStatusUnlimitedText()
                            : String.format("%,d", remaining);

                    String planName = body.has("plan_name") && !body.get("plan_name").isJsonNull()
                            ? body.get("plan_name").getAsString()
                            : "Free";

                    MessageUtils.send(sender, mainConfig.getStatusHeaderMessage());
                    MessageUtils.send(sender, mainConfig.getStatusBackendMessage()
                            .replace("{status}", connected ? mainConfig.getStatusConnectedText() : mainConfig.getStatusNotConnectedText()));
                    MessageUtils.send(sender, mainConfig.getStatusRequestsTodayMessage()
                            .replace("{requests_today}", String.format("%,d", requestsToday)));
                    MessageUtils.send(sender, mainConfig.getStatusRequestsRemainingMessage()
                            .replace("{requests_remaining}", remainingStr));
                } catch (Exception parseException) {
                    MessageUtils.send(sender, mainConfig.getStatusErrorMessage()
                            .replace("{error}", "Ошибка обработки ответа: " + parseException.getMessage()));
                }
            });
        });
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
