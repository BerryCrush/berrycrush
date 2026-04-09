package io.github.ktakashi.lemoncheck.step.testscanner2

import io.github.ktakashi.lemoncheck.step.Step

/**
 * Sample step definitions in second package for package scanning tests.
 */
class MoreStepsForScanning {
    @Step("I have {int} items in package 2")
    fun setItemCount(count: Int) {
        // Implementation
    }
}
