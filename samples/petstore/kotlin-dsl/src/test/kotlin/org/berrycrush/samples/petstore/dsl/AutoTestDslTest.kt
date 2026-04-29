package org.berrycrush.samples.petstore.dsl

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.dsl.BerryCrushSuite
import org.berrycrush.junit.BerryCrushExtension
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.junit.ScenarioTest
import org.berrycrush.model.Scenario
import org.berrycrush.samples.petstore.PetstoreApplication
import org.berrycrush.scenario.AutoTestType
import org.berrycrush.spring.BerryCrushContextConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

/**
 * Demonstrates auto-test generation using the Kotlin DSL.
 *
 * Auto-tests automatically generate test cases based on the OpenAPI schema:
 * - INVALID: Tests that violate schema constraints (minLength, maxLength, pattern, etc.)
 * - SECURITY: Tests for common security vulnerabilities (SQL injection, XSS, etc.)
 * - MULTI: Idempotency tests that send multiple requests (sequential/concurrent)
 *
 * Test display names follow these formats:
 * - `[invalid - minLength]` for invalid tests
 * - `[security - SQL Injection]` for security tests
 * - `[multi:sequential] 3 requests` for multi tests
 */
@SpringBootTest(
    classes = [PetstoreApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BerryCrushExtension::class)
@BerryCrushContextConfiguration
@BerryCrushSpec("classpath:/petstore.yaml")
@DisplayName("Auto-Test DSL Examples")
class AutoTestDslTest {
    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup(config: BerryCrushConfiguration) {
        config.baseUrl = "http://localhost:$port/api/v1"
    }

    // ========== INVALID Auto-Tests ==========

    @ScenarioTest
    @DisplayName("Create pet with invalid input tests")
    fun invalidAutoTests(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("Create pet - Invalid input tests") {
            whenever("I create a pet with invalid data") {
                call("createPet") {
                    body("""{"name": "Fluffy", "category": "cat"}""")
                    // Enable auto-test generation for invalid inputs
                    autoTest(AutoTestType.INVALID)
                }
            }
            afterwards("The API returns appropriate error responses") {
                statusCode(400..499)
            }
        }
    }

    // ========== SECURITY Auto-Tests ==========

    @ScenarioTest
    @DisplayName("Create pet with security tests")
    fun securityAutoTests(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("Create pet - Security tests") {
            whenever("I test security vulnerabilities") {
                call("createPet") {
                    body("""{"name": "Fluffy", "category": "cat"}""")
                    // Enable security test generation
                    autoTest(AutoTestType.SECURITY)
                }
            }
            // Accept any response - auto-tests have their own assertions
            afterwards("The request completes") {
                statusCode(200..599)
            }
        }
    }

    // ========== MULTI (Idempotency) Auto-Tests ==========

    @ScenarioTest
    @DisplayName("List pets idempotency test")
    fun multiSequentialTest(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("List pets - Idempotency test") {
            whenever("I send multiple requests for idempotency testing") {
                call("listPets") {
                    // Enable multi-request idempotency testing
                    autoTest(AutoTestType.MULTI)
                }
            }
            afterwards("The requests complete successfully") {
                statusCode(200..599)
            }
        }
    }

    // ========== Combined Auto-Tests ==========

    @ScenarioTest
    @DisplayName("Create pet with all auto-tests")
    fun combinedAutoTests(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("Create pet - All auto-tests") {
            whenever("I test the create pet endpoint comprehensively") {
                call("createPet") {
                    body("""{"name": "Fluffy", "category": "cat"}""")
                    // Enable all auto-test types
                    autoTest(AutoTestType.INVALID, AutoTestType.SECURITY, AutoTestType.MULTI)
                }
            }
            afterwards("The API behaves correctly") {
                // Accept both success and expected error responses
                statusCode(200..499)
            }
        }
    }

    // ========== Using Boolean Parameters ==========

    @ScenarioTest
    @DisplayName("Create pet with boolean auto-test config")
    fun booleanAutoTestConfig(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("Create pet - Boolean config") {
            whenever("I test with boolean auto-test configuration") {
                call("createPet") {
                    body("""{"name": "Fluffy", "category": "cat"}""")
                    // Alternative API using boolean parameters
                    autoTest(invalid = true, security = true, multi = false)
                }
            }
            afterwards("The API validates inputs") {
                statusCode(200..499)
            }
        }
    }

    // ========== Excluding Test Categories ==========

    @ScenarioTest
    @DisplayName("Create pet with excluded categories")
    fun excludeCategories(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"

        return suite.scenario("Create pet - Exclude categories") {
            whenever("I test excluding certain categories") {
                call("createPet") {
                    body("""{"name": "Fluffy", "category": "cat"}""")
                    // Enable auto-tests but exclude specific categories
                    autoTest(AutoTestType.INVALID, AutoTestType.SECURITY)
                    excludes("XSS", "minLength")
                }
            }
            afterwards("The API validates remaining inputs") {
                statusCode(200..499)
            }
        }
    }

    // ========== Custom Multi-Test Parameters ==========

    @ScenarioTest
    @DisplayName("List pets with custom multi-test parameters")
    fun customMultiTestParams(suite: BerryCrushSuite): Scenario {
        suite.configuration.baseUrl = "http://localhost:$port/api/v1"
        // Set custom multi-test parameters at configuration level
        suite.configuration.multiTestSequentialCount = 5
        suite.configuration.multiTestConcurrentCount = 10

        return suite.scenario("List pets - Custom multi-test params") {
            whenever("I send custom number of requests for idempotency") {
                call("listPets") {
                    autoTest(AutoTestType.MULTI)
                }
            }
            afterwards("All requests complete successfully") {
                statusCode(200..599)
            }
        }
    }
}
