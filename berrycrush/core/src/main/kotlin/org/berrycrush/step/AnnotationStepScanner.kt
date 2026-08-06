package org.berrycrush.step

import org.berrycrush.scanner.AnnotationScanner
import org.berrycrush.scanner.createInstance
import org.berrycrush.scanner.forAllAnnotation
import org.berrycrush.scanner.scanMethodAnnotations
import org.berrycrush.util.StepDefinition
import java.lang.reflect.Modifier
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * Scans classes for methods annotated with [@Step].
 *
 * Extracts step definitions from annotated methods in provided classes.
 */
class AnnotationStepScanner : AnnotationScanner<StepDefinition> {
    /**
     * Scans a class for @Step annotated methods.
     *
     * @param clazz The class to scan
     * @param instance Optional instance for non-static methods (created if null)
     * @return List of step definitions found in the class
     */
    override fun scan(
        clazz: Class<*>,
        instance: Any?,
    ): List<StepDefinition> {
        val actualInstance = instance ?: createInstance(clazz)

        return clazz.scanMethodAnnotations(Step::class) { method, annotation ->
            val isStatic = Modifier.isStatic(method.modifiers)
            StepDefinition(
                pattern = annotation.pattern,
                method = method,
                instance = if (isStatic) null else actualInstance,
                description = annotation.description,
            )
        }
    }

    /**
     * Scans multiple classes for @Step annotated methods.
     *
     * @param classes The classes to scan
     * @return List of all step definitions found
     */
    override fun scanAll(vararg classes: Class<*>): List<StepDefinition> = classes.flatMap { scan(it) }

    /**
     * Scans multiple classes with their instances for @Step annotated methods.
     *
     * @param instances The class instances to scan
     * @return List of all step definitions found
     */
    override fun scanInstances(vararg instances: Any): List<StepDefinition> = instances.flatMap { scan(it.javaClass, it) }
}
