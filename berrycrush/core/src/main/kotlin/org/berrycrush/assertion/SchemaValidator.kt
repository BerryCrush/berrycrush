package org.berrycrush.assertion

import com.networknt.schema.Error
import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import org.berrycrush.exception.SchemaValidationException
import org.berrycrush.model.ValidationError
import tools.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.media.Schema as OpenApiSchema

/**
 * Validates JSON responses against OpenAPI schemas.
 */
class SchemaValidator(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    // Use SchemaRegistry supporting all standard dialects, default to Draft 2020-12
    private val schemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

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
        schema: OpenApiSchema<*>,
        strict: Boolean = false,
    ): List<ValidationError> {
        val jsonSchema = convertToJsonSchema(schema, strict)
        return validateAgainstSchema(responseBody, jsonSchema)
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
        schema: OpenApiSchema<*>,
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
        val schema = schemaRegistry.getSchema(jsonSchemaString, InputFormat.JSON)
        return validateAgainstSchema(responseBody, schema)
    }

    private fun validateAgainstSchema(
        responseBody: String,
        schema: Schema,
    ): List<ValidationError> {
        val errors: List<Error> = schema.validate(responseBody, InputFormat.JSON)

        return errors.map { error ->
            ValidationError(
                path = error.instanceLocation.toString(),
                message = error.message,
                keyword = error.keyword ?: "unknown",
                schemaPath = error.schemaLocation.toString(),
            )
        }
    }

    private fun convertToJsonSchema(
        schema: OpenApiSchema<*>,
        strict: Boolean,
    ): Schema {
        val jsonSchemaMap = buildJsonSchemaMap(schema, strict)
        val jsonSchemaString = objectMapper.writeValueAsString(jsonSchemaMap)
        return schemaRegistry.getSchema(jsonSchemaString, InputFormat.JSON)
    }

    private fun buildJsonSchemaMap(
        schema: OpenApiSchema<*>,
        strict: Boolean,
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        result[$$"$schema"] = "https://json-schema.org/draft/2020-12/schema"

        // Handle type - support both single type and array types (3.1 feature)
        @Suppress("UNCHECKED_CAST")
        when {
            schema.types != null && schema.types.isNotEmpty() -> {
                val types = schema.types as? List<String>
                if (types != null) {
                    result["type"] = if (types.size == 1) types.first() else types
                }
            }
            schema.type != null -> {
                // Handle 3.0 nullable: true -> 3.1 type: ["type", "null"]
                if (schema.nullable == true) {
                    result["type"] = listOf(schema.type, "null")
                } else {
                    result["type"] = schema.type
                }
            }
        }

        schema.format?.let { result["format"] = it }

        // Numeric constraints
        schema.minimum?.let { result["minimum"] = it }
        schema.maximum?.let { result["maximum"] = it }
        schema.exclusiveMinimum?.let { result["exclusiveMinimum"] = it }
        schema.exclusiveMaximum?.let { result["exclusiveMaximum"] = it }
        schema.multipleOf?.let { result["multipleOf"] = it }

        // String constraints
        schema.minLength?.let { result["minLength"] = it }
        schema.maxLength?.let { result["maxLength"] = it }
        schema.pattern?.let { result["pattern"] = it }

        // 3.1 content encoding
        schema.contentMediaType?.let { result["contentMediaType"] = it }
        schema.contentEncoding?.let { result["contentEncoding"] = it }

        // Enum and const (3.1 feature)
        schema.enum?.let { result["enum"] = it }
        schema.const?.let { result["const"] = it }

        // Handle object properties
        schema.properties?.let { props ->
            result["properties"] =
                props.mapValues { (_, v) ->
                    buildJsonSchemaMap(v, strict)
                }
        }

        schema.required?.let { result["required"] = it }
        schema.minProperties?.let { result["minProperties"] = it }
        schema.maxProperties?.let { result["maxProperties"] = it }

        // additionalProperties - can be boolean or schema
        when (val addProps = schema.additionalProperties) {
            is Boolean -> result["additionalProperties"] = addProps
            is OpenApiSchema<*> -> result["additionalProperties"] = buildJsonSchemaMap(addProps, strict)
            else -> if (strict && schema.type == "object") {
                result["additionalProperties"] = false
            }
        }

        // 3.1 dependent schemas
        schema.dependentRequired?.let { result["dependentRequired"] = it }
        @Suppress("UNCHECKED_CAST")
        (schema.dependentSchemas as? Map<String, OpenApiSchema<*>>)?.let { deps ->
            result["dependentSchemas"] = deps.mapValues { (_, v) -> buildJsonSchemaMap(v, strict) }
        }

        // Handle array items
        schema.items?.let { items ->
            result["items"] = buildJsonSchemaMap(items, strict)
        }

        // 3.1 prefixItems for tuple validation
        @Suppress("UNCHECKED_CAST")
        (schema.prefixItems as? List<OpenApiSchema<*>>)?.let { prefixItems ->
            result["prefixItems"] = prefixItems.map { buildJsonSchemaMap(it, strict) }
        }

        schema.minItems?.let { result["minItems"] = it }
        schema.maxItems?.let { result["maxItems"] = it }
        schema.uniqueItems?.let { if (it) result["uniqueItems"] = true }

        // Composition keywords
        @Suppress("UNCHECKED_CAST")
        (schema.allOf as? List<OpenApiSchema<*>>)?.let { schemas ->
            result["allOf"] = schemas.map { buildJsonSchemaMap(it, strict) }
        }
        @Suppress("UNCHECKED_CAST")
        (schema.anyOf as? List<OpenApiSchema<*>>)?.let { schemas ->
            result["anyOf"] = schemas.map { buildJsonSchemaMap(it, strict) }
        }
        @Suppress("UNCHECKED_CAST")
        (schema.oneOf as? List<OpenApiSchema<*>>)?.let { schemas ->
            result["oneOf"] = schemas.map { buildJsonSchemaMap(it, strict) }
        }
        @Suppress("UNCHECKED_CAST")
        (schema.not as? OpenApiSchema<*>)?.let { notSchema ->
            result["not"] = buildJsonSchemaMap(notSchema, strict)
        }

        // 3.1 conditional schemas (if/then/else)
        @Suppress("UNCHECKED_CAST")
        (schema.`if` as? OpenApiSchema<*>)?.let { ifSchema ->
            result["if"] = buildJsonSchemaMap(ifSchema, strict)
        }
        @Suppress("UNCHECKED_CAST")
        (schema.then as? OpenApiSchema<*>)?.let { thenSchema ->
            result["then"] = buildJsonSchemaMap(thenSchema, strict)
        }
        @Suppress("UNCHECKED_CAST")
        (schema.`else` as? OpenApiSchema<*>)?.let { elseSchema ->
            result["else"] = buildJsonSchemaMap(elseSchema, strict)
        }

        // $ref handling - in 3.1, $ref can coexist with other keywords
        schema.`$ref`?.let { result["\$ref"] = it }

        return result
    }
}
