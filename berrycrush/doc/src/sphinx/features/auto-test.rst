Auto-Generated Tests
====================

BerryCrush can automatically generate invalid request and security tests based on your OpenAPI schema constraints.
This feature helps ensure your API properly validates input and rejects common attack patterns.

Overview
--------

The ``auto:`` directive in API calls generates multiple test variations that:

* Violate OpenAPI schema constraints (invalid tests)
* Include common attack payloads (security tests)

Each generated test appears as a separate test in your test reports, making it easy to identify which validations
your API handles correctly.

Basic Syntax
------------

Add the ``auto:`` directive to any API call:

.. code-block:: text

    call ^operationId
      auto: [<test-types>]
      <base-parameters>

Where ``<test-types>`` is a space-separated list of:

* ``invalid`` - Generate tests that violate OpenAPI schema constraints
* ``security`` - Generate tests with common attack payloads
* ``multi`` - Generate idempotency tests with sequential and concurrent requests

You can use one or more types:

.. code-block:: text

    auto: [invalid]          # Only invalid tests
    auto: [security]         # Only security tests
    auto: [multi]            # Only multi/idempotency tests
    auto: [invalid security] # Both invalid and security
    auto: [invalid security multi] # All three types

Example
-------

.. code-block:: text

    scenario: Auto-generated tests for createPet
      when: I create a pet with invalid input
        call ^createPet
          auto: [invalid security]
          body:
            name: "TestPet"
            status: "available"
      
      if status 4xx
        # Test passed - invalid request rejected
      else
        fail "Expected 4xx for {{test.type}}: {{test.description}}"

This generates tests for each field in the request body based on the OpenAPI schema constraints.

Invalid Tests
-------------

Invalid tests are generated based on OpenAPI schema properties:

======================== =================================================
Schema Property          Generated Test
======================== =================================================
``minLength``            String shorter than minimum
``maxLength``            String longer than maximum
``minimum``              Number below minimum value
``maximum``              Number above maximum value
``pattern``              String that violates the regex pattern
``format: email``        Invalid email (e.g., "not-an-email")
``format: uuid``         Invalid UUID (e.g., "not-a-uuid")
``format: date``         Invalid date format
``format: date-time``    Invalid date-time format
``required``             Missing required fields
``enum``                 Value not in allowed list
``type``                 Wrong type (e.g., string instead of number)
======================== =================================================

Security Tests
--------------

Security tests inject common attack payloads to verify your API properly sanitizes input:

SQL Injection
^^^^^^^^^^^^^

.. code-block:: text

    ' OR '1'='1
    "; DROP TABLE users; --
    ' UNION SELECT * FROM users --

Cross-Site Scripting (XSS)
^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: text

    <script>alert('XSS')</script>
    javascript:alert(1)
    <img src=x onerror=alert(1)>

Path Traversal
^^^^^^^^^^^^^^

.. code-block:: text

    ../../etc/passwd
    ....//....//etc/passwd
    ..%2F..%2Fetc%2Fpasswd

Command Injection
^^^^^^^^^^^^^^^^^

.. code-block:: text

    ; ls -la
    $(whoami)
    `id`
    | cat /etc/passwd

LDAP Injection
^^^^^^^^^^^^^^

.. code-block:: text

    *)(uid=*))(|(uid=*
    admin)(&)

Multi Tests (Idempotency)
-------------------------

Multi tests verify API idempotency by executing the same request multiple times and 
checking that responses are consistent. This is crucial for:

* **Idempotent operations** - Ensuring GET, PUT, DELETE return the same result when repeated
* **Race condition detection** - Identifying concurrency issues
* **State consistency** - Verifying the API maintains consistent state under load

Execution Modes
^^^^^^^^^^^^^^^

Multi tests execute in two modes:

===================== ============================================================
Mode                  Description
===================== ============================================================
``SEQUENTIAL``        Requests executed one after another (default: 3 requests)
``CONCURRENT``        Requests executed simultaneously (default: 5 requests)
===================== ============================================================

Basic Example
^^^^^^^^^^^^^

.. code-block:: text

    scenario: Idempotency test for getPet
      when: I get a pet multiple times
        call ^getPetById
          auto: [multi]
          petId: 1
      
      if status 2xx
        # Multi-test results are automatically verified
      else
        fail "Expected 2xx status"

Configuring Request Counts
^^^^^^^^^^^^^^^^^^^^^^^^^^

Override default counts using the parameters block at the file level:

.. code-block:: text

    parameters:
      multiTestSequentialCount: 5    # Run 5 sequential requests (default: 3)
      multiTestConcurrentCount: 10   # Run 10 concurrent requests (default: 5)

    scenario: Heavy idempotency test
      when: I stress test getPet
        call ^getPetById
          auto: [multi]
          petId: 1

At the feature level:

.. code-block:: text

    feature: Idempotency Tests
      parameters:
        multiTestSequentialCount: 10
        multiTestConcurrentCount: 20
      
      scenario: Custom count test
        when: I test idempotency
          call ^getPetById
            auto: [multi]
            petId: 1

Or at the step level (in the call directive):

.. code-block:: text

    when: I stress test with custom counts
      call ^getPetById
        auto: [multi]
        petId: 1
        multiTestSequentialCount: 5
        multiTestConcurrentCount: 10

Step-level parameters override file-level and feature-level parameters.

Multi Test Context Variables
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

During multi-test execution, these additional variables are available:

======================= ================================================ ========================
Variable                Description                                      Example Values
======================= ================================================ ========================
``multiTest.mode``      Execution mode                                   ``"SEQUENTIAL"``, ``"CONCURRENT"``
``multiTest.count``     Number of requests in current mode               ``3``, ``5``
``multiTest.passed``    Whether all requests passed                      ``true``, ``false``
``multiTest.duration``  Total execution time in milliseconds             ``150``
======================= ================================================ ========================

Multi Test Display Names
^^^^^^^^^^^^^^^^^^^^^^^^

Multi-tests appear in test reports with descriptive names:

.. code-block:: text

    [multi:sequential] 3 requests
    [multi:concurrent] 5 requests

With custom counts:

.. code-block:: text

    [multi:sequential] 10 requests
    [multi:concurrent] 20 requests

Combining with Other Test Types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Multi tests can be combined with invalid and security tests:

.. code-block:: text

    scenario: Comprehensive API test
      when: I test the API thoroughly
        call ^createPet
          auto: [invalid security multi]
          body:
            name: "TestPet"
            status: "available"
      
      if test.type equals invalid and status 4xx
        # Invalid request rejected
      else if test.type equals security and status 4xx
        # Security attack blocked
      else if test.type equals multi and status 2xx
        # Multi-test passed
      else
        fail "Test failed: {{test.type}} - {{test.description}}"

Parameter Locations
-------------------

Auto-tests are generated for parameters in different locations:

================== ====================================== ========================
Location           Description                            Display Name
================== ====================================== ========================
Request body       JSON body fields                       ``request body``
Path parameter     URL path variables                     ``path variable``
Query parameter    Query string parameters                ``query parameter``
Header             HTTP headers                           ``header``
================== ====================================== ========================

Path Parameter Example
^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: text

    scenario: Auto-generated tests for getPetById
      when: I get a pet with invalid ID
        call ^getPetById
          auto: [invalid security]
          petId: 1
      
      if status 4xx
        # Invalid ID rejected - test passed

Context Variables
-----------------

During auto-test execution, these variables are available for use in assertions:

================== ================================================ ========================
Variable           Description                                      Example Values
================== ================================================ ========================
``test.type``      Test category                                    ``"invalid"``, ``"security"``
``test.field``     Field being tested                               ``"name"``, ``"petId"``
``test.description`` Human-readable test description                ``"SQL Injection"``, ``"minLength violation"``
``test.value``     The invalid/attack value used                    ``"' OR '1'='1"``
``test.location``  Parameter location                               ``"request body"``, ``"path variable"``
================== ================================================ ========================

Using Context Variables in Assertions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: text

    scenario: Auto-generated security tests
      when: I create a pet with attack payloads
        call ^createPet
          auto: [security]
          body:
            name: "TestPet"
      
      if status 4xx and test.type equals security
        # Security attack blocked - expected
      else if status 2xx
        fail "Security vulnerability: {{test.description}} not blocked for field {{test.field}}"

Test Display Names
------------------

Auto-tests appear in test reports with descriptive names:

.. code-block:: text

    [Invalid request - minLength] request body name with value <empty string>
    [Invalid request - enum] request body status with value INVALID_ENUM_VALUE
    [Invalid request - type] path variable petId with value not-a-number
    [security - SQL Injection] request body name with value ' OR '1'='1
    [security - XSS] request body name with value <script>alert('XSS')</script>
    [security - Path Traversal] path variable petId with value ../../etc/passwd
    [multi:sequential] 3 requests
    [multi:concurrent] 5 requests

This format allows you to:

* Quickly identify which tests failed
* Understand what type of validation is missing
* Locate the affected field and value

Excluding Test Types
--------------------

Use the ``excludes:`` directive to skip specific test types:

.. code-block:: text

    call ^createPet
      auto: [invalid security]
      excludes: [SQLInjection maxLength]
      body:
        name: "TestPet"
        status: "available"

This generates all tests except SQL Injection and maxLength violation tests.

Available Test Types to Exclude
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Invalid Tests:**

* ``minLength`` - String too short
* ``maxLength`` - String too long
* ``pattern`` - Pattern violation
* ``format`` - Format violation (email, uuid, etc.)
* ``enum`` - Invalid enum value
* ``minimum`` - Number below minimum
* ``maximum`` - Number above maximum
* ``type`` - Wrong type
* ``required`` - Missing required field
* ``minItems`` - Array too small
* ``maxItems`` - Array too large

**Security Tests:**

* ``SQLInjection`` - SQL injection payloads
* ``XSS`` - Cross-site scripting payloads
* ``PathTraversal`` - Path traversal attacks
* ``CommandInjection`` - Command injection payloads
* ``LDAPInjection`` - LDAP injection payloads
* ``XXE`` - XML External Entity attacks
* ``HeaderInjection`` - HTTP header injection

Custom Providers
----------------

BerryCrush supports custom test providers for extending auto-tests with your own
invalid value generators and security payloads. This uses Java's ServiceLoader pattern
for automatic discovery.

Creating a Custom Invalid Test Provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Implement the ``InvalidTestProvider`` interface:

**Java Example:**

.. code-block:: java

    package com.example;

    import org.berrycrush.berrycrush.autotest.provider.InvalidTestProvider;
    import org.berrycrush.berrycrush.autotest.provider.InvalidTestValue;
    import io.swagger.v3.oas.models.media.Schema;
    import java.util.List;

    public class EmojiTestProvider implements InvalidTestProvider {
        
        @Override
        public String getTestType() {
            return "emoji";
        }
        
        @Override
        public int getPriority() {
            return 100; // Higher than built-in providers (0)
        }

        @Override
        public boolean canHandle(Schema<?> schema) {
            return "string".equals(schema.getType());
        }

        @Override
        public List<InvalidTestValue> generateInvalidValues(
                String fieldName, Schema<?> schema) {
            return List.of(
                new InvalidTestValue(
                    "Test 🎉 emoji 🐱 string",
                    "String with emoji characters"
                )
            );
        }
    }

**Kotlin Example:**

.. code-block:: kotlin

    class EmojiTestProvider : InvalidTestProvider {
        override val testType: String = "emoji"
        override val priority: Int = 100

        override fun canHandle(schema: Schema<*>): Boolean =
            schema.type == "string"

        override fun generateInvalidValues(
            fieldName: String,
            schema: Schema<*>,
        ): List<InvalidTestValue> = listOf(
            InvalidTestValue(
                value = "Test 🎉 emoji 🐱 string",
                description = "String with emoji characters",
            )
        )
    }

Creating a Custom Security Test Provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Implement the ``SecurityTestProvider`` interface:

**Kotlin Example:**

.. code-block:: kotlin

    class NoSqlInjectionProvider : SecurityTestProvider {
        override val testType: String = "NoSQLInjection"
        override val displayName: String = "NoSQL Injection"
        override val priority: Int = 100

        override fun applicableLocations(): Set<ParameterLocation> =
            setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

        override fun generatePayloads(): List<SecurityPayload> = listOf(
            SecurityPayload("MongoDB \$ne", "{\"\$ne\": null}"),
            SecurityPayload("MongoDB \$where", "{\"\$where\": \"sleep(5000)\"}"),
        )
    }

Registering Custom Providers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Create ServiceLoader configuration files in your project:

**META-INF/services/org.berrycrush.berrycrush.autotest.provider.InvalidTestProvider:**

.. code-block:: text

    com.example.EmojiTestProvider

**META-INF/services/org.berrycrush.berrycrush.autotest.provider.SecurityTestProvider:**

.. code-block:: text

    com.example.NoSqlInjectionProvider

Custom providers are automatically discovered at runtime and can:

* Add new test types alongside built-in ones
* Override built-in providers (use same ``testType`` with higher ``priority``)
* Use any JVM language (Java, Kotlin, Scala, etc.)

Provider Properties
^^^^^^^^^^^^^^^^^^^

================== ============================================================
Property           Description
================== ============================================================
``testType``       Unique identifier (used for ``excludes`` and deduplication)
``displayName``    Human-readable name for test reports (security providers only)
``priority``       Override order (higher wins); built-in = 0, custom = 100
================== ============================================================

Dependencies
^^^^^^^^^^^^

Custom providers need the OpenAPI parser for schema inspection:

**Gradle:**

.. code-block:: kotlin

    testImplementation("io.swagger.parser.v3:swagger-parser:2.1.39")

Best Practices
--------------

1. **Provide valid base parameters**
   
   Auto-tests modify one parameter at a time while keeping others valid:

   .. code-block:: text

       call ^createPet
         auto: [invalid security]
         body:
           name: "ValidName"      # This is the base value
           status: "available"    # This is also a base value

2. **Use conditional assertions**
   
   Handle different test types appropriately:

   .. code-block:: text

       if status 4xx and test.type equals invalid
         # Invalid input correctly rejected
       else if status 4xx and test.type equals security
         # Security attack blocked
       else
         fail "{{test.type}} test should return 4xx: {{test.description}}"

3. **Expect 4xx responses**
   
   Both invalid and security tests should be rejected by a secure, well-validated API.

4. **Review generated tests**
   
   The number of tests depends on schema constraints. Complex schemas with many constraints
   generate more tests. Run tests with logging enabled to see what's being generated.

5. **Combine with regular tests**
   
   Auto-tests supplement but don't replace targeted functional tests:

   .. code-block:: text

       # Regular functional test
       scenario: Create pet successfully
         when: I create a pet
           call ^createPet
             body:
               name: "Fluffy"
               status: "available"
         then: pet is created
           assert status 201
       
       # Auto-generated validation tests
       scenario: Auto-tests for createPet validation
         when: I create a pet with invalid data
           call ^createPet
             auto: [invalid security]
             body:
               name: "Fluffy"
               status: "available"
         if status 4xx
           # Test passed

Integration with JUnit
----------------------

Auto-tests integrate seamlessly with JUnit. Each generated test case appears as a separate
test in the JUnit report:

.. code-block:: text

    PetStoreTest
    ├── 01-create-pet.scenario
    │   └── Create pet successfully
    └── 98-auto-tests.scenario
        └── Auto-tests for createPet validation
            ├── [Invalid request] request body name with value <empty string>
            ├── [Invalid request] request body name with value <too long>
            ├── [Security SQL Injection] request body name with value ' OR '1'='1
            └── ... (more tests)

Limitations
-----------

* Auto-tests are generated at runtime, not during JUnit discovery
* Tests are generated only for parameters with OpenAPI schema constraints
* Nested object properties are tested but deeply nested structures may generate many tests
* Custom validation rules not expressed in the OpenAPI schema are not tested

See Also
--------

* :doc:`scenario-syntax` - Complete scenario syntax reference
* :doc:`parameters` - Configuration options
* :doc:`reporting` - Test report formats
