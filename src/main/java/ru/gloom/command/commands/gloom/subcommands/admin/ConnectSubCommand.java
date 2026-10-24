package ru.gloom.command.commands.gloom.subcommands.admin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.command.BuildableCommand;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.config.MainConfigManager;
import ru.gloom.utils.MessageUtils;

@SubCommandRegister(permission = "gloom.command.connect", aliases = "connect")
public final class ConnectSubCommand implements BuildableCommand {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final Gson gson = new Gson();

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        GloomAI plugin = GloomAI.INSTANCE;
        MainConfigManager config = plugin.getMainConfigManager();
        if (args.length == 2 && args[1].equalsIgnoreCase("status")) {
            MessageUtils.send(sender, plugin.getChecksConfigManager().getLicenseKey().isBlank()
                    ? config.getConnectStatusMissingMessage()
                    : config.getConnectStatusSavedMessage());
            return;
        }
        if (args.length != 2 || !args[1].matches("(?:[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}|gloom_[A-Za-z0-9_-]{43})")) {
            MessageUtils.send(sender, config.getConnectUsageMessage());
            return;
        }
        final HttpRequest request;
        try {
            URI panel = URI.create(plugin.getChecksConfigManager().getPanelUrl());
            if (!"https".equalsIgnoreCase(panel.getScheme()) || panel.getHost() == null
                    || panel.getUserInfo() != null || panel.getQuery() != null || panel.getFragment() != null) {
                throw new IllegalArgumentException("Invalid panel URL");
            }
            request = HttpRequest.newBuilder(URI.create(panel.toString().replaceAll("/+$", "")
                            + "/api/studio/integration/connect"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(Map.of("credential", args[1]))))
                    .build();
        } catch (IllegalArgumentException exception) {
            MessageUtils.send(sender, config.getConnectInvalidPanelUrlMessage());
            return;
        }
        if (!connecting.compareAndSet(false, true)) {
            MessageUtils.send(sender, config.getConnectAlreadyConnectingMessage());
            return;
        }
        MessageUtils.send(sender, config.getConnectConnectingMessage());
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, failure) -> {
            connecting.set(false);
            if (!plugin.isEnabled()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (failure != null) {
                    MessageUtils.send(sender, config.getConnectNetworkErrorMessage());
                    return;
                }
                if (response.statusCode() != 200) {
                    MessageUtils.send(sender, response.statusCode() == 429
                            ? config.getConnectRateLimitedMessage()
                            : config.getConnectRejectedMessage());
                    return;
                }
                try {
                    JsonObject body = gson.fromJson(response.body(), JsonObject.class);
                    String key = body.get("key").getAsString();
                    if (!key.matches("gloom_[A-Za-z0-9_-]{43}")) {
                        throw new IllegalArgumentException("Invalid key response");
                    }
                    plugin.getChecksConfigManager().saveLicenseKey(key);
                    plugin.getAnalyzeBatchDispatcher().retryNow();
                    MessageUtils.send(sender, config.getConnectSuccessMessage());
                } catch (IOException exception) {
                    MessageUtils.send(sender, config.getConnectSaveErrorMessage());
                } catch (RuntimeException exception) {
                    MessageUtils.send(sender, config.getConnectInvalidResponseMessage());
                }
            });
        });
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return args.length == 2 && "status".startsWith(args[1].toLowerCase()) ? List.of("status") : List.of();
    }
}
