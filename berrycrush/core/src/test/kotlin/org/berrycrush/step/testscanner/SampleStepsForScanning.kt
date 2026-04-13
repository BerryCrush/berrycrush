package org.berrycrush.step.testscanner

import org.berrycrush.step.Step

/**
 * Sample step definitions for package scanning tests.
 */
class SampleStepsForScanning {
    @Step("I have {int} items in package 1")
    fun setItemCount(count: Int) {
        // Implementation
    }

    @Step("the name in package 1 is {string}")
    fun setName(name: String) {
        // Implementation
    }
}
