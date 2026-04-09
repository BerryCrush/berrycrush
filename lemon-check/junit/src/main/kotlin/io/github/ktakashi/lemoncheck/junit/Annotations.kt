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
 * When applied, the LemonCheckTestEngine will discover and execute scenario files
 * from the specified locations.
 *
 * @property locations Classpath locations to search for scenario files.
 *                     Supports glob patterns (e.g., lemoncheck/scenarios/`*`.scenario, `**`/`*`.scenario).
 *                     Paths are relative to the classpath root.
 *                     Default is ["lemoncheck/scenarios/\*.scenario"].
 * @property fragments Classpath locations to search for fragment files.
 *                     Supports glob patterns (e.g., lemoncheck/fragments/`*`.fragment).
 *                     Paths are relative to the classpath root.
 *                     Default is ["lemoncheck/fragments/\*.fragment"].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckScenarios(
    vararg val locations: String = ["lemoncheck/scenarios/*.scenario"],
    val fragments: Array<String> = ["lemoncheck/fragments/*.fragment"],
)

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
