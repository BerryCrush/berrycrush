package org.berrycrush.samples.petstore.steps

import org.berrycrush.step.Step
import org.berrycrush.step.StepContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom step definitions for Petstore scenarios.
 *
 * These steps demonstrate BerryCrush's custom step capability with
 * user-defined logic that can interact with the test context.
 */
class PetstoreSteps {
    // In-memory pet data storage for demonstration
    private data class PetData(
        val name: String,
        val status: String,
        val price: Double = 0.0,
    )

    private val petDataStore = ConcurrentHashMap<String, PetData>()

    @Step("I have a pet named {string} with status {string}")
    fun createPetData(
        name: String,
        status: String,
        context: StepContext,
    ) {
        val pet = PetData(name = name, status = status)
        petDataStore[name] = pet

        // Store in context for use by other steps
        context.setVariable("currentPetName", name)
        context.setVariable("currentPetStatus", status)
    }

    @Step("the pet data should contain {string}")
    @Suppress("ThrowsCount") // Multiple validation checks in step assertion
    fun verifyPetDataContains(
        expectedValue: String,
        context: StepContext,
    ) {
        val petName = context.variable("currentPetName") as? String
        checkNotNull(petName) { "No pet data set" }

        val pet = petDataStore[petName]
        checkNotNull(pet) { "Pet '$petName' not found in data store" }

        val containsValue =
            pet.name.contains(expectedValue) ||
                pet.status.contains(expectedValue)

        check(containsValue) { "Pet data does not contain '$expectedValue'. Pet: $pet" }
    }

    @Step("I should have {int} pets with status {word}")
    fun verifyPetCount(
        expectedCount: Int,
        expectedStatus: String,
        @Suppress("UNUSED_PARAMETER") context: StepContext,
    ) {
        val matchingPets = petDataStore.values.filter { it.status == expectedStatus }

        if (matchingPets.size != expectedCount) {
            throw AssertionError(
                "Expected $expectedCount pets with status '$expectedStatus', " +
                    "but found ${matchingPets.size}",
            )
        }
    }

    @Step("the pet price should be {float}")
    @Suppress("ThrowsCount") // Multiple validation checks in step assertion
    fun verifyPetPrice(
        expectedPrice: Double,
        context: StepContext,
    ) {
        val petName = context.variable("currentPetName") as? String
        checkNotNull(petName) { "No pet data set" }

        // For demo, we update price when checking
        val pet = petDataStore[petName]
        checkNotNull(pet) { "Pet '$petName' not found" }

        petDataStore[petName] = pet.copy(price = 199.99)

        // Verify against expected
        val actualPrice = petDataStore[petName]?.price
        check(actualPrice == expectedPrice) {
            "Expected price $expectedPrice, but got $actualPrice"
        }
    }

    @Step("I reset the pet data")
    fun resetPetData(
        @Suppress("UNUSED_PARAMETER") context: StepContext,
    ) {
        petDataStore.clear()
    }

    @Step("I modify the name of the pet {string}")
    fun modifyPetName(
        petName: String,
        context: StepContext,
    ) {
        val pet = petDataStore[petName]
        if (pet != null) {
            val newName = petName.uppercase()
            petDataStore.remove(petName)
            petDataStore[newName] = pet.copy(name = newName)
            context.setVariable("modifiedPetName", newName)
        } else {
            // Use the variable if direct lookup fails
            val varPetName = context.variable(petName) as? String
            if (varPetName != null) {
                val existingPet = petDataStore[varPetName]
                if (existingPet != null) {
                    val newName = varPetName.uppercase()
                    petDataStore.remove(varPetName)
                    petDataStore[newName] = existingPet.copy(name = newName)
                    context.setVariable("modifiedPetName", newName)
                }
            }
        }
    }
}
