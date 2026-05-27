package org.berrycrush.executor.http

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.BodyProperty
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DefaultHttpExecutorTest {
    private val configuration = BerryCrushConfiguration()
    private val executor = DefaultHttpExecutor(configuration)

    // --- resolveBody Tests ---

    @Test
    fun `resolveBody - inline body takes precedence`() {
        val step =
            createStep(
                operationId = "createPet",
                body = """{"name": "Fluffy"}""",
            )
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertEquals("""{"name": "Fluffy"}""", body)
    }

    @Test
    fun `resolveBody - interpolates variables in inline body`() {
        val step =
            createStep(
                operationId = "createPet",
                body = """{"name": "${"$"}{petName}"}""",
            )
        val context = ExecutionContext()
        context["petName"] = "Max"

        val body = executor.resolveBody(step, null, context)

        assertEquals("""{"name": "Max"}""", body)
    }

    @Test
    fun `resolveBody - returns null when no body configured`() {
        val step = createStep(operationId = "getPets")
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertNull(body)
    }

    @Test
    fun `resolveBody - generates body from structured properties`() {
        val step =
            createStep(
                operationId = "createPet",
                bodyProperties =
                    mapOf(
                        "name" to BodyProperty.Simple("Fluffy"),
                        "age" to BodyProperty.Simple(3),
                    ),
            )
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertNotNull(body)
        // Body should contain both properties as JSON
        assert(body.contains("\"name\""))
        assert(body.contains("Fluffy"))
        assert(body.contains("\"age\""))
        assert(body.contains("3"))
    }

    @Test
    fun `resolveBody - handles nested body properties`() {
        val step =
            createStep(
                operationId = "createPet",
                bodyProperties =
                    mapOf(
                        "pet" to
                            BodyProperty.Nested(
                                mapOf(
                                    "name" to BodyProperty.Simple("Fluffy"),
                                    "owner" to BodyProperty.Simple("John"),
                                ),
                            ),
                    ),
            )
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertNotNull(body)
        assert(body.contains("\"pet\""))
        assert(body.contains("\"name\""))
        assert(body.contains("Fluffy"))
    }

    @Test
    fun `resolveBody - interpolates variables in body properties`() {
        val step =
            createStep(
                operationId = "createPet",
                bodyProperties =
                    mapOf(
                        "name" to BodyProperty.Simple("${"$"}{petName}"),
                    ),
            )
        val context = ExecutionContext()
        context["petName"] = "Max"

        val body = executor.resolveBody(step, null, context)

        assertNotNull(body)
        assert(body.contains("Max"))
    }

    @Test
    fun `resolveBody - handles boolean properties`() {
        val step =
            createStep(
                operationId = "updatePet",
                bodyProperties =
                    mapOf(
                        "available" to BodyProperty.Simple(true),
                    ),
            )
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertNotNull(body)
        assert(body.contains("true"))
    }

    @Test
    fun `resolveBody - handles list properties`() {
        val step =
            createStep(
                operationId = "createPet",
                bodyProperties =
                    mapOf(
                        "tags" to BodyProperty.Simple(listOf("cat", "fluffy")),
                    ),
            )
        val context = ExecutionContext()

        val body = executor.resolveBody(step, null, context)

        assertNotNull(body)
        assert(body.contains("\"tags\""))
        assert(body.contains("cat"))
        assert(body.contains("fluffy"))
    }

    // --- Helper Functions ---

    private fun createStep(
        operationId: String? = null,
        body: String? = null,
        bodyProperties: Map<String, BodyProperty>? = null,
    ): Step =
        Step(
            type = StepType.WHEN,
            description = "test step",
            operationId = operationId,
            body = body,
            bodyProperties = bodyProperties,
        )
}
