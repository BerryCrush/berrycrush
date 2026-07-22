package org.berrycrush.junit

/**
 * Container annotation for multiple [BerryCrushSpec] annotations.
 *
 * This is automatically used when multiple `@BerryCrushSpec` annotations
 * are applied to a single class.
 *
 * @property value Array of BerryCrushSpec annotations
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushSpecs(
    vararg val value: BerryCrushSpec,
)

/**
 * Annotation to specify OpenAPI spec(s) for a BerryCrush test class.
 *
 * Multiple specs can be defined using repeatable annotations:
 *
 * ```kotlin
 * @BerryCrushSpec(paths = ["petstore.yaml"], name = "default")
 * @BerryCrushSpec(paths = ["auth.yaml"], name = "auth")
 * class MyTest
 * ```
 *
 * For simple cases with a single spec:
 * ```kotlin
 * @BerryCrushSpec("petstore.yaml")
 * class MyTest
 * ```
 *
 * The spec with `name = "default"` is treated as the primary spec.
 * If no spec is explicitly named "default", the first spec is used as default.
 *
 * @property paths Paths to OpenAPI specification files
 * @property baseUrl Base URL override for API requests
 * @property name Unique name for this spec. Default is "default" for the primary spec.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Repeatable
annotation class BerryCrushSpec(
    vararg val paths: String = [],
    val baseUrl: String = "",
    val name: String = BerryCrushBindings.DEFAULT_BINDING_NAME,
)

/**
 * Annotation to mark a test method or class as providing BerryCrush scenarios.
 *
 * When applied, the BerryCrushTestEngine will discover and execute scenario files
 * from the specified locations.
 *
 * @property locations Classpath locations to search for scenario files.
 *                     Supports glob patterns (e.g., berrycrush/scenarios/`*`.scenario, `**`/`*`.scenario).
 *                     Paths are relative to the classpath root.
 *                     Default is ["berrycrush/scenarios/\*.scenario"].
 * @property fragments Classpath locations to search for fragment files.
 *                     Supports glob patterns (e.g., berrycrush/fragments/`*`.fragment).
 *                     Paths are relative to the classpath root.
 *                     Default is ["berrycrush/fragments/\*.fragment"].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushScenarios(
    vararg val locations: String = ["berrycrush/scenarios/*.scenario"],
    val fragments: Array<String> = ["berrycrush/fragments/*.fragment"],
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
annotation class BerryCrushTags(
    val include: Array<String> = [],
    val exclude: Array<String> = [],
)

/**
 * Marks a method as a BerryCrush scenario test.
 *
 * Methods annotated with `@ScenarioTest` must return a [org.berrycrush.model.Scenario] object
 * that will be automatically executed by the BerryCrush test engine.
 *
 * ## Usage
 *
 * ```kotlin
 * @Suite
 * @BerryCrushSpec(paths = ["petstore.yaml"])
 * class PetstoreTest {
 *
 *     @ScenarioTest
 *     fun createPet(suite: BerryCrushSuite): Scenario =
 *         suite.scenario("Create a pet") {
 *             whenever("I create a pet") {
 *                 call("createPet") { body(mapOf("name" to "Fluffy")) }
 *             }
 *             afterwards("it is created") {
 *                 statusCode(201)
 *             }
 *         }
 * }
 * ```
 *
 * ## Spring Boot Integration
 *
 * ```kotlin
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @BerryCrushSpec(paths = ["petstore.yaml"])
 * @BerryCrushContextConfiguration
 * class PetstoreSpringTest {
 *
 *     @LocalServerPort
 *     var port: Int = 0
 *
 *     @BeforeEach
 *     fun setup(config: BerryCrushConfiguration) {
 *         config.baseUrl = "http://localhost:$port/api"
 *     }
 *
 *     @ScenarioTest
 *     fun createPet(suite: BerryCrushSuite): Scenario = ...
 * }
 * ```
 *
 * ## Method Requirements
 *
 * - Must return `Scenario` (from `BerryCrushSuite.scenario()`)
 * - Can accept `BerryCrushSuite` as parameter (injected by engine)
 * - Can be a member function of a class annotated with `@BerryCrushSpec`
 *
 * ## Lifecycle Support
 *
 * Scenario methods run with JUnit lifecycle semantics:
 *
 * - `@BeforeAll` / `@AfterAll` are executed at class scope
 * - `@BeforeEach` / `@AfterEach` are executed for each scenario method
 * - `@TestInstance(PER_CLASS)` reuses the same test instance across scenario methods
 *
 * @see BerryCrushSpec
 * @see org.berrycrush.junit.BerryCrushSuite
 * @see org.berrycrush.model.Scenario
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ScenarioTest
