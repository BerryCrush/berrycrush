package org.berrycrush.junit

import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.executor.ScenarioExecutor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(BerryCrushExtension::class)
@BerryCrushSpec
class BerryCrushExtensionTest {
    @Test
    fun `extension should inject BerryCrushSuite`(suite: BerryCrushSuite) {
        assertNotNull(suite)
    }

    @Test
    fun `extension should inject ScenarioExecutor`(executor: ScenarioExecutor) {
        assertNotNull(executor)
    }

    @Test
    fun `annotation should be present`() {
        val annotation = BerryCrushExtensionTest::class.java.getAnnotation(BerryCrushSpec::class.java)
        assertNotNull(annotation)
    }

    @Test
    fun `annotations should contain expected properties`() {
        val specAnnotation = TestClassWithSpec::class.java.getAnnotation(BerryCrushSpec::class.java)
        assertNotNull(specAnnotation)
        assertTrue(specAnnotation.paths.isNotEmpty())
    }
}

@BerryCrushSpec("test-spec.yaml", baseUrl = "http://localhost:8080")
class TestClassWithSpec

/**
 * Example test class extending ScenarioTest.
 */
class ExampleScenarioTest : ScenarioTest() {
    override fun defineScenarios() {
        // Define scenarios here
        // This is a placeholder - actual scenarios would be defined when spec is loaded
    }

    @Test
    fun `scenario test base class should work`() {
        val suite = getSuite()
        assertNotNull(suite)
    }
}
