package org.berrycrush.junit.glue

import org.berrycrush.assertion.Assertion

class CustomAssertions {
    @Assertion("the param name {string} must be {any}")
    fun checkValue(name: String, expected: Any?) {
        println(name)
    }
}