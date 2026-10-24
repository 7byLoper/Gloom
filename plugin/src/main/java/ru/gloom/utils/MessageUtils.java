package ru.gloom.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@UtilityClass
public class MessageUtils {
    public void send(CommandSender recipient, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        recipient.sendMessage(message);
    }

    public void sendActionBar(Player recipient, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        recipient.sendActionBar(message);
    }
}
