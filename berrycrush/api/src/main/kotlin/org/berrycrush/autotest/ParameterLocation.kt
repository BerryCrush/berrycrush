package org.berrycrush.autotest

/**
 * Location of the parameter being tested.
 */
enum class ParameterLocation(
    val locationName: String,
) {
    BODY("body"),
    PATH("path"),
    QUERY("query"),
    HEADER("header"),
}
