package com.betterconfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigurationParserTest {
    private final static ConfigurationParser parser = new ConfigurationParser();

    @Test
    public void parseThrowsArgumentInvalid() {
        assertThrows(ParsingFailedException.class, () -> parser.parse(Object.class, null));
        assertThrows(ParsingFailedException.class, () -> parser.parse(Object.class, ""));
    }

    @Test
    public void parseValueThrowsArgumentInvalid() {
        assertThrows(ParsingFailedException.class, () -> parser.parseValue(Object.class, null, "key"));
        assertThrows(ParsingFailedException.class, () -> parser.parseValue(Object.class, "", "key"));
        assertThrows(ParsingFailedException.class, () -> parser.parseValue(Object.class, "config", null));
        assertThrows(ParsingFailedException.class, () -> parser.parseValue(Object.class, "config", ""));
        assertThrows(ParsingFailedException.class, () -> parser.parseValue(Object.class, "config", "key"));
    }

    @Test
    public void parseValueThrowsInvalidJson() {
        String badJson = "{ test: test] }";
        ParsingFailedException exp = assertThrows(ParsingFailedException.class, () -> parser.parseValue(String.class, badJson, "test"));
        assertEquals(badJson, exp.getJson());
    }
}
