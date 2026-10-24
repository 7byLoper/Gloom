package ru.gloom.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomConfigMessageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @SuppressWarnings("deprecation")
    void missingAndBlankMessagesStayDisabled() {
        Plugin plugin = (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, method, args) -> null);
        CustomConfig config = new CustomConfig("messages.yml", false, plugin, temporaryDirectory.toFile());

        assertEquals("", config.configMessage("missing"));
        assertEquals("", config.configMessage("missing", "fallback"));

        config.getConfig().set("blank", "   ");
        assertEquals("", config.configMessage("blank"));
    }
}
