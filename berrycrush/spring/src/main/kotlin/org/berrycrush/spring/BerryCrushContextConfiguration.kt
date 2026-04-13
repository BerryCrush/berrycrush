package org.berrycrush.spring

/**
 * Enables Spring TestContext integration for BerryCrush scenarios.
 *
 * When present on a test class alongside @SpringBootTest, the bindings
 * class is obtained from Spring's ApplicationContext instead of direct
 * instantiation, enabling dependency injection.
 *
 * Example usage:
 * ```java
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @BerryCrushContextConfiguration
 * @BerryCrushScenarios(locations = "scenarios/\*.scenario")
 * @BerryCrushConfiguration(bindings = MyBindings.class)
 * public class MyApiTest {}
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushContextConfiguration
