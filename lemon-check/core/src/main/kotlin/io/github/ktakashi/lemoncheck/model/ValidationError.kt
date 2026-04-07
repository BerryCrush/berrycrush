package io.github.ktakashi.lemoncheck.model

/**
 * Represents a schema validation error.
 *
 * @property path JSON path where the error occurred
 * @property message Description of the validation error
 * @property keyword JSON Schema keyword that failed
 * @property schemaPath Path in the schema where the constraint is defined
 */
data class ValidationError(
    val path: String,
    val message: String,
    val keyword: String? = null,
    val schemaPath: String? = null,
)
