package io.github.ktakashi.lemoncheck.openapi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiLoaderTest {
    private val loader = OpenApiLoader()

    @Test
    fun `should load petstore spec from resources`() {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found in test resources")

        val openApi = loader.load(specPath)

        assertNotNull(openApi)
        assertEquals("Petstore API", openApi.info.title)
        assertEquals("1.0.0", openApi.info.version)
    }

    @Test
    fun `should resolve operations from loaded spec`() {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found in test resources")

        val openApi = loader.load(specPath)

        assertNotNull(openApi.paths)
        assertTrue(openApi.paths.isNotEmpty())
        assertNotNull(openApi.paths["/pets"])
        assertNotNull(openApi.paths["/pets/{petId}"])
    }

    @Test
    fun `should extract server URLs from spec`() {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found in test resources")

        val openApi = loader.load(specPath)

        assertNotNull(openApi.servers)
        assertTrue(openApi.servers.isNotEmpty())
        assertEquals("https://petstore.example.com/api/v1", openApi.servers[0].url)
    }

    @Test
    fun `should parse component schemas`() {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found in test resources")

        val openApi = loader.load(specPath)

        assertNotNull(openApi.components?.schemas)
        assertTrue("Pet" in openApi.components.schemas)
        assertTrue("NewPet" in openApi.components.schemas)
        assertTrue("Error" in openApi.components.schemas)
    }
}
