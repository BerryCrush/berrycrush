package org.berrycrush.step.testscanner2

import org.berrycrush.step.Step

/**
 * Sample step definitions in second package for package scanning tests.
 */
class MoreStepsForScanning {
    @Step("I have {int} items in package 2")
    fun setItemCount(count: Int) {
        // Implementation
    }
}
