package org.berrycrush.assertion

import org.berrycrush.scanner.AnnotationScanner
import org.berrycrush.scanner.createInstance
import java.lang.reflect.Modifier

/**
 * Scans classes for methods annotated with [@Assertion].
 *
 * Extracts assertion definitions from annotated methods in provided classes.
 */
class AnnotationAssertionScanner : AnnotationScanner<AssertionDefinition> {
    /**
     * Scans a class for @Assertion annotated methods.
     *
     * @param clazz The class to scan
     * @param instance Optional instance for non-static methods (created if null)
     * @return List of assertion definitions found in the class
     */
    override fun scan(
        clazz: Class<*>,
        instance: Any?,
    ): List<AssertionDefinition> {
        val actualInstance = instance ?: createInstance(clazz)

        return clazz.declaredMethods
            .mapNotNull { method ->
                method.getAnnotation(Assertion::class.java)?.let { annotation ->
                    if (!Modifier.isPublic(method.modifiers)) {
                        method.isAccessible = true
                    }
                    AssertionDefinition(
                        pattern = annotation.pattern,
                        method = method,
                        instance = if (Modifier.isStatic(method.modifiers)) null else actualInstance,
                        description = annotation.description,
                    )
                }
            }
    }

    /**
     * Scans multiple classes for @Assertion annotated methods.
     *
     * @param classes The classes to scan
     * @return List of all assertion definitions found
     */
    override fun scanAll(vararg classes: Class<*>): List<AssertionDefinition> = classes.flatMap { scan(it) }

    /**
     * Scans multiple classes with their instances for @Assertion annotated methods.
     *
     * @param instances The class instances to scan
     * @return List of all assertion definitions found
     */
    override fun scanInstances(vararg instances: Any): List<AssertionDefinition> = instances.flatMap { scan(it.javaClass, it) }
}
