package io.github.ktakashi.lemoncheck.model

/**
 * Result status of a step or scenario execution.
 */
enum class ResultStatus {
    /** Step/scenario passed all assertions */
    PASSED,

    /** Step/scenario failed one or more assertions */
    FAILED,

    /** Step/scenario was skipped (e.g., due to previous failure) */
    SKIPPED,

    /** Step/scenario encountered an error during execution */
    ERROR,

    /** Step/scenario is pending (not yet implemented) */
    PENDING,
}
