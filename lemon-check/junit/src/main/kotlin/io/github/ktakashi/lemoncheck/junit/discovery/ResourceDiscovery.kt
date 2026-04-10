package io.github.ktakashi.lemoncheck.junit.discovery

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.jar.JarFile

/**
 * Marker interface for discovered classpath resources.
 */
interface Discovered {
    val path: String
    val name: String
    val url: URL
}

/**
 * Generic resource discovery for classpath files.
 *
 * Supports glob patterns for flexible file matching.
 * Discovers files from both file system and JAR resources.
 *
 * @param T The type of discovered resource
 * @property fileExtension The file extension to match (e.g., ".scenario", ".fragment")
 * @property resourceFactory Factory function to create discovered resource instances
 */
abstract class ResourceDiscovery<T : Discovered>(
    private val fileExtension: String,
    private val resourceFactory: (path: String, name: String, url: URL) -> T,
) {
    /**
     * Discovers files matching the given location patterns.
     *
     * @param classLoader The class loader to use for resource discovery
     * @param patterns Location patterns (glob syntax supported)
     * @return List of discovered resources
     */
    fun discover(
        classLoader: ClassLoader,
        patterns: Array<out String>,
    ): List<T> {
        val results = mutableListOf<T>()

        for (pattern in patterns) {
            results.addAll(discoverForPattern(classLoader, pattern))
        }

        return results.distinctBy { it.path }
    }

    private fun discoverForPattern(
        classLoader: ClassLoader,
        pattern: String,
    ): List<T> {
        val results = mutableListOf<T>()

        // Extract base directory from pattern (before any wildcards)
        val baseDir = extractBaseDirectory(pattern)
        val globPattern = buildGlobPattern(pattern)

        // Get resources from the base directory
        val resources = classLoader.getResources(baseDir)

        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            when (resource.protocol) {
                "file" -> results.addAll(discoverFromFileSystem(resource, baseDir, globPattern))
                "jar" -> results.addAll(discoverFromJar(resource, baseDir, globPattern))
            }
        }

        // Also try direct resource lookup for exact paths
        if (!pattern.contains("*")) {
            classLoader.getResource(pattern)?.let { url ->
                if (pattern.endsWith(fileExtension)) {
                    val name = pattern.substringAfterLast("/")
                    results.add(resourceFactory(pattern, name, url))
                }
            }
        }

        return results
    }

    private fun buildGlobPattern(pattern: String): String {
        if (pattern.contains("*")) {
            return pattern
        }
        // Build pattern like: "basedir/**//*.extension"
        // Using raw strings to avoid comment lexer issues
        val starStar = "*".repeat(2)
        return "$pattern/$starStar/*$fileExtension"
    }

    private fun extractBaseDirectory(pattern: String): String {
        val parts = pattern.split("/", "\\")
        val baseParts = mutableListOf<String>()

        for (part in parts) {
            if (part.contains("*") || part.contains("?")) {
                break
            }
            baseParts.add(part)
        }

        return baseParts.joinToString("/").ifEmpty { "" }
    }

    private fun discoverFromFileSystem(
        resource: URL,
        baseDir: String,
        globPattern: String,
    ): List<T> {
        val results = mutableListOf<T>()
        val basePath = File(resource.toURI())

        if (!basePath.exists() || !basePath.isDirectory) {
            return results
        }

        // Create a PathMatcher for the glob pattern
        val matcher = createPathMatcher(globPattern)

        basePath
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(fileExtension) }
            .forEach { file ->
                val relativePath = "$baseDir/${file.relativeTo(basePath).path}".replace("\\", "/")
                if (matcher.matches(Path.of(relativePath))) {
                    results.add(
                        resourceFactory(
                            relativePath,
                            file.name,
                            file.toURI().toURL(),
                        ),
                    )
                }
            }

        return results
    }

    private fun discoverFromJar(
        resource: URL,
        baseDir: String,
        globPattern: String,
    ): List<T> {
        val results = mutableListOf<T>()

        // Extract JAR path from URL (format: jar:file:/path/to/jar.jar!/path/in/jar)
        val jarPath = resource.path.substringAfter("file:").substringBefore("!")
        val jarFile = JarFile(jarPath)
        val matcher = createPathMatcher(globPattern)

        jarFile.use { jar ->
            jar
                .entries()
                .asSequence()
                .filter { !it.isDirectory && it.name.endsWith(fileExtension) }
                .filter { it.name.startsWith(baseDir) }
                .forEach { entry ->
                    if (matcher.matches(Path.of(entry.name))) {
                        val url = URI.create("jar:file:$jarPath!/${entry.name}").toURL()
                        results.add(
                            resourceFactory(
                                entry.name,
                                entry.name.substringAfterLast("/"),
                                url,
                            ),
                        )
                    }
                }
        }

        return results
    }

    private fun createPathMatcher(pattern: String): PathMatcher {
        val globPattern = "glob:$pattern"
        return FileSystems.getDefault().getPathMatcher(globPattern)
    }
}
