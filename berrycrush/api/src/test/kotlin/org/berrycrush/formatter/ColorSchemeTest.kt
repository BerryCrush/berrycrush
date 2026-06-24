package org.berrycrush.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for ColorScheme configuration class.
 */
class ColorSchemeTest {
    @Test
    fun `DEFAULT scheme has green for passed`() {
        val scheme = ColorScheme.DEFAULT
        assertEquals(AnsiColors.GREEN, scheme.passed)
    }

    @Test
    fun `DEFAULT scheme has red for failed`() {
        val scheme = ColorScheme.DEFAULT
        assertEquals(AnsiColors.RED, scheme.failed)
    }

    @Test
    fun `DEFAULT scheme has gray for skipped`() {
        val scheme = ColorScheme.DEFAULT
        assertEquals(AnsiColors.GRAY, scheme.skipped)
    }

    @Test
    fun `DEFAULT scheme has yellow for error`() {
        val scheme = ColorScheme.DEFAULT
        assertEquals(AnsiColors.YELLOW, scheme.error)
    }

    @Test
    fun `NONE scheme has empty strings for all colors`() {
        val scheme = ColorScheme.NONE
        assertEquals("", scheme.passed)
        assertEquals("", scheme.failed)
        assertEquals("", scheme.skipped)
        assertEquals("", scheme.error)
        assertEquals("", scheme.customHighlight)
        assertEquals("", scheme.header)
    }

    @Test
    fun `highlight wraps text with custom highlight color`() {
        val scheme = ColorScheme.DEFAULT
        val result = scheme.highlight("custom step")
        assertTrue(result.contains("custom step"))
        assertTrue(result.contains(AnsiColors.BOLD))
        assertTrue(result.contains(AnsiColors.BRIGHT_CYAN))
    }

    @Test
    fun `highlight with NONE scheme returns unchanged text`() {
        val scheme = ColorScheme.NONE
        val result = scheme.highlight("custom step")
        assertEquals("custom step", result)
    }

    @Test
    fun `headerStyle applies header color`() {
        val scheme = ColorScheme.DEFAULT
        val result = scheme.headerStyle("Header")
        assertTrue(result.contains(AnsiColors.BOLD))
        assertTrue(result.contains("Header"))
    }

    @Test
    fun `custom scheme allows custom colors`() {
        val customScheme =
            ColorScheme(
                passed = AnsiColors.BRIGHT_GREEN,
                failed = AnsiColors.BRIGHT_RED,
            )
        assertEquals(AnsiColors.BRIGHT_GREEN, customScheme.passed)
        assertEquals(AnsiColors.BRIGHT_RED, customScheme.failed)
    }

    @Test
    fun `HIGH_CONTRAST scheme has bold colors`() {
        val scheme = ColorScheme.HIGH_CONTRAST
        assertTrue(scheme.passed.contains(AnsiColors.BOLD))
        assertTrue(scheme.failed.contains(AnsiColors.BOLD))
        assertTrue(scheme.error.contains(AnsiColors.BOLD))
    }

    @Test
    fun `MONOCHROME scheme uses only styles`() {
        val scheme = ColorScheme.MONOCHROME
        // Monochrome uses bold for failed, dim for skipped
        assertEquals("", scheme.passed)
        assertEquals(AnsiColors.BOLD, scheme.failed)
        assertEquals(AnsiColors.DIM, scheme.skipped)
        // Should not contain color codes
        assertFalse(scheme.failed.contains(AnsiColors.RED))
    }
}
