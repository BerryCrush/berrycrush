package io.github.ktakashi.lemoncheck.junit.discovery

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.jar.JarFile

/**
 * Discovers .fragment files from classpath locations.
 *
 * Supports glob patterns for flexible file matching (e.g., fragments/\*.fragment).
 */
object FragmentDiscovery {
    /**
     * Discovers fragment files matching the given location patterns.
     *
     * @param classLoader The class loader to use for resource discovery
     * @param patterns Location patterns (glob syntax supported)
     * @return List of URLs pointing to discovered fragment files
     */
    fun discoverFragments(
        classLoader: ClassLoader,
        patterns: Array<out String>,
    ): List<DiscoveredFragment> {
        val fragments = mutableListOf<DiscoveredFragment>()

        for (pattern in patterns) {
            fragments.addAll(discoverForPattern(classLoader, pattern))
        }

        return fragments.distinctBy { it.path }
    }

    private fun discoverForPattern(
        classLoader: ClassLoader,
        pattern: String,
    ): List<DiscoveredFragment> {
        val fragments = mutableListOf<DiscoveredFragment>()

        // Extract base directory from pattern (before any wildcards)
        val baseDir = extractBaseDirectory(pattern)
        val globPattern = if (pattern.contains("*")) pattern else "$pattern/**/*.fragment"

        // Get resources from the base directory
        val resources = classLoader.getResources(baseDir)

        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            when (resource.protocol) {
                "file" -> fragments.addAll(discoverFromFileSystem(resource, baseDir, globPattern))
                "jar" -> fragments.addAll(discoverFromJar(resource, baseDir, globPattern))
            }
        }

        // Also try direct resource lookup for exact paths
        if (!pattern.contains("*")) {
            classLoader.getResource(pattern)?.let { url ->
                if (pattern.endsWith(".fragment")) {
                    val name = pattern.substringAfterLast("/")
                    fragments.add(DiscoveredFragment(pattern, name, url))
                }
            }
        }

        return fragments
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
    ): List<DiscoveredFragment> {
        val fragments = mutableListOf<DiscoveredFragment>()
        val basePath = File(resource.toURI())

        if (!basePath.exists() || !basePath.isDirectory) {
            return fragments
        }

        // Create a PathMatcher for the glob pattern
        val matcher = createPathMatcher(globPattern)

        basePath
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".fragment") }
            .forEach { file ->
                val relativePath = "$baseDir/${file.relativeTo(basePath).path}".replace("\\", "/")
                if (matcher.matches(Path.of(relativePath))) {
                    fragments.add(
                        DiscoveredFragment(
                            path = relativePath,
                            name = file.name,
                            url = file.toURI().toURL(),
                        ),
                    )
                }
            }

        return fragments
    }

    private fun discoverFromJar(
        resource: URL,
        baseDir: String,
        globPattern: String,
    ): List<DiscoveredFragment> {
        val fragments = mutableListOf<DiscoveredFragment>()

        // Extract JAR path from URL (format: jar:file:/path/to/jar.jar!/path/in/jar)
        val jarPath = resource.path.substringAfter("file:").substringBefore("!")
        val jarFile = JarFile(jarPath)
        val matcher = createPathMatcher(globPattern)

        jarFile.use { jar ->
            jar
                .entries()
                .asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".fragment") }
                .filter { it.name.startsWith(baseDir) }
                .forEach { entry ->
                    if (matcher.matches(Path.of(entry.name))) {
                        val url = URI.create("jar:file:$jarPath!/${entry.name}").toURL()
                        fragments.add(
                            DiscoveredFragment(
                                path = entry.name,
                                name = entry.name.substringAfterLast("/"),
                                url = url,
                            ),
                        )
                    }
                }
        }

        return fragments
    }

    private fun createPathMatcher(pattern: String): PathMatcher {
        val globPattern = "glob:$pattern"
        return FileSystems.getDefault().getPathMatcher(globPattern)
    }
}

/**
 * Represents a discovered fragment file.
 *
 * @property path The classpath path to the fragment file
 * @property name The filename (without path)
 * @property url The URL to access the fragment file
 */
data class DiscoveredFragment(
    val path: String,
    val name: String,
    val url: URL,
)
