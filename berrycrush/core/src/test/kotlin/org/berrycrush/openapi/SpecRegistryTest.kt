package org.berrycrush.openapi

import org.berrycrush.exception.ConfigurationException
import org.berrycrush.exception.OperationNotFoundException
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpecRegistryTest {
    @Test
    fun `registerDefault should register and expose default spec`() {
        val registry = SpecRegistry()

        registry.registerDefault(petstoreSpecPath())

        assertTrue(registry.hasSpecs())
        assertEquals(setOf("default"), registry.specNames())
        assertEquals("default", registry.getDefault().name)
    }

    @Test
    fun `register should apply config overrides`() {
        val registry = SpecRegistry()

        registry.register("petstore", petstoreSpecPath()) {
            baseUrl = "https://example.test"
            header("Authorization", "Bearer test-token")
        }

        val loaded = registry.get("petstore")
        assertEquals("https://example.test", loaded.baseUrl)
        assertEquals("Bearer test-token", loaded.defaultHeaders["Authorization"])
    }

    @Test
    fun `get should throw for unknown spec`() {
        val registry = SpecRegistry()

        val error =
            assertFailsWith<ConfigurationException> {
                registry.get("missing")
            }

        assertContains(error.message ?: "", "Spec 'missing' not found")
    }

    @Test
    fun `resolve should return operation from selected spec`() {
        val registry = SpecRegistry()
        registry.registerDefault(petstoreSpecPath())

        val (loaded, resolved) = registry.resolve("listPets", "default")

        assertEquals("default", loaded.name)
        assertEquals("listPets", resolved.operationId)
        assertEquals("/pets", resolved.path)
        assertEquals(HttpMethod.GET, resolved.method)
    }

    @Test
    fun `resolve should throw not found with operation id`() {
        val registry = SpecRegistry()
        registry.registerDefault(petstoreSpecPath())

        val error =
            assertFailsWith<OperationNotFoundException> {
                registry.resolve("doesNotExist")
            }

        assertEquals("doesNotExist", error.operationId)
        assertTrue(error.availableOperations.isNotEmpty())
    }

    @Test
    fun `resolve should throw when operation is ambiguous across specs`() {
        val registry = SpecRegistry()

        val firstSpec = createTempSpec("first", "/v1/ping")
        val secondSpec = createTempSpec("second", "/v2/ping")

        registry.register("first", firstSpec)
        registry.register("second", secondSpec)

        val error =
            assertFailsWith<AmbiguousOperationException> {
                registry.resolve("sharedOperation")
            }

        assertContains(error.message ?: "", "sharedOperation")
        assertContains(error.message ?: "", "first")
        assertContains(error.message ?: "", "second")
    }

    @Test
    fun `updateBaseUrl should replace existing spec base url`() {
        val registry = SpecRegistry()
        registry.register("petstore", petstoreSpecPath())

        registry.updateBaseUrl("petstore", "https://override.example")

        assertEquals("https://override.example", registry.get("petstore").baseUrl)
    }

    @Test
    fun `updateBaseUrl should throw when spec is missing`() {
        val registry = SpecRegistry()

        val error =
            assertFailsWith<IllegalArgumentException> {
                registry.updateBaseUrl("missing", "https://example.test")
            }

        assertContains(error.message ?: "", "Spec 'missing' not found")
    }

    private fun petstoreSpecPath(): String =
        javaClass.getResource("/petstore.yaml")?.path
            ?: error("petstore.yaml not found")

    private fun createTempSpec(
        title: String,
        path: String,
    ): String {
        val specFile = createTempFile(prefix = "berrycrush-$title-", suffix = ".yaml")
        specFile.writeText(
            """
            openapi: 3.0.3
            info:
              title: $title
              version: 1.0.0
            servers:
              - url: https://api.example.test
            paths:
              $path:
                get:
                  operationId: sharedOperation
                  responses:
                    '200':
                      description: OK
            """.trimIndent(),
        )
        val file = specFile.toFile()
        file.deleteOnExit()
        return file.absolutePath
    }
}
