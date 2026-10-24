package ru.gloom.api.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public record BuildableCommandWrapper(BuildableCommand command, List<String> aliases, String permission) {
    public boolean hasPermission(@NotNull CommandSender sender) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        return sender.hasPermission(permission);
    }
}
