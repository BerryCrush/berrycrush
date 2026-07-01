package org.berrycrush.samples.petstore.dsl

import org.berrycrush.dsl.scenario
import org.berrycrush.junit.BerryCrushExtension
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.junit.ScenarioTest
import org.berrycrush.model.Scenario
import org.berrycrush.samples.petstore.PetstoreApplication
import org.berrycrush.spring.BerryCrushContextConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(
    classes = [PetstoreApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@BerryCrushContextConfiguration
@ExtendWith(BerryCrushExtension::class)
@BerryCrushSpec("classpath:/petstore.yaml")
@BerryCrushSpec(name = "auth", paths = ["classpath:/auth.yaml"])
class MultiSpecPetstoreApiTest {
    @LocalServerPort
    private var port: Int = 0

    @ScenarioTest
    fun listAllPetsTestWithAuth(suite: BerryCrushSuite): Scenario {
        return suite.scenario("list all pets") {
            parameters {
                // binding.* = default binding config
                binding {
                    baseUrl = "http://localhost:$port/api/v1"
                }
                // binding.{name}.* = named binding config
                binding("auth") {
                    baseUrl = "http://localhost:$port/auth/api/v1"
                }
                set("logRequests" to true)
                set("logResponses" to true)
            }
            given("authenticated") {
                using("auth")
                call("login") {
                    body("username" to "admin", "password" to "admin")
                }
                extractTo("authToken", "\$.token")
            }
            whenever("I get all pets") {
                call("listPets") {
                    bearerToken("{{authToken}}")
                    header("X-Debug-Token", "{{authToken}}")
                }
            }
            afterwards("the response should be 200") {
                statusCode(200)
            }
        }
    }
}