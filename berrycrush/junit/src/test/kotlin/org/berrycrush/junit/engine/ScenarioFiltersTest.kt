package org.berrycrush.junit.engine

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ScenarioFilters].
 */
@DisplayName("ScenarioFilters")
class ScenarioFiltersTest {
    @AfterEach
    fun clearSystemProperties() {
        System.clearProperty(ScenarioFilters.PROPERTY_SCENARIO_FILE)
        System.clearProperty(ScenarioFilters.PROPERTY_SCENARIO_NAME)
        System.clearProperty(ScenarioFilters.PROPERTY_FEATURE_NAME)
    }

    @Nested
    @DisplayName("fromSystemProperties")
    inner class FromSystemProperties {
        @Test
        fun `returns empty filters when no properties set`() {
            val filters = ScenarioFilters.fromSystemProperties()

            assertNull(filters.scenarioFile)
            assertNull(filters.scenarioName)
            assertNull(filters.featureName)
            assertFalse(filters.hasFilters)
        }

        @Test
        fun `reads scenarioFile property`() {
            System.setProperty(ScenarioFilters.PROPERTY_SCENARIO_FILE, "login.scenario")

            val filters = ScenarioFilters.fromSystemProperties()

            assertEquals("login.scenario", filters.scenarioFile)
            assertTrue(filters.hasFilters)
        }

        @Test
        fun `reads scenarioName property`() {
            System.setProperty(ScenarioFilters.PROPERTY_SCENARIO_NAME, "User Login")

            val filters = ScenarioFilters.fromSystemProperties()

            assertEquals("User Login", filters.scenarioName)
            assertTrue(filters.hasFilters)
        }

        @Test
        fun `reads featureName property`() {
            System.setProperty(ScenarioFilters.PROPERTY_FEATURE_NAME, "Authentication")

            val filters = ScenarioFilters.fromSystemProperties()

            assertEquals("Authentication", filters.featureName)
            assertTrue(filters.hasFilters)
        }

        @Test
        fun `reads all properties`() {
            System.setProperty(ScenarioFilters.PROPERTY_SCENARIO_FILE, "auth.scenario")
            System.setProperty(ScenarioFilters.PROPERTY_SCENARIO_NAME, "Login")
            System.setProperty(ScenarioFilters.PROPERTY_FEATURE_NAME, "Auth")

            val filters = ScenarioFilters.fromSystemProperties()

            assertEquals("auth.scenario", filters.scenarioFile)
            assertEquals("Login", filters.scenarioName)
            assertEquals("Auth", filters.featureName)
            assertTrue(filters.hasFilters)
        }
    }

    @Nested
    @DisplayName("matchesFile")
    inner class MatchesFile {
        @Test
        fun `returns true when no filter set`() {
            val filters = ScenarioFilters()

            assertTrue(filters.matchesFile("scenarios/login.scenario", "login.scenario"))
        }

        @Test
        fun `matches by filename`() {
            val filters = ScenarioFilters(scenarioFile = "login.scenario")

            assertTrue(filters.matchesFile("scenarios/login.scenario", "login.scenario"))
            assertFalse(filters.matchesFile("scenarios/signup.scenario", "signup.scenario"))
        }

        @Test
        fun `matches by path suffix`() {
            val filters = ScenarioFilters(scenarioFile = "auth/login.scenario")

            assertTrue(filters.matchesFile("scenarios/auth/login.scenario", "login.scenario"))
            assertFalse(filters.matchesFile("scenarios/login.scenario", "login.scenario"))
        }

        @Test
        fun `matches by partial path`() {
            val filters = ScenarioFilters(scenarioFile = "auth")

            assertTrue(filters.matchesFile("scenarios/auth/login.scenario", "login.scenario"))
            assertFalse(filters.matchesFile("scenarios/public/login.scenario", "login.scenario"))
        }
    }

    @Nested
    @DisplayName("matchesScenarioName")
    inner class MatchesScenarioName {
        @Test
        fun `returns true when no filter set`() {
            val filters = ScenarioFilters()

            assertTrue(filters.matchesScenarioName("Any Scenario"))
        }

        @Test
        fun `matches exact name case-insensitive`() {
            val filters = ScenarioFilters(scenarioName = "User Login")

            assertTrue(filters.matchesScenarioName("User Login"))
            assertTrue(filters.matchesScenarioName("user login"))
            assertTrue(filters.matchesScenarioName("USER LOGIN"))
            assertFalse(filters.matchesScenarioName("User Signup"))
        }
    }

    @Nested
    @DisplayName("matchesFeatureName")
    inner class MatchesFeatureName {
        @Test
        fun `returns true when no filter set`() {
            val filters = ScenarioFilters()

            assertTrue(filters.matchesFeatureName("Any Feature"))
        }

        @Test
        fun `matches exact name case-insensitive`() {
            val filters = ScenarioFilters(featureName = "Authentication")

            assertTrue(filters.matchesFeatureName("Authentication"))
            assertTrue(filters.matchesFeatureName("authentication"))
            assertTrue(filters.matchesFeatureName("AUTHENTICATION"))
            assertFalse(filters.matchesFeatureName("Authorization"))
        }
    }

    @Nested
    @DisplayName("EMPTY constant")
    inner class EmptyConstant {
        @Test
        fun `has no filters`() {
            assertFalse(ScenarioFilters.EMPTY.hasFilters)
            assertNull(ScenarioFilters.EMPTY.scenarioFile)
            assertNull(ScenarioFilters.EMPTY.scenarioName)
            assertNull(ScenarioFilters.EMPTY.featureName)
        }

        @Test
        fun `matches everything`() {
            assertTrue(ScenarioFilters.EMPTY.matchesFile("any/path.scenario", "path.scenario"))
            assertTrue(ScenarioFilters.EMPTY.matchesScenarioName("Any Scenario"))
            assertTrue(ScenarioFilters.EMPTY.matchesFeatureName("Any Feature"))
        }
    }
}
