package io.github.ktakashi.lemoncheck.step

import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarFile

/**
 * Scans packages for classes containing @Step annotated methods.
 *
 * Uses classpath reflection to discover step definition classes in specified packages.
 */
class PackageStepScanner {
    private val annotationScanner = AnnotationStepScanner()

    /**
     * Scans a package for step definitions.
     *
     * @param packageName The package name to scan
     * @param classLoader The class loader to use (defaults to thread context class loader)
     * @return List of step definitions found in the package
     */
    fun scan(
        packageName: String,
        classLoader: ClassLoader? = null,
    ): List<StepDefinition> {
        val loader = classLoader ?: Thread.currentThread().contextClassLoader
        val classes = findClasses(packageName, loader)

        return classes
            .filter { clazz -> clazz.declaredMethods.any { it.isAnnotationPresent(Step::class.java) } }
            .flatMap { annotationScanner.scan(it) }
    }

    /**
     * Scans multiple packages for step definitions.
     *
     * @param packageNames The package names to scan
     * @param classLoader The class loader to use
     * @return List of all step definitions found
     */
    fun scanAll(
        vararg packageNames: String,
        classLoader: ClassLoader? = null,
    ): List<StepDefinition> = packageNames.flatMap { scan(it, classLoader) }

    private fun findClasses(
        packageName: String,
        classLoader: ClassLoader,
    ): List<Class<*>> {
        val path = packageName.replace('.', '/')
        val resources = classLoader.getResources(path)
        val classes = mutableListOf<Class<*>>()

        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            when (resource.protocol) {
                "file" -> {
                    val directory = File(resource.toURI())
                    classes.addAll(findClassesInDirectory(directory, packageName, classLoader))
                }
                "jar" -> {
                    val connection = resource.openConnection() as JarURLConnection
                    classes.addAll(findClassesInJar(connection.jarFile, packageName, classLoader))
                }
            }
        }

        return classes
    }

    private fun findClassesInDirectory(
        directory: File,
        packageName: String,
        classLoader: ClassLoader,
    ): List<Class<*>> {
        if (!directory.exists()) {
            return emptyList()
        }

        val classes = mutableListOf<Class<*>>()
        val files = directory.listFiles() ?: return emptyList()

        for (file in files) {
            if (file.isDirectory) {
                classes.addAll(
                    findClassesInDirectory(
                        file,
                        "$packageName.${file.name}",
                        classLoader,
                    ),
                )
            } else if (file.name.endsWith(".class")) {
                val className = "$packageName.${file.name.removeSuffix(".class")}"
                try {
                    classes.add(classLoader.loadClass(className))
                } catch (e: ClassNotFoundException) {
                    // Skip classes that can't be loaded
                } catch (e: NoClassDefFoundError) {
                    // Skip classes with missing dependencies
                }
            }
        }

        return classes
    }

    private fun findClassesInJar(
        jarFile: JarFile,
        packageName: String,
        classLoader: ClassLoader,
    ): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        val path = packageName.replace('.', '/')

        for (entry in jarFile.entries()) {
            if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
                val className =
                    entry.name
                        .removeSuffix(".class")
                        .replace('/', '.')
                try {
                    classes.add(classLoader.loadClass(className))
                } catch (e: ClassNotFoundException) {
                    // Skip classes that can't be loaded
                } catch (e: NoClassDefFoundError) {
                    // Skip classes with missing dependencies
                }
            }
        }

        return classes
    }
}
