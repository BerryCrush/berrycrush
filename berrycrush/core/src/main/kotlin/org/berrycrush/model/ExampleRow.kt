package org.berrycrush.model

/**
 * A row of example data for scenario outline parameterization.
 *
 * @property values Map of parameter name to value
 */
data class ExampleRow(
    val values: Map<String, Any>,
)
