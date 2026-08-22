package ru.gloom.command.commands.gloom.subcommands;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.command.BuildableCommand;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.player.GloomPlayer;

@SubCommandRegister(permission = "gloom.command.reload", aliases = "reload")
public class ReloadSubCommand implements BuildableCommand {
    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] args) {
        commandSender.sendMessage(GloomAI.INSTANCE.getMainConfigManager().getReloadingMessage());

        GloomAI.INSTANCE.getMainConfigManager().reloadAll();
        GloomAI.INSTANCE.getChecksConfigManager().reloadAll();
        GloomAI.INSTANCE.getHologramConfigManager().reloadAll();
        GloomAI.INSTANCE.getDataCollectConfigManager().reloadAll();
        GloomAI.INSTANCE.getPunishmentConfigManager().reloadAll();
        GloomAI.INSTANCE.getPlayerDataManager().getEntries().forEach(GloomPlayer::reload);

        commandSender.sendMessage(GloomAI.INSTANCE.getMainConfigManager().getReloadedMessage());
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        return List.of();
    }
}
