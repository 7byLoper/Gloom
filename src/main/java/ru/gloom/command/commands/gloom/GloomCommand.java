package ru.gloom.command.commands.gloom;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.gloom.api.command.register.CommandRegister;
import ru.gloom.command.commands.gloom.subcommands.*;
import ru.gloom.command.handler.BaseCommandExecutor;

@CommandRegister(name = "gloom", permission = "gloom.command.use")
public class GloomCommand extends BaseCommandExecutor {
    @Override
    public String getNoPermissionMessage() {
        return "";
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new AlertsSubCommand());
        addSubCommand(new VerboseSubCommand());
        addSubCommand(new HologramsSubCommand());
        addSubCommand(new HistorySubCommand());
        addSubCommand(new MenuSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new MonitorSubCommand());
        addSubCommand(new DataCollectSubCommand());
    }

    @Override
    public String handleNoArguments(@NotNull CommandSender sender) {
        return "";
    }
}
