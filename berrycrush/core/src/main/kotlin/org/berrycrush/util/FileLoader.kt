package org.berrycrush.util

import org.berrycrush.exception.ConfigurationException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Utility for loading external files from classpath or file system.
 *
 * Commonly used for:
 * - Request body templates
 * - Expected response fixtures
 * - Configuration files
 *
 * Supported path formats:
 * - `classpath:path/to/file.json` - Load from classpath
 * - `file:./relative/path.json` - Load from file system (relative to working directory)
 * - `file:/absolute/path.json` - Load from file system (absolute path)
 * - `/absolute/path.json` - Load from file system (absolute path, shorthand)
 * - `./relative/path.json` - Load from file system (relative path, shorthand)
 */
object FileLoader {
    /**
     * Load content from the specified URI.
     */
    fun load(path: URI): String =
        when (path.scheme) {
            "jar" -> {
                FileSystems.newFileSystem(path, emptyMap<String, Any>()).use { fs ->
                    // format jar:file:/.../foo.jar!/path/to/file.scenario
                    val jarPath = fs.getPath(path.schemeSpecificPart.takeLastWhile { c -> c != '!' })
                    load(jarPath)
                }
            }

            else -> {
                load(Paths.get(path))
            }
        }

    /**
     * Load content from the specified Path.
     */
    fun load(path: Path): String = Files.readString(path)

    /**
     * Load content from the specified path.
     *
     * @param path The file path with optional prefix (classpath: or file:)
     * @param baseDirectory Optional base directory for resolving relative paths
     * @return The file content as a string
     * @throws ConfigurationException if the file cannot be loaded
     */
    fun load(
        path: String,
        baseDirectory: Path? = null,
    ): String =
        runCatching {
            val uri = URI.create(path)
            when (uri.scheme) {
                "classpath" -> {
                    loadFromClasspath(uri.schemeSpecificPart)
                }

                "file" -> {
                    load(resolveBaseDir(Paths.get(uri), baseDirectory))
                }

                "jar" -> {
                    load(uri)
                }

                else -> {
                    if (path.startsWith("/") || path.startsWith("./") || path.startsWith("../")) {
                        load(resolveBaseDir(Paths.get(path), baseDirectory))
                    } else {
                        loadFromClasspath(path) // Default to classpath
                    }
                }
            }
        }.getOrElse { e ->
            throw ConfigurationException("Failed to load file '$path': ${e.message}", e)
        }

    private fun resolveBaseDir(
        basePath: Path,
        baseDirectory: Path?,
    ): Path =
        when {
            basePath.isAbsolute -> basePath
            baseDirectory != null -> baseDirectory.resolve(basePath).normalize()
            else -> basePath.toAbsolutePath().normalize()
        }

    /**
     * Load content from classpath.
     */
    private fun loadFromClasspath(path: String): String {
        val normalizedPath = path.trimStart('/')
        val inputStream =
            Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath)
                ?: FileLoader::class.java.classLoader?.getResourceAsStream(normalizedPath)
                ?: throw ConfigurationException("Classpath resource not found: $normalizedPath")

        return inputStream.bufferedReader().use { it.readText() }
    }
}
