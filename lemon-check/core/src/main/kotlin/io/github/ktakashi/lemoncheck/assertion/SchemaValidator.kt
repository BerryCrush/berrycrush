package io.github.ktakashi.lemoncheck.assertion

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import io.github.ktakashi.lemoncheck.exception.SchemaValidationException
import io.github.ktakashi.lemoncheck.model.ValidationError
import io.swagger.v3.oas.models.media.Schema

/**
 * Validates JSON responses against OpenAPI schemas.
 */
class SchemaValidator(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)

    /**
     * Validate a JSON response against an OpenAPI schema.
     *
     * @param responseBody JSON response body to validate
     * @param schema OpenAPI schema to validate against
     * @param strict If true, fail on additional properties not in schema
     * @return List of validation errors (empty if valid)
     */
    fun validate(
        responseBody: String,
        schema: Schema<*>,
        strict: Boolean = false,
    ): List<ValidationError> {
        val jsonSchema = convertToJsonSchema(schema, strict)
        val jsonNode = objectMapper.readTree(responseBody)

        return validateAgainstSchema(jsonNode, jsonSchema)
    }

    /**
     * Validate a JSON response, throwing an exception if invalid.
     *
     * @param responseBody JSON response body to validate
     * @param schema OpenAPI schema to validate against
     * @param strict If true, fail on additional properties not in schema
     * @throws SchemaValidationException if validation fails
     */
    fun validateOrThrow(
        responseBody: String,
        schema: Schema<*>,
        strict: Boolean = false,
    ) {
        val errors = validate(responseBody, schema, strict)
        if (errors.isNotEmpty()) {
            throw SchemaValidationException(errors.map { it.message })
        }
    }

    /**
     * Validate JSON against a JSON Schema string.
     *
     * @param responseBody JSON response body
     * @param jsonSchemaString JSON Schema as a string
     * @return List of validation errors
     */
    fun validateAgainstJsonSchema(
        responseBody: String,
        jsonSchemaString: String,
    ): List<ValidationError> {
        val jsonSchema = schemaFactory.getSchema(jsonSchemaString)
        val jsonNode = objectMapper.readTree(responseBody)

        return validateAgainstSchema(jsonNode, jsonSchema)
    }

    private fun validateAgainstSchema(
        jsonNode: JsonNode,
        schema: JsonSchema,
    ): List<ValidationError> {
        val validationMessages = schema.validate(jsonNode)

        return validationMessages.map { msg ->
            ValidationError(
                path = msg.instanceLocation.toString(),
                message = msg.message,
                keyword = msg.type,
                schemaPath = msg.schemaLocation.toString(),
            )
        }
    }

    private fun convertToJsonSchema(
        schema: Schema<*>,
        strict: Boolean,
    ): JsonSchema {
        val jsonSchemaMap = buildJsonSchemaMap(schema, strict)
        val jsonSchemaString = objectMapper.writeValueAsString(jsonSchemaMap)
        return schemaFactory.getSchema(jsonSchemaString)
    }

    private fun buildJsonSchemaMap(
        schema: Schema<*>,
        strict: Boolean,
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        result[$$"$schema"] = "http://json-schema.org/draft-07/schema#"

        schema.type?.let { result["type"] = it }
        schema.format?.let { result["format"] = it }
        schema.minimum?.let { result["minimum"] = it }
        schema.maximum?.let { result["maximum"] = it }
        schema.minLength?.let { result["minLength"] = it }
        schema.maxLength?.let { result["maxLength"] = it }
        schema.pattern?.let { result["pattern"] = it }
        schema.enum?.let { result["enum"] = it }

        // Handle object properties
        schema.properties?.let { props ->
            result["properties"] =
                props.mapValues { (_, v) ->
                    buildJsonSchemaMap(v, strict)
                }
        }

        schema.required?.let { result["required"] = it }

        if (strict && schema.type == "object") {
            result["additionalProperties"] = false
        }

        // Handle array items
        schema.items?.let { items ->
            result["items"] = buildJsonSchemaMap(items, strict)
        }

        schema.minItems?.let { result["minItems"] = it }
        schema.maxItems?.let { result["maxItems"] = it }

        return result
    }
}
