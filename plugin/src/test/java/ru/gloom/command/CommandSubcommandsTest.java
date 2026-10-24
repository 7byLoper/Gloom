package ru.gloom.command;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import ru.gloom.api.command.register.SubCommandRegister;
import ru.gloom.command.commands.gloom.subcommands.HelpSubCommand;
import ru.gloom.command.commands.gloom.subcommands.StatusSubCommand;
import ru.gloom.command.commands.gloom.subcommands.VerboseSubCommand;

import static org.junit.jupiter.api.Assertions.*;

class CommandSubcommandsTest {

    @Test
    void subcommandsHaveProperAnnotations() {
        SubCommandRegister helpReg = HelpSubCommand.class.getAnnotation(SubCommandRegister.class);
        assertNotNull(helpReg);
        assertEquals("gloom.command.help", helpReg.permission());
        assertArrayEquals(new String[]{"help"}, helpReg.aliases());

        SubCommandRegister statusReg = StatusSubCommand.class.getAnnotation(SubCommandRegister.class);
        assertNotNull(statusReg);
        assertEquals("gloom.command.status", statusReg.permission());
        assertArrayEquals(new String[]{"status"}, statusReg.aliases());

        SubCommandRegister verboseReg = VerboseSubCommand.class.getAnnotation(SubCommandRegister.class);
        assertNotNull(verboseReg);
        assertEquals("gloom.command.verbose", verboseReg.permission());
        assertArrayEquals(new String[]{"verbose"}, verboseReg.aliases());
    }

    @Test
    void pluginYmlDeclaresPermissionsWithFalseDefaults() {
        try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(in, "plugin.yml not found in classpath");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("gloom.bypass:"), "gloom.bypass permission must be defined in plugin.yml");
            assertTrue(content.contains("gloom.command.verbose.enable-on-join:"), "verbose.enable-on-join must be defined in plugin.yml");
            assertTrue(content.contains("gloom.command.help:"));
            assertTrue(content.contains("gloom.command.status:"));
            assertTrue(content.contains("default: false"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void settingsYmlContainsTestMode() {
        try (InputStream in = getClass().getResourceAsStream("/settings.yml")) {
            assertNotNull(in, "settings.yml not found in classpath");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("test_mode: false"), "settings.yml must contain test_mode");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void translationYmlContainsModelPlaceholderAndNewSections() {
        try (InputStream in = getClass().getResourceAsStream("/translation.yml")) {
            assertNotNull(in, "translation.yml not found in classpath");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("alert:"), "translation.yml must have alert");
            assertTrue(content.contains("{model}"), "alert/verbose must contain {model}");
            assertFalse(content.contains("ai-alert:"), "translation.yml must not have legacy ai-alert");
            assertFalse(content.contains("ai-verbose:"), "translation.yml must not have legacy ai-verbose");
            assertFalse(content.contains("RotationAimCheck"), "translation.yml must not hardcode RotationAimCheck");

            assertTrue(content.contains("help:"), "translation.yml must contain help section");
            assertTrue(content.contains("status:"), "translation.yml must contain status section");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void defaultPunishmentsUseUnifiedAlertAction() {
        try (InputStream in = getClass().getResourceAsStream("/anticheat/punishments.yml")) {
            assertNotNull(in, "punishments.yml not found in classpath");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("[alert]"), "punishments.yml must use [alert]");
            assertFalse(content.contains("[ai_alert]"), "punishments.yml must not use legacy [ai_alert]");
        } catch (Exception e) {
            fail(e);
        }
    }
}
