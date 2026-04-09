package io.github.ktakashi.lemoncheck.step

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Tests for [PackageStepScanner].
 */
class PackageScanningTest {
    private val scanner = PackageStepScanner()

    @Test
    fun `scan package finds annotated classes`() {
        // Scan the package containing our test step classes
        val definitions = scanner.scan("io.github.ktakashi.lemoncheck.step.testscanner")

        // Should find steps from SampleStepsForScanning class
        assertTrue(definitions.isNotEmpty(), "Should find step definitions")
    }

    @Test
    fun `scan package returns empty for package without steps`() {
        // Scan a package that doesn't have step definitions
        val definitions = scanner.scan("io.github.ktakashi.lemoncheck.nonexistent")

        assertTrue(definitions.isEmpty())
    }

    @Test
    fun `scan multiple packages`() {
        val definitions =
            scanner.scanAll(
                "io.github.ktakashi.lemoncheck.step.testscanner",
                "io.github.ktakashi.lemoncheck.step.testscanner2",
            )

        // Should find definitions from both packages
        assertTrue(definitions.size >= 2)
    }
}
