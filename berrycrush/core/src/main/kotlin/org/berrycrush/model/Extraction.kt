package org.berrycrush.model

/**
 * Represents a value extraction from an API response.
 *
 * @property variableName Name to store the extracted value under
 * @property jsonPath JSONPath expression to extract the value
 * @property source Where to extract from (body, header, status)
 */
data class Extraction(
    val variableName: String,
    val jsonPath: String,
    val source: ExtractionSource = ExtractionSource.BODY,
) {
    init {
        require(variableName.isNotBlank()) { "Variable name cannot be blank" }
        require(jsonPath.isNotBlank()) { "JSONPath cannot be blank" }
    }
}

/**
 * Source for value extraction.
 */
enum class ExtractionSource {
    /** Extract from response body using JSONPath */
    BODY,

    /** Extract from response headers */
    HEADER,

    /** Extract status code */
    STATUS,
}
