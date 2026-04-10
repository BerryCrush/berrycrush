package io.github.ktakashi.lemoncheck.report

/**
 * Utility object for building test summary statistics from result collections.
 *
 * Provides reusable methods for counting test results by status,
 * eliminating duplication across reporters and plugins.
 */
object TestSummaryBuilder {
    /**
     * Build a TestSummary from a list of items with status.
     *
     * @param T The type of items in the list
     * @param items The list of items to summarize
     * @param statusExtractor Function to extract the status string from each item
     * @return A [TestSummary] with counts for each status
     */
    fun <T> buildSummary(
        items: List<T>,
        statusExtractor: (T) -> String,
    ): TestSummary {
        var passed = 0
        var failed = 0
        var skipped = 0
        var errors = 0

        for (item in items) {
            when (statusExtractor(item).uppercase()) {
                "PASSED" -> passed++
                "FAILED" -> failed++
                "SKIPPED" -> skipped++
                "ERROR" -> errors++
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

    /**
     * Build a TestSummary from items that have a ResultStatus.
     *
     * @param items The list of items to summarize
     * @param statusExtractor Function to extract ResultStatus from each item
     * @return A [TestSummary] with counts for each status
     */
    fun <T> fromModelStatus(
        items: List<T>,
        statusExtractor: (T) -> io.github.ktakashi.lemoncheck.model.ResultStatus,
    ): TestSummary {
        var passed = 0
        var failed = 0
        var skipped = 0
        var errors = 0

        for (item in items) {
            when (statusExtractor(item)) {
                io.github.ktakashi.lemoncheck.model.ResultStatus.PASSED -> passed++
                io.github.ktakashi.lemoncheck.model.ResultStatus.FAILED -> failed++
                io.github.ktakashi.lemoncheck.model.ResultStatus.SKIPPED -> skipped++
                io.github.ktakashi.lemoncheck.model.ResultStatus.ERROR -> errors++
                io.github.ktakashi.lemoncheck.model.ResultStatus.PENDING -> skipped++
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

    /**
     * Build a TestSummary from items that have a plugin ResultStatus.
     *
     * @param items The list of items to summarize
     * @param statusExtractor Function to extract ResultStatus from each item
     * @return A [TestSummary] with counts for each status
     */
    fun <T> fromPluginStatus(
        items: List<T>,
        statusExtractor: (T) -> io.github.ktakashi.lemoncheck.plugin.ResultStatus,
    ): TestSummary {
        var passed = 0
        var failed = 0
        var skipped = 0
        var errors = 0

        for (item in items) {
            when (statusExtractor(item)) {
                io.github.ktakashi.lemoncheck.plugin.ResultStatus.PASSED -> passed++
                io.github.ktakashi.lemoncheck.plugin.ResultStatus.FAILED -> failed++
                io.github.ktakashi.lemoncheck.plugin.ResultStatus.SKIPPED -> skipped++
                io.github.ktakashi.lemoncheck.plugin.ResultStatus.ERROR -> errors++
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
