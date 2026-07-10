package org.berrycrush.autotest.provider

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.openapi.OpenApiLoader
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InvalidTestProviderTest {
    private val loader = OpenApiLoader()

    private val schema30Path: String =
        javaClass.getResource("/schema-test-30.yaml")?.path
            ?: error("schema-test-30.yaml not found in test resources")

    private val schema31Path: String =
        javaClass.getResource("/schema-test-31.yaml")?.path
            ?: error("schema-test-31.yaml not found in test resources")

    private val openApi30: OpenAPI by lazy { loader.load(schema30Path) }
    private val openApi31: OpenAPI by lazy { loader.load(schema31Path) }

    @Test
    fun `default invalid providers should include all built-in types`() {
        val expectedTypes =
            setOf(
                "minLength",
                "maxLength",
                "pattern",
                "format",
                "enum",
                "minimum",
                "exclusiveMinimum",
                "maximum",
                "exclusiveMaximum",
                "multipleOf",
                "const",
                "type",
                "required",
                "minItems",
                "maxItems",
                "uniqueItems",
                "minProperties",
                "maxProperties",
            )

        val actualTypes = DefaultInvalidTestProviders.all.map { it.testType }

        assertEquals(expectedTypes, actualTypes.toSet())
        assertEquals(actualTypes.size, actualTypes.distinct().size, "Provider testType values should be unique")
    }

    @Test
    fun `minLength provider should generate shorter values for both specs`() {
        val provider = MinLengthProvider()
        withSchemas("MinLengthSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as String).length < 5 })
            }
        }
    }

    @Test
    fun `maxLength provider should generate longer values for both specs`() {
        val provider = MaxLengthProvider()
        withSchemas("MaxLengthSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as String).length > 10 })
            }
        }
    }

    @Test
    fun `pattern provider should generate non-matching values for both specs`() {
        val provider = PatternProvider()
        withSchemas("PatternSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { it.invalidValue == "!!!invalid_pattern!!!" })
            }
        }
    }

    @Test
    fun `format provider should generate invalid format value for both specs`() {
        val provider = FormatProvider()
        withSchemas("FormatSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { it.invalidValue == "not-an-email" })
            }
        }
    }

    @Test
    fun `enum provider should generate value outside enum for both specs`() {
        val provider = EnumProvider()
        withSchemas("EnumSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                val enumValues =
                    schema.enum
                        ?.map { it?.toString() }
                        .orEmpty()
                        .toSet()
                assertTrue(cases.any { (it.invalidValue as String) !in enumValues })
            }
        }
    }

    @Test
    fun `minimum provider should generate below minimum for both specs`() {
        val provider = MinimumProvider()
        withSchemas("MinimumSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as BigDecimal) < BigDecimal.ONE })
            }
        }
    }

    @Test
    fun `exclusiveMinimum provider should generate boundary value for both specs`() {
        val provider = ExclusiveMinimumProvider()
        withSchemas("ExclusiveMinimumSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as BigDecimal).compareTo(BigDecimal.ONE) == 0 })
            }
        }
    }

    @Test
    fun `maximum provider should generate above maximum for both specs`() {
        val provider = MaximumProvider()
        withSchemas("MaximumSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as BigDecimal) > BigDecimal.TEN })
            }
        }
    }

    @Test
    fun `exclusiveMaximum provider should generate boundary value for both specs`() {
        val provider = ExclusiveMaximumProvider()
        withSchemas("ExclusiveMaximumSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as BigDecimal).compareTo(BigDecimal.TEN) == 0 })
            }
        }
    }

    @Test
    fun `multipleOf provider should generate non-divisible value for both specs`() {
        val provider = MultipleOfProvider()
        withSchemas("MultipleOfSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                val divisor = schema.multipleOf ?: error("multipleOf must exist for MultipleOfSchema")
                assertTrue(
                    cases.any {
                        (it.invalidValue as BigDecimal).remainder(divisor).compareTo(BigDecimal.ZERO) != 0
                    },
                )
            }
        }
    }

    @Test
    fun `const provider should generate value different from constant for OpenAPI 3_1`() {
        val provider = ConstProvider()
        val schema31 = getSchema(openApi31, "ConstSchema")
        assertNotNull(schema31)

        checkProvider(provider, schema31) { cases ->
            assertTrue(cases.all { it.invalidValue != "fixedValue" })
        }
    }

    @Test
    fun `type provider should generate type mismatch for integer and boolean properties`() {
        val provider = TypeProvider()

        withPropertySchemas("ObjectSchema", "age") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { it.invalidValue == "not-a-integer" })
            }
        }

        withPropertySchemas("ObjectSchema", "married") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { it.invalidValue == "not-a-boolean" })
            }
        }
    }

    @Test
    fun `required provider should remove required field from body for both specs`() {
        val provider = RequiredProvider()
        withSchemas("ObjectSchema") { schema ->
            val baseBody = mapOf("name" to "Berry", "age" to 20, "married" to false)
            val request =
                InvalidTestRequest(
                    fieldName = "name",
                    fieldPath = listOf("name"),
                    schema = schema,
                    location = ParameterLocation.BODY,
                    baseBody = baseBody,
                )

            val cases = provider.generateTestCases(request)
            assertTrue(cases.isNotEmpty())
            assertTrue(cases.all { it.invalidValue == null })
            assertTrue(cases.all { !it.body.containsKey("name") })
        }
    }

    @Test
    fun `minItems provider should generate smaller arrays for both specs`() {
        val provider = MinItemsProvider()
        withSchemas("ArraySchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as List<*>).isEmpty() })
            }
        }
    }

    @Test
    fun `maxItems provider should generate larger arrays for both specs`() {
        val provider = MaxItemsProvider()
        withSchemas("ArraySchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as List<*>).size > 3 })
            }
        }
    }

    @Test
    fun `uniqueItems provider should generate duplicates for both specs`() {
        val provider = UniqueItemsProvider()
        withSchemas("UniqueItemsSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as List<*>).distinct().size < (it.invalidValue as List<*>).size })
            }
        }
    }

    @Test
    fun `minProperties provider should generate object with too few properties for both specs`() {
        val provider = MinPropertiesProvider()
        withSchemas("MinPropertiesSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as Map<*, *>).size < 2 })
            }
        }
    }

    @Test
    fun `maxProperties provider should generate object with too many properties for both specs`() {
        val provider = MaxPropertiesProvider()
        withSchemas("MaxPropertiesSchema") { schema ->
            checkProvider(provider, schema) { cases ->
                assertTrue(cases.any { (it.invalidValue as Map<*, *>).size > 2 })
            }
        }
    }

    @Test
    fun `null-capable schemas are currently documented as deferred for dedicated invalid provider behavior`() {
        val schema30 = getSchema(openApi30, "NullableStringSchema")
        val schema31 = getSchema(openApi31, "NullableStringSchema")
        assertNotNull(schema30)
        assertNotNull(schema31)

        val matched30 = DefaultInvalidTestProviders.all.filter { it.canHandle(schema30) }
        val matched31 = DefaultInvalidTestProviders.all.filter { it.canHandle(schema31) }

        assertTrue(matched30.isNotEmpty(), "type provider should match nullable-only schema in OAS 3.0")
        assertTrue(matched31.isNotEmpty(), "type provider should match nullable-only schema in OAS 3.1")
    }

    private fun checkProvider(
        provider: InvalidTestProvider,
        schema: Schema<*>,
        fieldName: String = "target",
        fieldPath: List<String> = listOf(fieldName),
        baseBody: Map<String, Any?> = mapOf(fieldName to "valid"),
        assertion: (List<org.berrycrush.autotest.AutoTestCase>) -> Unit,
    ) {
        assertTrue(provider.canHandle(schema), "Provider ${provider.testType} should handle schema")
        val cases =
            provider.generateTestCases(
                InvalidTestRequest(
                    fieldName = fieldName,
                    fieldPath = fieldPath,
                    schema = schema,
                    location = ParameterLocation.BODY,
                    baseBody = baseBody,
                ),
            )
        assertTrue(cases.isNotEmpty(), "Provider ${provider.testType} should generate at least one test case")
        assertTrue(cases.all { it.testType == provider.testType })
        assertion(cases)
    }

    private fun withSchemas(
        schemaName: String,
        block: (Schema<*>) -> Unit,
    ) {
        listOf(openApi30, openApi31).forEach { openApi ->
            val schema = getSchema(openApi, schemaName)
            assertNotNull(schema, "Schema '$schemaName' should exist in fixture ${openApi.info?.title}")
            block(schema)
        }
    }

    private fun withPropertySchemas(
        schemaName: String,
        propertyName: String,
        block: (Schema<*>) -> Unit,
    ) {
        listOf(openApi30, openApi31).forEach { openApi ->
            val schema = getPropertySchema(openApi, schemaName, propertyName)
            assertNotNull(schema, "Property '$propertyName' should exist in $schemaName for ${openApi.info?.title}")
            block(schema)
        }
    }

    private fun getSchema(
        openApi: OpenAPI,
        schemaName: String,
    ): Schema<*>? = openApi.components?.schemas?.get(schemaName)

    private fun getPropertySchema(
        openApi: OpenAPI,
        schemaName: String,
        propertyName: String,
    ): Schema<*>? {
        val rootSchema = getSchema(openApi, schemaName) ?: return null
        val properties = rootSchema.properties ?: return null
        return properties[propertyName]
    }
}
