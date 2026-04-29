package org.berrycrush.junit.engine

import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

/**
 * Test descriptor for a multi-request idempotency test.
 *
 * Represents a single multi-test mode (sequential or concurrent) in the JUnit test tree.
 */
class MultiTestDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    val mode: MultiMode,
    val requestCount: Int,
) : AbstractTestDescriptor(uniqueId, displayName) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    override fun getSource(): java.util.Optional<TestSource> = java.util.Optional.empty()

    companion object {
        /**
         * Create a display name for a multi-test.
         * Format: [multi:{mode}] {count} requests
         */
        fun createDisplayName(
            mode: MultiMode,
            requestCount: Int,
        ): String {
            val modeLabel = mode.name.lowercase()
            return "[multi:$modeLabel] $requestCount requests"
        }

        /**
         * Create a display name from a MultiTestResult.
         */
        fun createDisplayName(result: MultiTestResult): String = createDisplayName(result.mode, result.requestCount)

        /**
         * Build a failure message for a multi-test result.
         */
        fun buildFailureMessage(result: MultiTestResult): String =
            buildString {
                append(createDisplayName(result))
                append("\n")
                append("  ${result.failureReason ?: "Unknown failure"}")
                append("\n  Duration: ${result.totalDurationMs}ms")
                append("\n  Results:")
                result.results.forEach { requestResult ->
                    append("\n    Request #${requestResult.requestIndex + 1}: ")
                    append("status=${requestResult.statusCode}, ")
                    append("time=${requestResult.durationMs}ms")
                    requestResult.threadName?.let { append(", thread=$it") }
                }
            }
    }
}
