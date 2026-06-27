package org.berrycrush.report

import org.berrycrush.formatter.AnsiColors
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for ColorScheme configuration class.
 */
class ColorSchemeTest {
    @Test
    fun `DEFAULT scheme has green for passed`() {
        val scheme = ColorScheme.DEFAULT
        Assertions.assertEquals(AnsiColors.GREEN, scheme.passed)
    }

    @Test
    fun `DEFAULT scheme has red for failed`() {
        val scheme = ColorScheme.DEFAULT
        Assertions.assertEquals(AnsiColors.RED, scheme.failed)
    }

    @Test
    fun `DEFAULT scheme has gray for skipped`() {
        val scheme = ColorScheme.DEFAULT
        Assertions.assertEquals(AnsiColors.GRAY, scheme.skipped)
    }

    @Test
    fun `DEFAULT scheme has yellow for error`() {
        val scheme = ColorScheme.DEFAULT
        Assertions.assertEquals(AnsiColors.YELLOW, scheme.error)
    }

    @Test
    fun `NONE scheme has empty strings for all colors`() {
        val scheme = ColorScheme.NONE
        Assertions.assertEquals("", scheme.passed)
        Assertions.assertEquals("", scheme.failed)
        Assertions.assertEquals("", scheme.skipped)
        Assertions.assertEquals("", scheme.error)
        Assertions.assertEquals("", scheme.customHighlight)
        Assertions.assertEquals("", scheme.header)
    }

    @Test
    fun `highlight wraps text with custom highlight color`() {
        val scheme = ColorScheme.DEFAULT
        val result = scheme.highlight("custom step")
        Assertions.assertTrue(result.contains("custom step"))
        Assertions.assertTrue(result.contains(AnsiColors.BOLD))
        Assertions.assertTrue(result.contains(AnsiColors.BRIGHT_CYAN))
    }

    @Test
    fun `highlight with NONE scheme returns unchanged text`() {
        val scheme = ColorScheme.NONE
        val result = scheme.highlight("custom step")
        Assertions.assertEquals("custom step", result)
    }

    @Test
    fun `headerStyle applies header color`() {
        val scheme = ColorScheme.DEFAULT
        val result = scheme.headerStyle("Header")
        Assertions.assertTrue(result.contains(AnsiColors.BOLD))
        Assertions.assertTrue(result.contains("Header"))
    }

    @Test
    fun `custom scheme allows custom colors`() {
        val customScheme =
            ColorScheme(
                passed = AnsiColors.BRIGHT_GREEN,
                failed = AnsiColors.BRIGHT_RED,
            )
        Assertions.assertEquals(AnsiColors.BRIGHT_GREEN, customScheme.passed)
        Assertions.assertEquals(AnsiColors.BRIGHT_RED, customScheme.failed)
    }

    @Test
    fun `HIGH_CONTRAST scheme has bold colors`() {
        val scheme = ColorScheme.HIGH_CONTRAST
        Assertions.assertTrue(scheme.passed.contains(AnsiColors.BOLD))
        Assertions.assertTrue(scheme.failed.contains(AnsiColors.BOLD))
        Assertions.assertTrue(scheme.error.contains(AnsiColors.BOLD))
    }

    @Test
    fun `MONOCHROME scheme uses only styles`() {
        val scheme = ColorScheme.MONOCHROME
        // Monochrome uses bold for failed, dim for skipped
        Assertions.assertEquals("", scheme.passed)
        Assertions.assertEquals(AnsiColors.BOLD, scheme.failed)
        Assertions.assertEquals(AnsiColors.DIM, scheme.skipped)
        // Should not contain color codes
        Assertions.assertFalse(scheme.failed.contains(AnsiColors.RED))
    }
}
