package ru.gloom.command.commands.gloom.subcommands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.command.BuildableCommand;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.api.model.data.DatasetType;
import ru.gloom.player.GloomPlayer;

@SubCommandRegister(permission = "gloom.command.datacollect", aliases = "datacollect")
public final class DataCollectSubCommand implements BuildableCommand {
    private static final String START_PERMISSION = "gloom.command.datacollect.start";
    private static final String STOP_PERMISSION = "gloom.command.datacollect.stop";

    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] args) {
        if (args.length < 2) {
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStartHelp());
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStopHelp());
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> handleStart(commandSender, args);
            case "stop" -> handleStop(commandSender, args);
            default -> {
                commandSender.sendMessage(
                        GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStartHelp());
                commandSender.sendMessage(
                        GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStopHelp());
            }
        }
    }

    private void handleStart(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(START_PERMISSION)) {
            return;
        }

        if (args.length < 5) {
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStartHelp());
            return;
        }

        String targetName = args[2];
        String type = args[3].toLowerCase(Locale.ROOT);
        DatasetType datasetType = DatasetType.fromName(args[4]);

        if (datasetType == null) {
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStartHelp());
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            commandSender.sendMessage(GloomAI.INSTANCE
                    .getDataCollectConfigManager()
                    .getMessageAllOffline()
                    .replace("{target}", targetName));
            return;
        }

        GloomPlayer gloomPlayer = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(targetPlayer.getUniqueId());
        if (gloomPlayer == null) {
            return;
        }

        if (gloomPlayer.getTrainData().isDatasetsCollecting()) {
            commandSender.sendMessage(GloomAI.INSTANCE
                    .getDataCollectConfigManager()
                    .getMessageAllAlreadyCollecting()
                    .replace("{target}", targetName));
            return;
        }

        if (type.equals("cheat")) {
            if (args.length < 6) {
                commandSender.sendMessage(
                        GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStartNoneSelected());
                return;
            }

            setDatasetOwner(commandSender, gloomPlayer);
            gloomPlayer.startDatasetsCollection(args[5], true, datasetType);
        } else if (type.equals("legit")) {
            setDatasetOwner(commandSender, gloomPlayer);
            gloomPlayer.startDatasetsCollection("legit", false, datasetType);
        } else {
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageAllNoneType());
            return;
        }

        commandSender.sendMessage(GloomAI.INSTANCE
                .getDataCollectConfigManager()
                .getMessageAllStart()
                .replace("{target}", gloomPlayer.getName()));
    }

    private void handleStop(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(STOP_PERMISSION)) {
            return;
        }

        if (args.length < 3) {
            commandSender.sendMessage(
                    GloomAI.INSTANCE.getDataCollectConfigManager().getMessageStopHelp());
            return;
        }

        String targetName = args[2];

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            commandSender.sendMessage(GloomAI.INSTANCE
                    .getDataCollectConfigManager()
                    .getMessageAllOffline()
                    .replace("{target}", targetName));
            return;
        }

        GloomPlayer gloomPlayer = GloomAI.INSTANCE.getPlayerDataManager().getPlayer(targetPlayer.getUniqueId());
        if (gloomPlayer == null) {
            return;
        }

        if (!gloomPlayer.getTrainData().isDatasetsCollecting()) {
            commandSender.sendMessage(GloomAI.INSTANCE
                    .getDataCollectConfigManager()
                    .getMessageStopNoneCollecting()
                    .replace("{target}", targetName));
            return;
        }

        gloomPlayer.stopDatasetsCollection();
        commandSender.sendMessage(GloomAI.INSTANCE
                .getDataCollectConfigManager()
                .getMessageAllStop()
                .replace("{target}", gloomPlayer.getName()));
    }

    private void setDatasetOwner(CommandSender commandSender, GloomPlayer gloomPlayer) {
        if (commandSender instanceof Player player) {
            gloomPlayer.getTrainData().setDatasetsOwner(player.getUniqueId());
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        if (args.length == 2) {
            return Stream.of("start", "stop")
                    .filter(command -> hasPermission(commandSender, command))
                    .filter(command -> command.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3 && isStartOrStop(args[1])) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(player -> player.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("start")) {
            return Stream.of("legit", "cheat")
                    .filter(line -> line.startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 5 && args[1].equalsIgnoreCase("start")) {
            return Stream.of(DatasetType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .filter(type -> type.startsWith(args[4].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 6 && args[1].equalsIgnoreCase("start") && args[3].equalsIgnoreCase("cheat")) {
            return Stream.of("name")
                    .filter(line -> line.toLowerCase(Locale.ROOT).startsWith(args[5].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return Collections.emptyList();
    }

    private boolean hasPermission(CommandSender commandSender, String command) {
        return switch (command) {
            case "start" -> commandSender.hasPermission(START_PERMISSION);
            case "stop" -> commandSender.hasPermission(STOP_PERMISSION);
            default -> false;
        };
    }

    private boolean isStartOrStop(String command) {
        return command.equalsIgnoreCase("start") || command.equalsIgnoreCase("stop");
    }
}
