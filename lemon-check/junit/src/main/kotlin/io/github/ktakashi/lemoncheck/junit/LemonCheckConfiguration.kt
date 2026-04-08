package io.github.ktakashi.lemoncheck.junit

import kotlin.reflect.KClass

/**
 * Annotation for configuring LemonCheck scenario execution.
 *
 * Apply this annotation to a test class to customize scenario execution behavior.
 * This includes specifying custom bindings, OpenAPI spec path overrides,
 * and per-scenario timeout settings.
 *
 * @property bindings Class implementing LemonCheckBindings to provide runtime values.
 *                    Must have a public no-arg constructor. Defaults to DefaultBindings.
 * @property openApiSpec Path to the OpenAPI specification file (classpath resource).
 *                       Overrides any spec from LemonCheckSpec if specified.
 * @property timeout Timeout per scenario execution in milliseconds.
 *                   Default is 30000 (30 seconds).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LemonCheckConfiguration(
    val bindings: KClass<out LemonCheckBindings> = DefaultBindings::class,
    val openApiSpec: String = "",
    val timeout: Long = 30_000L,
)
