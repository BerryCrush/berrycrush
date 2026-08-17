package org.berrycrush.spring

import org.berrycrush.assertion.AnnotationAssertionScanner
import org.berrycrush.assertion.Assertion
import org.berrycrush.assertion.AssertionDefinition
import org.berrycrush.assertion.AssertionRegistry
import org.berrycrush.assertion.DefaultAssertionRegistry
import org.berrycrush.scanner.AnnotationScanner
import org.berrycrush.step.AnnotationStepScanner
import org.berrycrush.step.DefaultStepRegistry
import org.berrycrush.step.Step
import org.berrycrush.util.StepDefinition
import org.berrycrush.util.StepRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring auto-discovery support for @Step annotated methods.
 *
 * Automatically discovers and registers step definitions from Spring-managed beans
 * annotated with @Component (or its derivatives) that have methods with @Step.
 *
 * To enable auto-discovery, include this configuration class in your Spring context:
 *
 * ```kotlin
 * @SpringBootTest
 * @Import(SpringStepDiscovery::class)
 * class MyApiTest {
 *     @Autowired
 *     lateinit var stepRegistry: StepRegistry
 * }
 * ```
 *
 * Or annotate your step definitions with @Component:
 *
 * ```kotlin
 * @Component
 * class MySteps {
 *     @Step("I have {int} pets")
 *     fun setPetCount(count: Int) {
 *         // Step implementation
 *     }
 * }
 * ```
 */
@Configuration
class SpringStepDiscovery {
    private val annotationStepScanner = AnnotationStepScanner()
    private val annotationAssertionScanner = AnnotationAssertionScanner()

    /**
     * Creates a StepRegistry bean populated with all step definitions
     * discovered from Spring-managed beans.
     *
     * @param context The Spring ApplicationContext
     * @return A StepRegistry containing all discovered step definitions
     */
    @Bean
    fun stepRegistry(context: ApplicationContext): StepRegistry {
        val registry = DefaultStepRegistry()
        val definitions = discoverSteps(context)
        registry.registerAll(definitions)
        return registry
    }

    @Bean
    fun assertionRegistry(context: ApplicationContext): AssertionRegistry {
        val registry = DefaultAssertionRegistry()
        val definitions = discoverAssertions(context)
        registry.registerAll(definitions)
        return registry
    }

    /**
     * Discovers all step definitions from Spring-managed beans.
     *
     * Scans all beans in the application context for methods annotated with @Step
     * and creates StepDefinition instances using the Spring-managed bean instances.
     *
     * @param context The Spring ApplicationContext
     * @return List of discovered StepDefinitions
     */
    private fun discoverSteps(context: ApplicationContext): List<StepDefinition> =
        discover(context, Step::class.java, annotationStepScanner)

    private fun discoverAssertions(context: ApplicationContext): List<AssertionDefinition> =
        discover(context, Assertion::class.java, annotationAssertionScanner)

    private inline fun <reified T, A : Annotation> discover(
        context: ApplicationContext,
        annotationClass: Class<A>,
        scanner: AnnotationScanner<T>,
    ): List<T> =
        context.beanDefinitionNames
            .mapNotNull { beanName ->
                runCatching {
                    val bean = context.getBean(beanName)
                    val beanClass = bean.javaClass
                    val hasAnnotatedMethods = beanClass.declaredMethods.any { it.isAnnotationPresent(annotationClass) }
                    if (hasAnnotatedMethods) scanner.scan(beanClass, bean) else null
                }.getOrNull()
            }.flatten()
}
