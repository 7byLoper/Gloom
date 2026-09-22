package ru.gloom.command.commands.gloom.subcommands;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.gloom.GloomAI;
import ru.gloom.api.command.BuildableCommand;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.config.MainConfigManager;
import ru.gloom.utils.MessageUtils;

@SubCommandRegister(permission = "gloom.command.help", aliases = "help")
public class HelpSubCommand implements BuildableCommand {
    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        sendHelp(sender);
    }

    public static void sendHelp(@NotNull CommandSender sender) {
        MainConfigManager config = GloomAI.INSTANCE.getMainConfigManager();
        MessageUtils.send(sender, config.getHelpHeaderMessage());

        sendLine(sender, config, "gloom help", "Просмотр списка команд");
        sendLine(sender, config, "gloom status", "Статус подключения к AI и тариф");
        sendLine(sender, config, "gloom alerts", "Включить/выключить оповещения о читерах");
        sendLine(sender, config, "gloom verbose", "Подробный вывод анализа (тестовый режим)");
        sendLine(sender, config, "gloom holograms", "Включить/выключить голограммы над игроками");
        sendLine(sender, config, "gloom history <игрок>", "История нарушений игрока");
        sendLine(sender, config, "gloom menu", "Открыть графическое меню");
        sendLine(sender, config, "gloom monitor <игрок>", "Мониторинг игрока в реальном времени");
        sendLine(sender, config, "gloom connect <код>", "Подключить сервер к кабинету");
        sendLine(sender, config, "gloom reload", "Перезагрузить конфигурацию");

        MessageUtils.send(sender, config.getHelpFooterMessage());
    }

    private static void sendLine(CommandSender sender, MainConfigManager config, String command, String description) {
        MessageUtils.send(sender, config.getHelpFormatMessage()
                .replace("{command}", command)
                .replace("{description}", description));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        return List.of();
    }
}
