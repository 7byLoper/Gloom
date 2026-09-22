package ru.gloom.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class MessageUtilsTest {
    @Test
    void sendsOnlyMessagesWithVisibleContent() {
        AtomicInteger sentMessages = new AtomicInteger();
        CommandSender recipient = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage")) {
                        sentMessages.incrementAndGet();
                    }
                    return null;
                });

        MessageUtils.send(recipient, null);
        MessageUtils.send(recipient, "");
        MessageUtils.send(recipient, "   ");
        assertEquals(0, sentMessages.get());

        MessageUtils.send(recipient, "message");
        assertEquals(1, sentMessages.get());
    }
}
