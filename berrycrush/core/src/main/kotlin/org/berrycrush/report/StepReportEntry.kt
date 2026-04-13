package org.berrycrush.report

import org.berrycrush.plugin.AssertionFailure
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.ResultStatus
import java.time.Duration

/**
 * Report entry for a single step with request/response/assertion details.
 *
 * Contains all execution information needed for detailed test reporting including
 * HTTP request/response details and failure diagnostics.
 *
 * @property description Step description text
 * @property status Step execution status (PASSED, FAILED, SKIPPED, ERROR)
 * @property duration Step execution time
 * @property request HTTP request details (null for non-HTTP steps)
 * @property response HTTP response details (null until response received)
 * @property failure Detailed failure information if status is FAILED (null otherwise)
 */
data class StepReportEntry(
    val description: String,
    val status: ResultStatus,
    val duration: Duration,
    val request: HttpRequest? = null,
    val response: HttpResponse? = null,
    val failure: AssertionFailure? = null,
)
