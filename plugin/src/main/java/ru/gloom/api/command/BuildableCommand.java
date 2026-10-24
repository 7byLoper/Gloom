package ru.gloom.api.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface BuildableCommand {
    void handle(@NotNull CommandSender commandSender, @NotNull String[] args);

    List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args);
}
