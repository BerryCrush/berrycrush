OpenAPI 3.1 Support
===================

BerryCrush provides full support for OpenAPI 3.1 specifications, including all new schema validation features and webhook testing capabilities.

Schema Validation Features
--------------------------

Array Types (Nullable Fields)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

OpenAPI 3.1 allows expressing nullable types using arrays:

.. code-block:: yaml

    # OpenAPI 3.1
    type:
      - string
      - "null"

BerryCrush automatically validates these nullable types correctly.

Const Validation
~~~~~~~~~~~~~~~~

Validate fields against exact values:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    properties:
      version:
        const: "v1.0"

Tuple Validation with prefixItems
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Validate arrays as tuples with different types per position:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    type: array
    prefixItems:
      - type: string   # First element must be string
      - type: integer  # Second element must be integer
      - type: boolean  # Third element must be boolean

Conditional Schema Validation (if/then/else)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Apply different validation rules based on conditions:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    properties:
      type:
        type: string
    if:
      properties:
        type:
          const: "person"
    then:
      properties:
        name:
          type: string
    else:
      properties:
        productId:
          type: integer

Dependent Schemas
~~~~~~~~~~~~~~~~~

Validate additional properties when certain fields are present:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    dependentRequired:
      creditCard:
        - billingAddress
        - securityCode

    dependentSchemas:
      creditCard:
        properties:
          billingAddress:
            type: string

Composition Keywords
~~~~~~~~~~~~~~~~~~~~

Full support for allOf, anyOf, oneOf, and not:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    oneOf:
      - type: string
      - type: integer
      - type: "null"

$ref with Siblings
~~~~~~~~~~~~~~~~~~

OpenAPI 3.1 allows $ref to coexist with other keywords:

.. code-block:: yaml

    # OpenAPI 3.1 schema
    $ref: "#/components/schemas/Pet"
    description: "Extended pet with optional fields"
    nullable: true

Webhook Testing
---------------

BerryCrush provides a ``MockWebhookServer`` for testing webhook deliveries in your scenarios.

Setting Up the Webhook Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: kotlin

    import org.berrycrush.webhook.MockWebhookServer
    import org.berrycrush.webhook.WebhookAssertions

    val webhookServer = MockWebhookServer()
    webhookServer.expect("onPetAdopted")
    webhookServer.start()

    // Your test code that triggers webhooks...

    // Verify webhooks
    val assertions = WebhookAssertions(webhookServer)
    assertions
        .assertReceived("onPetAdopted")
        .assertReceivedCount("onPetAdopted", 1)
        .assertBodyContains("$.petId", 123)
        .assertContentType("onPetAdopted", "application/json")

    webhookServer.stop()

Webhook Server API
~~~~~~~~~~~~~~~~~~

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Method
     - Description
   * - ``expect(operationId)``
     - Register a webhook endpoint to listen for
   * - ``registerFromSpec(spec)``
     - Register all webhooks from an OpenAPI spec
   * - ``start(): Int``
     - Start the server, returns the port
   * - ``stop()``
     - Stop the server
   * - ``getWebhookUrl(operationId)``
     - Get the URL for a webhook endpoint
   * - ``getReceived(operationId)``
     - Get all received calls for a webhook
   * - ``wasReceived(operationId)``
     - Check if a webhook was called
   * - ``getReceivedCount(operationId)``
     - Get call count for a webhook
   * - ``verify(operationId)``
     - Verify against expectations
   * - ``clearReceived()``
     - Clear received calls
   * - ``reset()``
     - Clear all expectations and calls

Webhook Assertions API
~~~~~~~~~~~~~~~~~~~~~~

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Method
     - Description
   * - ``assertReceived(operationId)``
     - Assert webhook was received
   * - ``assertNotReceived(operationId)``
     - Assert webhook was NOT received
   * - ``assertReceivedCount(operationId, count)``
     - Assert exact call count
   * - ``assertBodyContains(jsonPath, value)``
     - Assert body contains value at JSON path
   * - ``assertBodyEquals(operationId, body)``
     - Assert body matches exactly
   * - ``assertContentType(operationId, type)``
     - Assert content type
   * - ``assertHeader(operationId, name, value)``
     - Assert header value
   * - ``getCalls(operationId)``
     - Get all calls for custom assertions
   * - ``getLastCall(operationId)``
     - Get most recent call

Example: Complete Webhook Test
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: kotlin

    @Test
    fun `webhook is delivered when pet is adopted`() {
        // Setup webhook server
        val webhookServer = MockWebhookServer()
        webhookServer.expect("onPetAdopted")
        val port = webhookServer.start()

        // Configure your application to send webhooks to the mock server
        val webhookUrl = webhookServer.getWebhookUrl("onPetAdopted")
        applicationConfig.webhookEndpoint = webhookUrl

        // Trigger the action that sends a webhook
        petService.adoptPet(petId = 123, adopterId = 456)

        // Verify the webhook was delivered correctly
        WebhookAssertions(webhookServer)
            .assertReceived("onPetAdopted")
            .assertBodyContains("$.petId", 123)
            .assertBodyContains("$.adopterId", 456)
            .assertHeader("onPetAdopted", "Content-Type", "application/json")

        webhookServer.stop()
    }

Version Detection
-----------------

BerryCrush automatically detects the OpenAPI version and applies the appropriate validation rules:

.. code-block:: kotlin

    val spec = OpenApiLoader.load("petstore.yaml")
    when (spec.version) {
        OpenApiVersion.V3_0_X -> println("Using OpenAPI 3.0.x rules")
        OpenApiVersion.V3_1_X -> println("Using OpenAPI 3.1.x rules")
    }

Migration from OpenAPI 3.0
--------------------------

When migrating your OpenAPI specifications from 3.0 to 3.1, BerryCrush handles:

1. **Nullable fields**: Both ``nullable: true`` (3.0) and ``type: [string, "null"]`` (3.1) are supported
2. **$ref behavior**: Both standalone (3.0) and with siblings (3.1) are validated
3. **Type changes**: ``exclusiveMinimum``/``exclusiveMaximum`` as both boolean (3.0) and number (3.1)

No changes to your scenarios are required when upgrading your specs.
