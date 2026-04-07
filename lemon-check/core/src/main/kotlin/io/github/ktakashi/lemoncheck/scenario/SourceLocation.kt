package io.github.ktakashi.lemoncheck.scenario

/**
 * Represents a location in the source file.
 *
 * @property line The line number (1-based)
 * @property column The column number (1-based)
 * @property file Optional file name or path
 */
data class SourceLocation(
    val line: Int,
    val column: Int,
    val file: String? = null,
) {
    override fun toString(): String {
        val filePrefix = file?.let { "$it:" } ?: ""
        return "$filePrefix$line:$column"
    }
}
