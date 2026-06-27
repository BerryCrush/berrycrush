package org.berrycrush.report

import org.berrycrush.plugin.ResultStatus

/**
 * Utility object for building test summary statistics from result collections.
 *
 * Provides reusable methods for counting test results by status,
 * eliminating duplication across reporters and plugins.
 */
object TestSummaryBuilder {
    /**
     * Build a TestSummary from items that have a plugin ResultStatus.
     *
     * @param items The list of items to summarize
     * @param statusExtractor Function to extract ResultStatus from each item
     * @return A [TestSummary] with counts for each status
     */
    fun <T> fromPluginStatus(
        items: List<T>,
        statusExtractor: (T) -> ResultStatus,
    ): TestSummary {
        var passed = 0
        var failed = 0
        var skipped = 0
        var errors = 0

        for (item in items) {
            when (statusExtractor(item)) {
                ResultStatus.PASSED -> passed++
                ResultStatus.FAILED -> failed++
                ResultStatus.SKIPPED -> skipped++
                ResultStatus.ERROR -> errors++
            }
        }

        return TestSummary(
            total = items.size,
            passed = passed,
            failed = failed,
            skipped = skipped,
            errors = errors,
        )
    }
}
