package org.berrycrush.autotest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.berrycrush.autotest.provider.AutoTestProviderRegistry
import org.berrycrush.autotest.provider.InvalidTestRequest
import org.berrycrush.autotest.provider.SecurityTestRequest
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.SpecRegistry
import io.swagger.v3.oas.models.parameters.Parameter as SwaggerParameter

/**
 * Generates auto-test cases based on OpenAPI schema constraints and security patterns.
 *
 * This class analyzes an OpenAPI specification and generates test cases that:
 * - Violate schema constraints (invalid tests): minLength, maxLength, pattern, required, enum, type
 * - Include common attack payloads (security tests): SQL injection, XSS, path traversal, etc.
 *
 * Test cases are generated for:
 * - Request body fields
 * - Path parameters
 * - Header parameters
 *
 * ## Usage
 *
 * ```kotlin
 * val generator = AutoTestGenerator.fromSpec(loadedSpec)
 * val testCases = generator.generateTestCases(
 *     operationId = "createPet",
 *     testTypes = setOf(AutoTestType.INVALID, AutoTestType.SECURITY),
 *     baseBody = mapOf("name" to "ValidName")
 * )
 * ```
 *
 * @property openApi The parsed OpenAPI specification
 * @property registry The provider registry for extensibility
 * @see AutoTestCase The data class representing a generated test case
 * @see AutoTestProviderRegistry Provider registration for custom test types
 */
@Suppress("TooManyFunctions")
class AutoTestGenerator(
    private val openApi: OpenAPI,
    private val registry: AutoTestProviderRegistry = AutoTestProviderRegistry.default,
) {
    companion object {
        /**
         * Create an AutoTestGenerator from a SpecRegistry using the default spec.
         */
        fun fromRegistry(
            specRegistry: SpecRegistry,
            providerRegistry: AutoTestProviderRegistry = AutoTestProviderRegistry.default,
        ): AutoTestGenerator = AutoTestGenerator(specRegistry.getDefault().openApi, providerRegistry)

        /**
         * Create an AutoTestGenerator from a SpecRegistry for a specific spec.
         */
        fun fromRegistry(
            specRegistry: SpecRegistry,
            specName: String,
            providerRegistry: AutoTestProviderRegistry = AutoTestProviderRegistry.default,
        ): AutoTestGenerator = AutoTestGenerator(specRegistry.get(specName).openApi, providerRegistry)

        /**
         * Create an AutoTestGenerator from a LoadedSpec.
         */
        fun fromSpec(
            spec: LoadedSpec,
            providerRegistry: AutoTestProviderRegistry = AutoTestProviderRegistry.default,
        ): AutoTestGenerator = AutoTestGenerator(spec.openApi, providerRegistry)
    }

    /**
     * Generate test cases for an operation.
     *
     * @param operationId The operation to generate tests for
     * @param testTypes Which types of tests to generate
     * @param baseBody Optional existing body properties to start from
     * @param basePathParams Optional existing path parameters (with valid values for other params)
     * @param baseHeaders Optional existing headers (with valid values for other headers)
     * @return List of generated test cases
     */
    fun generateTestCases(
        operationId: String,
        testTypes: Set<AutoTestType>,
        baseBody: Map<String, Any?>? = null,
        basePathParams: Map<String, Any?>? = null,
        baseHeaders: Map<String, String>? = null,
    ): List<AutoTestCase> {
        val operation = findOperation(operationId) ?: return emptyList()
        val requestBodySchema = extractRequestBodySchema(operation)

        val testCases = mutableListOf<AutoTestCase>()
        val effectiveBody = baseBody ?: emptyMap()
        val effectivePathParams = basePathParams ?: emptyMap()
        val effectiveHeaders = baseHeaders ?: emptyMap()

        // Generate tests for request body
        if (requestBodySchema != null) {
            testCases.addAll(
                generateByType(
                    testTypes = testTypes,
                    invalidGenerator = { generateInvalidTestCases(requestBodySchema, effectiveBody) },
                    securityGenerator = { generateSecurityTestCases(requestBodySchema, effectiveBody) },
                ),
            )
        }

        // Generate tests for path parameters
        val pathParams = getParametersByLocation(operation, ParameterLocation.PATH)
        if (pathParams.isNotEmpty()) {
            testCases.addAll(
                generateByType(
                    testTypes = testTypes,
                    invalidGenerator = { generatePathParamInvalidTests(pathParams, effectiveBody, effectivePathParams) },
                    securityGenerator = { generatePathParamSecurityTests(pathParams, effectiveBody, effectivePathParams) },
                ),
            )
        }

        // Generate tests for header parameters
        val headerParams = getParametersByLocation(operation, ParameterLocation.HEADER)
        if (headerParams.isNotEmpty()) {
            testCases.addAll(
                generateByType(
                    testTypes = testTypes,
                    invalidGenerator = { generateHeaderInvalidTests(headerParams, effectiveBody, effectiveHeaders) },
                    securityGenerator = { generateHeaderSecurityTests(headerParams, effectiveBody, effectiveHeaders) },
                ),
            )
        }

        return testCases
    }

    private fun getParametersByLocation(
        operation: Operation,
        location: ParameterLocation,
    ): List<SwaggerParameter> = operation.parameters?.filter { it.`in` == location.locationName } ?: emptyList()

    private fun generateByType(
        testTypes: Set<AutoTestType>,
        invalidGenerator: () -> List<AutoTestCase>,
        securityGenerator: () -> List<AutoTestCase>,
    ): List<AutoTestCase> {
        val testCases = mutableListOf<AutoTestCase>()
        if (AutoTestType.INVALID in testTypes) {
            testCases.addAll(invalidGenerator())
        }
        if (AutoTestType.SECURITY in testTypes) {
            testCases.addAll(securityGenerator())
        }
        return testCases
    }

    private fun findOperation(operationId: String): Operation? {
        openApi.paths?.values?.forEach { pathItem ->
            listOfNotNull(
                pathItem.get,
                pathItem.post,
                pathItem.put,
                pathItem.delete,
                pathItem.patch,
            ).forEach { operation ->
                if (operation.operationId == operationId) {
                    return operation
                }
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRequestBodySchema(operation: Operation): Schema<*>? {
        val content = operation.requestBody?.content
        // Try JSON content types
        val mediaType =
            content?.get("application/json")
                ?: content?.entries?.firstOrNull()?.value
        val schema = mediaType?.schema

        // Resolve $ref if present
        return schema?.`$ref`?.substringAfterLast("/")?.let { refName ->
            // if the schema is $ref and the ref name doesn't exist, then
            // we should return null
            openApi.components?.schemas?.get(refName) ?: return null
        } ?: schema
    }

    /**
     * Generate invalid test cases based on schema constraints.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateInvalidTestCases(
        schema: Schema<*>,
        baseBody: Map<String, Any?>,
    ): List<AutoTestCase> {
        val testCases = mutableListOf<AutoTestCase>()
        val properties = schema.properties ?: return testCases

        properties.forEach { (fieldName, fieldSchema) ->
            val resolvedSchema = resolveSchema(fieldSchema as Schema<*>)

            // Generate constraint violation tests
            testCases.addAll(generateConstraintViolations(fieldName, resolvedSchema, baseBody))
        }

        // Missing-required generation is provider-owned.
        testCases.addAll(generateRequiredBodyTestCases(schema, baseBody))

        return testCases
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveSchema(schema: Schema<*>): Schema<*> {
        if (schema.`$ref` != null) {
            val refName = schema.`$ref`.substringAfterLast("/")
            return openApi.components?.schemas?.get(refName) ?: schema
        }
        return schema
    }

    /**
     * Generate constraint violation tests using registered providers.
     */
    private fun generateConstraintViolations(
        fieldName: String,
        schema: Schema<*>,
        baseBody: Map<String, Any?>,
    ): List<AutoTestCase> =
        sortedInvalidProviders()
            .filter { it.canHandle(schema) }
            .filterNot { it.testType == "required" }
            .flatMap { provider ->
                provider.generateTestCases(
                    InvalidTestRequest(
                        fieldName = fieldName,
                        fieldPath = listOf(fieldName),
                        schema = schema,
                        location = ParameterLocation.BODY,
                        baseBody = baseBody,
                    ),
                )
            }

    private fun generateRequiredBodyTestCases(
        schema: Schema<*>,
        baseBody: Map<String, Any?>,
    ): List<AutoTestCase> {
        val requiredProvider = sortedInvalidProviders().find { it.testType == "required" } ?: return emptyList()
        val requiredPaths = collectRequiredFieldPaths(schema)

        return requiredPaths.flatMap { fieldPath ->
            requiredProvider.generateTestCases(
                InvalidTestRequest(
                    fieldName = fieldPath.joinToString("."),
                    fieldPath = fieldPath,
                    schema = schema,
                    location = ParameterLocation.BODY,
                    baseBody = baseBody,
                ),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectRequiredFieldPaths(
        schema: Schema<*>,
        prefix: List<String> = emptyList(),
    ): List<List<String>> {
        val resolved = resolveSchema(schema)
        val properties = resolved.properties ?: return emptyList()
        val required = resolved.required ?: emptyList()

        val currentLevel = required.map { prefix + it }

        val nested =
            properties.entries.flatMap { (name, propertySchema) ->
                collectRequiredFieldPaths(resolveSchema(propertySchema as Schema<*>), prefix + name)
            }

        return currentLevel + nested
    }

    /**
     * Generate security test cases using registered providers.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateSecurityTestCases(
        schema: Schema<*>,
        baseBody: Map<String, Any?>,
    ): List<AutoTestCase> {
        val properties = schema.properties ?: return emptyList()

        // Find string fields to test
        val stringFields =
            properties
                .filter { (_, fieldSchema) ->
                    val resolved = resolveSchema(fieldSchema as Schema<*>)
                    resolved.type == "string" || resolved.type == null
                }.keys

        return stringFields.flatMap { fieldName ->
            sortedSecurityProviders()
                .filter { ParameterLocation.BODY in it.applicableLocations() }
                .flatMap { provider ->
                    provider.generateTestCases(
                        SecurityTestRequest(
                            fieldName = fieldName,
                            location = ParameterLocation.BODY,
                            baseBody = baseBody,
                        ),
                    )
                }
        }
    }

    /**
     * Generate invalid tests for path parameters using providers.
     */
    private fun generatePathParamInvalidTests(
        pathParams: List<SwaggerParameter>,
        baseBody: Map<String, Any?>,
        basePathParams: Map<String, Any?>,
    ): List<AutoTestCase> =
        pathParams.flatMap { param ->
            val schema = param.schema ?: return@flatMap emptyList()
            val resolvedSchema = resolveSchema(schema)

            sortedInvalidProviders()
                .filter { it.canHandle(resolvedSchema) }
                .filterNot { it.testType == "required" }
                .flatMap { provider ->
                    provider.generateTestCases(
                        InvalidTestRequest(
                            fieldName = param.name,
                            fieldPath = listOf(param.name),
                            schema = resolvedSchema,
                            location = ParameterLocation.PATH,
                            baseBody = baseBody,
                            basePathParams = basePathParams,
                        ),
                    )
                }
        }

    /**
     * Generate security tests for path parameters using providers.
     */
    private fun generatePathParamSecurityTests(
        pathParams: List<SwaggerParameter>,
        baseBody: Map<String, Any?>,
        basePathParams: Map<String, Any?>,
    ): List<AutoTestCase> =
        pathParams.flatMap { param ->
            sortedSecurityProviders()
                .filter { ParameterLocation.PATH in it.applicableLocations() }
                .flatMap { provider ->
                    provider.generateTestCases(
                        SecurityTestRequest(
                            fieldName = param.name,
                            location = ParameterLocation.PATH,
                            baseBody = baseBody,
                            basePathParams = basePathParams,
                        ),
                    )
                }
        }

    /**
     * Generate invalid tests for header parameters using providers.
     */
    private fun generateHeaderInvalidTests(
        headerParams: List<SwaggerParameter>,
        baseBody: Map<String, Any?>,
        baseHeaders: Map<String, String>,
    ): List<AutoTestCase> =
        headerParams.flatMap { param ->
            val schema = param.schema ?: return@flatMap emptyList()
            val resolvedSchema = resolveSchema(schema)

            sortedInvalidProviders()
                .filter { it.canHandle(resolvedSchema) }
                .filterNot { it.testType == "required" }
                .flatMap { provider ->
                    provider.generateTestCases(
                        InvalidTestRequest(
                            fieldName = param.name,
                            fieldPath = listOf(param.name),
                            schema = resolvedSchema,
                            location = ParameterLocation.HEADER,
                            baseBody = baseBody,
                            baseHeaders = baseHeaders,
                        ),
                    )
                }
        }

    /**
     * Generate security tests for header parameters using providers.
     */
    private fun generateHeaderSecurityTests(
        headerParams: List<SwaggerParameter>,
        baseBody: Map<String, Any?>,
        baseHeaders: Map<String, String>,
    ): List<AutoTestCase> =
        headerParams.flatMap { param ->
            sortedSecurityProviders()
                .filter { ParameterLocation.HEADER in it.applicableLocations() }
                .flatMap { provider ->
                    provider.generateTestCases(
                        SecurityTestRequest(
                            fieldName = param.name,
                            location = ParameterLocation.HEADER,
                            baseBody = baseBody,
                            baseHeaders = baseHeaders,
                        ),
                    )
                }
        }

    private fun sortedInvalidProviders() = registry.getInvalidTestProviders().sortedBy { it.testType }

    private fun sortedSecurityProviders() = registry.getSecurityTestProviders().sortedBy { it.testType }
}
