package dev.war.sentinel.utils;

import dev.war.sentinel.utils.CommandMatcher.Type;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CommandMatcherTest {

    private final CommandMatcher matcher = new CommandMatcher(List.of("l"), List.of(), List.of());

    @Test
    void matchesExactFirstTokenOnly() {
        assertEquals(Type.LOGIN, matcher.match("/login secret"));
        assertEquals(Type.LOGIN, matcher.match("/l secret"));
        assertEquals(Type.REGISTER, matcher.match("/register a a"));
        assertEquals(Type.CHANGEPASSWORD, matcher.match("/changepassword a b"));
        assertEquals(Type.LOGIN, matcher.match("/LOGIN secret"));
        assertEquals(Type.LOGIN, matcher.match("/sentinel:login secret"));
    }

    @Test
    void doesNotMatchPrefixesOrOtherCommands() {
        assertEquals(Type.NONE, matcher.match("/loginfoo secret"));
        assertEquals(Type.NONE, matcher.match("/registerx"));
        assertEquals(Type.NONE, matcher.match("/lobby"));
        assertEquals(Type.NONE, matcher.match("/r hello"));
        assertEquals(Type.NONE, matcher.match("/"));
        assertEquals(Type.NONE, matcher.match(""));
        assertEquals(Type.NONE, matcher.match(null));
    }

    @Test
    void aliasesComeFromConfigurationAndAreSanitised() {
        CommandMatcher custom = new CommandMatcher(List.of("In", "/entrar", "bad alias", ""), List.of("reg"), List.of("cp"));
        assertEquals(Type.LOGIN, custom.match("/in x"));
        assertEquals(Type.LOGIN, custom.match("/entrar x"));
        assertEquals(Type.REGISTER, custom.match("/reg x x"));
        assertEquals(Type.CHANGEPASSWORD, custom.match("/cp a b"));
        assertEquals(Type.NONE, custom.match("/l x"));
        assertFalse(custom.names().contains("bad alias"));
    }

    @Test
    void firstAliasOwnerWinsOnCollision() {
        CommandMatcher custom = new CommandMatcher(List.of("x"), List.of("x"), List.of());
        assertEquals(Type.LOGIN, custom.match("/x"));
    }

    @Test
    void splitsArguments() {
        assertArrayEquals(new String[]{"a", "b"}, CommandMatcher.arguments("/register a   b"));
        assertEquals(0, CommandMatcher.arguments("/login").length);
        assertEquals("login", CommandMatcher.firstToken("  /Login pw"));
    }

    @Test
    void logPatternCoversRealCommandsOnly() {
        Pattern p = matcher.logPattern();
        assertTrue(p.matcher("Steve issued server command: /login hunter2").matches());
        assertTrue(p.matcher("Steve issued server command: /register a a").matches());
        assertTrue(p.matcher("Steve issued server command: /changepassword a b").matches());
        assertTrue(p.matcher("Steve issued server command: /l hunter2").matches());
        assertTrue(p.matcher("Steve issued server command: /LOGIN hunter2").matches());
        assertTrue(p.matcher("Steve issued server command: /login").matches());
        assertFalse(p.matcher("Steve issued server command: /r hello").matches());
        assertFalse(p.matcher("Steve issued server command: /loginx a").matches());
        assertFalse(p.matcher("Steve issued server command: /home").matches());
    }

    @Test
    void logPatternFollowsConfiguredAliases() {
        CommandMatcher custom = new CommandMatcher(List.of("in"), List.of("r"), List.of());
        Pattern p = custom.logPattern();
        assertTrue(p.matcher("Steve issued server command: /in pw").matches());
        assertTrue(p.matcher("Steve issued server command: /r a a").matches());
        assertFalse(p.matcher("Steve issued server command: /l pw").matches());
    }
}
