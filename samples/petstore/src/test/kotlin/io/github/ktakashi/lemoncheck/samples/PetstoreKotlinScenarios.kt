package io.github.ktakashi.lemoncheck.samples

import io.github.ktakashi.lemoncheck.junit.LemonCheckSpec
import io.github.ktakashi.lemoncheck.junit.ScenarioTest

/**
 * Sample Petstore API tests demonstrating LemonCheck features.
 *
 * This sample covers all 5 user stories:
 * - US1: Simple scenario (list pets, check response)
 * - US2: Data flow between steps (create pet, extract ID, retrieve)
 * - US3: Reusable fragments (authentication)
 * - US4: Parameterized scenarios (multiple pets)
 * - US5: Schema validation (auto-assertions)
 */
@LemonCheckSpec("petstore.yaml")
class PetstoreKotlinScenarios : ScenarioTest() {
    override fun configureSuite() {
        // Configure test suite for Petstore API
        configure {
            baseUrl = "https://petstore.swagger.io/v2"
            timeout(30)
            header("Accept", "application/json")
        }
    }

    override fun defineScenarios() {
        // US1: Simple Scenario - List all pets
        scenario("US1 - List all pets") {
            given("the Petstore API is available")

            `when`("I request the list of pets") {
                call("findPetsByStatus") {
                    queryParam("status", "available")
                }
            }

            then("I receive a list of available pets") {
                statusCode(200)
                bodyArrayNotEmpty("$")
            }
        }

        // US1: Simple Scenario - Get pet by ID
        scenario("US1 - Get pet by ID", tags = setOf("smoke", "crud")) {
            given("a specific pet exists")

            `when`("I request the pet details") {
                call("getPetById") {
                    pathParam("petId", 1)
                }
            }

            then("I receive the pet information") {
                statusCode(200)
                bodyEquals("$.id", 1)
            }
        }

        // US2: Data Flow - Create and retrieve a pet
        scenario("US2 - Create and retrieve pet") {
            `when`("I create a new pet") {
                call("addPet") {
                    body(
                        mapOf(
                            "name" to "Fluffy",
                            "photoUrls" to listOf("https://example.com/fluffy.jpg"),
                            "status" to "available",
                        ),
                    )
                }
                extractTo("petId", "$.id")
            }

            then("the pet is created successfully") {
                statusCode(200)
            }

            and("I can retrieve the created pet") {
                call("getPetById") {
                    pathParam("petId", $$"${petId}")
                }
            }

            then("I see the correct pet details") {
                statusCode(200)
                bodyEquals("$.name", "Fluffy")
            }
        }

        // US2: Data Flow - Update and delete flow
        scenario("US2 - Update and delete pet") {
            given("I have a pet to update") {
                call("addPet") {
                    body(
                        mapOf(
                            "name" to "OldName",
                            "photoUrls" to listOf("https://example.com/photo.jpg"),
                            "status" to "available",
                        ),
                    )
                }
                extractTo("petId", "$.id")
            }

            `when`("I update the pet name") {
                call("updatePet") {
                    body(
                        mapOf(
                            "id" to $$"${petId}",
                            "name" to "NewName",
                            "photoUrls" to listOf("https://example.com/photo.jpg"),
                            "status" to "pending",
                        ),
                    )
                }
            }

            then("the update is successful") {
                statusCode(200)
            }

            and("I delete the pet") {
                call("deletePet") {
                    pathParam("petId", $$"${petId}")
                }
            }

            then("the pet is deleted") {
                statusCode(200)
            }
        }

        // US3: Fragments - Authentication scenario
        // Note: Since fragment reuse isn't directly supported in ScenarioScope,
        // we demonstrate authentication as a standalone scenario
        scenario("US3 - Access protected resource with authentication") {
            given("I have valid credentials") {
                call("loginUser") {
                    queryParam("username", "test")
                    queryParam("password", "abc123")
                }
                extractTo("sessionToken", "$.message")
            }

            `when`("I access a protected endpoint") {
                call("getInventory") {
                    header("Authorization", $$"Bearer ${sessionToken}")
                }
            }

            then("I receive the inventory data") {
                statusCode(200)
            }
        }

        // US4: Parameterized Scenarios - Multiple pet statuses
        // Note: For parameterized scenarios, use the placeholder syntax in strings
        scenarioOutline("US4 - Filter pets by status: <status>") {
            `when`("I filter pets by <status> status") {
                call("findPetsByStatus") {
                    queryParam("status", "<status>")
                }
            }

            then("I receive pets with <status> status") {
                statusCode(200)
            }

            examples(
                row("status" to "available"),
                row("status" to "pending"),
                row("status" to "sold"),
            )
        }

        // US5: Schema Validation - Auto-assertions
        scenario("US5 - Validate response against schema", tags = setOf("schema")) {
            `when`("I request a pet") {
                call("getPetById") {
                    pathParam("petId", 1)
                    // Auto-assertions enabled by default
                }
            }

            then("the response matches the OpenAPI schema") {
                matchesSchema()
                statusCode(200)
            }
        }

        // US5: Disable auto-assertions
        scenario("US5 - Manual assertions (auto-assert disabled)") {
            `when`("I request a pet without auto-assertions") {
                call("getPetById") {
                    pathParam("petId", 1)
                    autoAssert(false)
                }
            }

            then("I can add manual assertions only") {
                statusCode(200)
                bodyContains("name")
            }
        }
    }
}
