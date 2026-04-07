package io.github.ktakashi.lemoncheck.junit

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Annotation to specify OpenAPI spec(s) for a LemonCheck test class.
 *
 * @property paths Paths to OpenAPI specification files
 * @property baseUrl Base URL override for API requests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckSpec(
    vararg val paths: String = [],
    val baseUrl: String = "",
)

/**
 * Annotation to mark a test method or class as providing LemonCheck scenarios.
 *
 * When applied, the LemonCheckExtension will generate test cases for each scenario.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckScenarios

/**
 * Annotation to filter scenarios by tags.
 *
 * @property include Only run scenarios with these tags
 * @property exclude Skip scenarios with these tags
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckTags(
    val include: Array<String> = [],
    val exclude: Array<String> = [],
)

/**
 * Annotation to configure timeout for scenario execution.
 *
 * @property value Timeout value
 * @property unit Time unit (default: seconds)
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckTimeout(
    val value: Long,
    val unit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.SECONDS,
)
