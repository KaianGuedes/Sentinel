package dev.war.sentinel.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftVersionTest {

    @Test
    void parsesClassicVersions() {
        MinecraftVersion v = MinecraftVersion.parse("1.20.1-R0.1-SNAPSHOT");
        assertEquals(new MinecraftVersion(1, 20, 1), v);
        assertEquals(new MinecraftVersion(1, 21, 11), MinecraftVersion.parse("1.21.11-R0.1-SNAPSHOT"));
        assertEquals(new MinecraftVersion(1, 20, 0), MinecraftVersion.parse("1.20"));
    }

    @Test
    void parsesNewNumberingAndBuildSuffixes() {
        assertEquals(new MinecraftVersion(26, 3, 0), MinecraftVersion.parse("26.3-R0.1-SNAPSHOT"));
        assertEquals(new MinecraftVersion(26, 3, 1), MinecraftVersion.parse("26.3.1-R0.1-SNAPSHOT"));
        assertEquals(new MinecraftVersion(26, 3, 1), MinecraftVersion.parse("26.3.1.build.5-stable"));
        assertEquals(new MinecraftVersion(26, 3, 0), MinecraftVersion.parse("26.3-249-main@1a995d6"));
    }

    @Test
    void unparseableIsNull() {
        assertNull(MinecraftVersion.parse(null));
        assertNull(MinecraftVersion.parse(""));
        assertNull(MinecraftVersion.parse("snapshot"));
        assertNull(MinecraftVersion.parse("1"));
        assertNull(MinecraftVersion.parse("99999999999.1"));
    }

    @Test
    void comparesNumericallyNotAsText() {
        assertTrue(MinecraftVersion.parse("1.21.11").isAtLeast(1, 21));
        assertTrue(MinecraftVersion.parse("1.21").isAtLeast(1, 21));
        assertFalse(MinecraftVersion.parse("1.20.6").isAtLeast(1, 21));
        assertFalse(MinecraftVersion.parse("1.9.4").isAtLeast(1, 21));
        assertTrue(MinecraftVersion.parse("1.20.1").isAtLeast(1, 20));
        assertTrue(MinecraftVersion.parse("26.3").isAtLeast(1, 21));
        assertTrue(MinecraftVersion.parse("26.3.1").isAtLeast(1, 21));
        assertTrue(MinecraftVersion.parse("26.3.1").compareTo(MinecraftVersion.parse("26.3")) > 0);
        assertTrue(MinecraftVersion.parse("1.21.11").compareTo(MinecraftVersion.parse("1.21.4")) > 0);
    }
}
