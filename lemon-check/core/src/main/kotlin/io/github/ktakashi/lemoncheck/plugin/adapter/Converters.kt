package io.github.ktakashi.lemoncheck.plugin.adapter

import io.github.ktakashi.lemoncheck.plugin.ResultStatus
import io.github.ktakashi.lemoncheck.model.ResultStatus as MResultStatus

internal fun MResultStatus.mapTo() =
    when (this) {
        MResultStatus.PASSED -> ResultStatus.PASSED
        MResultStatus.FAILED -> ResultStatus.FAILED
        MResultStatus.SKIPPED -> ResultStatus.SKIPPED
        MResultStatus.ERROR -> ResultStatus.ERROR
        MResultStatus.PENDING -> ResultStatus.SKIPPED
    }
