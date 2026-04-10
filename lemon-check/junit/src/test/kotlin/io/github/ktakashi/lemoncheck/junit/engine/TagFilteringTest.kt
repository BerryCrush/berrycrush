package io.github.ktakashi.lemoncheck.junit.engine

import io.github.ktakashi.lemoncheck.junit.LemonCheckTags
import org.junit.jupiter.api.Test
import org.junit.platform.engine.UniqueId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for tag filtering functionality in ClassTestDescriptor.
 *
 * This validates the @LemonCheckTags annotation handling and the
 * shouldExecuteScenario() method which determines whether a scenario
 * should be run based on its tags.
 */
class TagFilteringTest {
    @Test
    fun `ClassTestDescriptor reads include tags from annotation`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", IncludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, IncludeOnlyTagsStub::class.java)

        assertEquals(setOf("smoke", "api"), descriptor.includeTags)
        assertEquals(emptySet(), descriptor.excludeTags)
    }

    @Test
    fun `ClassTestDescriptor reads exclude tags from annotation`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", ExcludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, ExcludeOnlyTagsStub::class.java)

        assertEquals(emptySet(), descriptor.includeTags)
        assertEquals(setOf("ignore", "wip"), descriptor.excludeTags)
    }

    @Test
    fun `ClassTestDescriptor reads both include and exclude tags`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", CombinedTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, CombinedTagsStub::class.java)

        assertEquals(setOf("api"), descriptor.includeTags)
        assertEquals(setOf("ignore"), descriptor.excludeTags)
    }

    @Test
    fun `shouldExecuteScenario returns true when no tags configured`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", NoTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, NoTagsStub::class.java)

        // No tags configured - all scenarios should run
        assertTrue(descriptor.shouldExecuteScenario(emptySet()))
        assertTrue(descriptor.shouldExecuteScenario(setOf("any", "tag")))
    }

    // --- Include-only filtering tests ---

    @Test
    fun `shouldExecuteScenario with include filter accepts matching scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", IncludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, IncludeOnlyTagsStub::class.java)

        // Scenario has @smoke tag which is in include set
        assertTrue(descriptor.shouldExecuteScenario(setOf("smoke")))
        // Scenario has @api tag which is in include set
        assertTrue(descriptor.shouldExecuteScenario(setOf("api")))
        // Scenario has both tags
        assertTrue(descriptor.shouldExecuteScenario(setOf("smoke", "api")))
    }

    @Test
    fun `shouldExecuteScenario with include filter rejects non-matching scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", IncludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, IncludeOnlyTagsStub::class.java)

        // Scenario has no tags - doesn't match include filter
        assertFalse(descriptor.shouldExecuteScenario(emptySet()))
        // Scenario has tags but not in include set
        assertFalse(descriptor.shouldExecuteScenario(setOf("regression")))
        assertFalse(descriptor.shouldExecuteScenario(setOf("other", "tags")))
    }

    // --- Exclude-only filtering tests ---

    @Test
    fun `shouldExecuteScenario with exclude filter rejects matching scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", ExcludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, ExcludeOnlyTagsStub::class.java)

        // Scenario has @ignore tag which is in exclude set
        assertFalse(descriptor.shouldExecuteScenario(setOf("ignore")))
        // Scenario has @wip tag which is in exclude set
        assertFalse(descriptor.shouldExecuteScenario(setOf("wip")))
        // Scenario has both excluded tags
        assertFalse(descriptor.shouldExecuteScenario(setOf("ignore", "wip")))
        // Scenario has @ignore plus other tag
        assertFalse(descriptor.shouldExecuteScenario(setOf("api", "ignore")))
    }

    @Test
    fun `shouldExecuteScenario with exclude filter accepts non-matching scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", ExcludeOnlyTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, ExcludeOnlyTagsStub::class.java)

        // Scenario has no tags - OK
        assertTrue(descriptor.shouldExecuteScenario(emptySet()))
        // Scenario has tags not in exclude set - OK
        assertTrue(descriptor.shouldExecuteScenario(setOf("api")))
        assertTrue(descriptor.shouldExecuteScenario(setOf("smoke", "regression")))
    }

    // --- Combined include + exclude filtering tests ---

    @Test
    fun `shouldExecuteScenario with combined filters - exclude takes precedence`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", CombinedTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, CombinedTagsStub::class.java)

        // CRITICAL TEST: Scenario has BOTH @api (in include) AND @ignore (in exclude)
        // The @ignore should cause exclusion even though @api matches include
        assertFalse(
            descriptor.shouldExecuteScenario(setOf("api", "ignore")),
            "Exclude should take precedence over include",
        )
    }

    @Test
    fun `shouldExecuteScenario with combined filters accepts api-only scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", CombinedTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, CombinedTagsStub::class.java)

        // Scenario has @api but not @ignore - should be included
        assertTrue(descriptor.shouldExecuteScenario(setOf("api")))
        // Scenario has @api and other non-excluded tags - should be included
        assertTrue(descriptor.shouldExecuteScenario(setOf("api", "smoke", "feature")))
    }

    @Test
    fun `shouldExecuteScenario with combined filters rejects ignore-only scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", CombinedTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, CombinedTagsStub::class.java)

        // Scenario has @ignore but not @api - excluded by both filters
        assertFalse(descriptor.shouldExecuteScenario(setOf("ignore")))
    }

    @Test
    fun `shouldExecuteScenario with combined filters rejects non-api scenario`() {
        val uniqueId = UniqueId.forEngine("lemoncheck").append("class", CombinedTagsStub::class.java.name)
        val descriptor = ClassTestDescriptor(uniqueId, CombinedTagsStub::class.java)

        // Scenario has no @api - excluded by include filter
        assertFalse(descriptor.shouldExecuteScenario(emptySet()))
        assertFalse(descriptor.shouldExecuteScenario(setOf("smoke")))
        assertFalse(descriptor.shouldExecuteScenario(setOf("regression", "smoke")))
    }
}

// --- Stub test classes for annotation parsing ---
// These are NOT actual test classes - they exist only for testing annotation parsing.
// They do NOT have @LemonCheckScenarios to prevent the lemoncheck engine from discovering them.

private class NoTagsStub

@LemonCheckTags(include = ["smoke", "api"])
private class IncludeOnlyTagsStub

@LemonCheckTags(exclude = ["ignore", "wip"])
private class ExcludeOnlyTagsStub

@LemonCheckTags(include = ["api"], exclude = ["ignore"])
private class CombinedTagsStub
