package org.berrycrush.plugin.adapter

import org.berrycrush.plugin.ResultStatus
import org.berrycrush.model.ResultStatus as MResultStatus

internal fun MResultStatus.mapTo() =
    when (this) {
        MResultStatus.PASSED -> ResultStatus.PASSED
        MResultStatus.FAILED -> ResultStatus.FAILED
        MResultStatus.SKIPPED -> ResultStatus.SKIPPED
        MResultStatus.ERROR -> ResultStatus.ERROR
        MResultStatus.PENDING -> ResultStatus.SKIPPED
    }
