package org.berrycrush.scanner

import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * Scans classes for methods annotated.
 *
 * Extracts step definitions from annotated methods in provided classes.
 */
interface AnnotationScanner<T> {
    /**
     * Scans a class for annotated methods.
     *
     * @param clazz The class to scan
     * @param instance Optional instance for non-static methods (created if null)
     * @return List of [T] found in the class
     */
    fun scan(
        clazz: Class<*>,
        instance: Any? = null,
    ): List<T>

    /**
     * Scans a class for annotated methods.
     *
     * @param clazz The class to scan
     * @return List of [T] found in the class
     */
    fun scan(clazz: Class<*>): List<T> = scan(clazz, null)

    /**
     * Scans multiple classes for annotated methods.
     *
     * @param classes The classes to scan
     * @return List of all [T] found
     */
    fun scanAll(vararg classes: Class<*>): List<T>

    /**
     * Scans multiple classes with their instances for annotated methods.
     *
     * @param instances The class instances to scan
     * @return List of all [T] found
     */
    fun scanInstances(vararg instances: Any): List<T>
}

internal fun <T : Annotation, R> Class<*>.scanMethodAnnotations(
    clazz: KClass<T>,
    block: (Method, T) -> R,
): List<R> =
    kotlin.memberFunctions
        .filter { it.javaMethod != null }
        .flatMap { method ->
            if (method.visibility != KVisibility.PUBLIC) {
                method.isAccessible = true
            }
            method.forAllAnnotation(clazz) {
                block(method.javaMethod!!, it)
            }
        }

internal fun <T : Annotation, R> KFunction<*>.forAllAnnotation(
    clazz: KClass<T>,
    proc: (T) -> R,
): List<R> =
    this.findAnnotations(clazz).mapNotNull { annotation ->
        proc(annotation)
    }

/**
 * Scans packages for classes containing [annotationClass] annotated methods.
 *
 * Uses classpath reflection to discover [T] classes in specified packages.
 */
abstract class PackageScanner<T>(
    private val annotationScanner: AnnotationScanner<T>,
    private val annotationClass: Class<out Annotation>,
) {
    /**
     * Scans a package for [T].
     *
     * @param packageName The package name to scan
     * @param classLoader The class loader to use (defaults to thread context class loader)
     * @return List of [T]s found in the package
     */
    fun scan(
        packageName: String,
        classLoader: ClassLoader? = null,
    ): List<T> {
        val loader = classLoader ?: Thread.currentThread().contextClassLoader
        val classes = findClasses(packageName, loader)

        return classes
            .filter { clazz -> clazz.declaredMethods.any { it.isAnnotationPresent(annotationClass) } }
            .flatMap { annotationScanner.scan(it) }
    }

    /**
     * Scans multiple packages for [T].
     *
     * @param packageNames The package names to scan
     * @param classLoader The class loader to use
     * @return List of all [T]s found
     */
    fun scanAll(
        vararg packageNames: String,
        classLoader: ClassLoader? = null,
    ): List<T> = packageNames.flatMap { scan(it, classLoader) }
}

internal fun createInstance(clazz: Class<*>): Any? =
    runCatching {
        val constructor = clazz.getDeclaredConstructor()
        if (!Modifier.isPublic(constructor.modifiers)) {
            constructor.isAccessible = true
        }
        constructor.newInstance()
    }.getOrNull()

internal fun findClasses(
    packageName: String,
    classLoader: ClassLoader,
): List<Class<*>> {
    val path = packageName.replace('.', '/')
    val resources = classLoader.getResources(path)

    return generateSequence { resources.takeIf { it.hasMoreElements() }?.nextElement() }
        .flatMap { resource ->
            when (resource.protocol) {
                "file" -> {
                    val directory = File(resource.toURI())
                    findClassesInDirectory(directory, packageName, classLoader).asSequence()
                }

                "jar" -> {
                    val connection = resource.openConnection() as JarURLConnection
                    findClassesInJar(connection.jarFile, packageName, classLoader).asSequence()
                }

                else -> {
                    emptySequence()
                }
            }
        }.toList()
}

private const val CLASS_FILE_EXTENSION = ".class"

private fun findClassesInDirectory(
    directory: File,
    packageName: String,
    classLoader: ClassLoader,
): List<Class<*>> {
    if (!directory.exists()) return emptyList()

    val files = directory.listFiles() ?: return emptyList()

    return files.flatMap { file ->
        when {
            file.isDirectory -> {
                findClassesInDirectory(file, "$packageName.${file.name}", classLoader)
            }

            file.name.endsWith(CLASS_FILE_EXTENSION) -> {
                val className = "$packageName.${file.name.removeSuffix(CLASS_FILE_EXTENSION)}"
                listOfNotNull(
                    runCatching { classLoader.loadClass(className) }.getOrNull(),
                )
            }

            else -> {
                emptyList()
            }
        }
    }
}

private fun findClassesInJar(
    jarFile: JarFile,
    packageName: String,
    classLoader: ClassLoader,
): List<Class<*>> {
    val path = packageName.replace('.', '/')

    return jarFile
        .entries()
        .asSequence()
        .filter { it.name.startsWith(path) && it.name.endsWith(CLASS_FILE_EXTENSION) }
        .mapNotNull { entry ->
            val className = entry.name.removeSuffix(CLASS_FILE_EXTENSION).replace('/', '.')
            runCatching { classLoader.loadClass(className) }.getOrNull()
        }.toList()
}
