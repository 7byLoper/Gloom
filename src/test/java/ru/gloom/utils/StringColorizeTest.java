package ru.gloom.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringColorizeTest {

    @Test
    void testGradientParsing() {
        String input = "<gradient:#FFFFFF:#777777>Gloom</gradient> &8»";
        String parsed = StringColorize.parse(input);
        assertNotNull(parsed);
        assertFalse(parsed.contains("<gradient:"));
        assertTrue(parsed.contains("§x§f§f§f§f§f§fG"));
        assertTrue(parsed.contains("§8»"));
    }

    @Test
    void testGradientWithFormatting() {
        String input = "<gradient:#FFFFFF:#666666:&l>AI</gradient>";
        String parsed = StringColorize.parse(input);
        assertNotNull(parsed);
        assertFalse(parsed.contains("<gradient:"));
        assertTrue(parsed.contains("§lA"));
    }

    @Test
    void testAmpHexAndLegacyColors() {
        String input = "&#FFFFFFTest &7Message";
        String parsed = StringColorize.parse(input);
        assertNotNull(parsed);
        assertTrue(parsed.toLowerCase().contains("§x§f§f§f§f§f§ftest"));
        assertTrue(parsed.contains("§7Message"));
    }

    @Test
    void testNullAndEmpty() {
        assertNull(StringColorize.parse((String) null));
        assertEquals("", StringColorize.parse(""));
    }
}
