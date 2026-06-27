package org.berrycrush.plugin

import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse

/**
 * Detailed failure information for debugging test failures.
 *
 * Captures expected vs actual values, computed diffs, and HTTP request/response
 * snapshots at the time of failure to aid in diagnosing what went wrong.
 *
 * @property message Human-readable failure message explaining what failed
 * @property expected Expected value for comparison assertions (null if not applicable)
 * @property actual Actual value that was received (null if not applicable)
 * @property diff Computed difference between expected and actual (for string/object comparisons)
 * @property stepDescription Description of the step that failed
 * @property assertionType Type of assertion that failed (e.g., "status", "jsonpath", "header")
 * @property requestSnapshot HTTP request details captured at failure time (null if step had no request)
 * @property responseSnapshot HTTP response details captured at failure time (null if step had no response)
 */
data class AssertionFailure(
    val message: String,
    val expected: Any? = null,
    val actual: Any? = null,
    val diff: String? = null,
    val stepDescription: String,
    val assertionType: String,
    val requestSnapshot: HttpRequest? = null,
    val responseSnapshot: HttpResponse? = null,
)

/**
 * Result status for steps and scenarios.
 *
 * @property PASSED Step or scenario succeeded
 * @property FAILED Assertion failure occurred
 * @property SKIPPED Not executed due to dependency failure
 * @property ERROR Unexpected exception occurred during execution
 */
enum class ResultStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR,
}

/**
 * Step type enumeration.
 *
 * @property CALL HTTP API call step
 * @property ASSERT Assertion/validation step
 * @property EXTRACT Variable extraction step
 * @property CUSTOM User-defined custom step
 */
enum class StepType {
    CALL,
    ASSERT,
    EXTRACT,
    CUSTOM,
}
