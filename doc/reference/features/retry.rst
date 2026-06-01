Retry
=====

BerryCrush provides built-in retry support for handling transient failures in HTTP
requests. The retry mechanism automatically retries failed requests based on
configurable conditions.

Overview
--------

When enabled, the retry mechanism:

- Automatically retries requests that fail with retryable status codes
- Handles transient exceptions like connection timeouts
- Supports multiple backoff strategies (fixed, linear, exponential)
- Adds jitter to prevent thundering herd problems
- Provides detailed exception information when all retries are exhausted

Configuration
-------------

Programmatic Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^

Configure retry behavior using the ``retry { }`` DSL block in your test
configuration:

.. code-block:: kotlin

   @BerryCrushConfiguration
   class PetApiTest {
       companion object {
           @JvmStatic
           @BeforeAll
           fun setup() {
               BerryCrush.configure {
                   spec("petstore.yaml")
                   retry {
                       maxAttempts = 3
                       delay = Duration.ofSeconds(1)
                       maxDelay = Duration.ofSeconds(30)
                       backoff = BackoffStrategy.EXPONENTIAL
                       jitter = true
                       retryOnStatusCodes = setOf(429, 502, 503, 504)
                   }
               }
           }
       }
   }

Java equivalent:

.. code-block:: java

   @BerryCrushConfiguration
   public class PetApiTest {
       @BeforeAll
       static void setup() {
           BerryCrush.configure(config -> {
               config.spec("petstore.yaml");
               config.retry(retry -> {
                   retry.setMaxAttempts(3);
                   retry.setDelay(Duration.ofSeconds(1));
                   retry.setMaxDelay(Duration.ofSeconds(30));
                   retry.setBackoff(BackoffStrategy.EXPONENTIAL);
                   retry.setJitter(true);
                   retry.setRetryOnStatusCodes(Set.of(429, 502, 503, 504));
               });
           });
       }
   }

Parameter-Based Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can also configure retry via file-level or scenario-level parameters.

**File-level parameters** (apply to all scenarios in the file):

.. code-block:: berrycrush

   parameters:
     retry.maxAttempts: 3
     retry.delay: "1s"
     retry.maxDelay: "30s"
     retry.backoff: exponential
     retry.jitter: true

   scenario: Pet API with retry
     when: I request pets
       call ^listPets
     then: I receive a response
       assert status 200

**Nested syntax** (alternative, more readable format):

.. code-block:: berrycrush

   parameters:
     retry:
       maxAttempts: 3
       delay: "1s"
       maxDelay: "30s"
       backoff: exponential
       jitter: true

**Scenario-level parameters** (apply to a specific scenario):

.. code-block:: berrycrush

   scenario: Flaky endpoint with retry
     parameters:
       retry:
         maxAttempts: 5
         delay: "500ms"
         backoff: exponential
     when: I call flaky endpoint
       call ^flakyOperation
     then: eventually succeeds
       assert status 200

Configuration Options
---------------------

.. list-table::
   :header-rows: 1
   :widths: 25 20 55

   * - Option
     - Default
     - Description
   * - ``maxAttempts``
     - 0 (disabled)
     - Maximum retry attempts. Set to 0 to disable retries.
   * - ``delay``
     - 1 second
     - Initial delay between retry attempts.
   * - ``maxDelay``
     - 30 seconds
     - Maximum delay cap (for exponential/linear backoff).
   * - ``backoff``
     - EXPONENTIAL
     - Backoff strategy: FIXED, LINEAR, or EXPONENTIAL.
   * - ``jitter``
     - true
     - Add ±20% randomization to delay (prevents thundering herd).
   * - ``retryOnStatusCodes``
     - 429, 502, 503, 504
     - HTTP status codes that trigger retry.
   * - ``retryOnExceptions``
     - See below
     - Exception types that trigger retry.

Backoff Strategies
------------------

BerryCrush supports three backoff strategies:

FIXED
^^^^^

Same delay for every retry attempt.

.. code-block:: text

   Attempt 1: wait 1s
   Attempt 2: wait 1s
   Attempt 3: wait 1s

LINEAR
^^^^^^

Delay increases linearly with each attempt.

.. code-block:: text

   Attempt 1: wait 1s  (1s × 1)
   Attempt 2: wait 2s  (1s × 2)
   Attempt 3: wait 3s  (1s × 3)

EXPONENTIAL
^^^^^^^^^^^

Delay doubles with each attempt (recommended for production).

.. code-block:: text

   Attempt 1: wait 1s  (1s × 2^0)
   Attempt 2: wait 2s  (1s × 2^1)
   Attempt 3: wait 4s  (1s × 2^2)

Default Retry Conditions
------------------------

Status Codes
^^^^^^^^^^^^

By default, the following HTTP status codes trigger retry:

- **429 Too Many Requests** - Rate limiting
- **502 Bad Gateway** - Server error
- **503 Service Unavailable** - Server temporarily unavailable
- **504 Gateway Timeout** - Server timeout

Exceptions
^^^^^^^^^^

The following exception types trigger retry by default:

- ``java.net.SocketTimeoutException``
- ``java.net.ConnectException``
- ``java.net.SocketException``

Custom Retry Conditions
-----------------------

Configure custom status codes:

.. code-block:: kotlin

   retry {
       retryOnStatusCodes = setOf(429, 500, 502, 503, 504)
   }

Configure custom exception types:

.. code-block:: kotlin

   retry {
       retryOnExceptions = setOf(
           SocketTimeoutException::class,
           ConnectException::class,
           SSLException::class,
       )
   }

Error Handling
--------------

When all retry attempts are exhausted, a ``RetryExhaustedException`` is thrown
containing:

- ``attempts`` - Total number of attempts made
- ``lastResponse`` - The last HTTP response (if status code failure)
- ``lastException`` - The last exception (if exception failure)

Example handling:

.. code-block:: kotlin

   try {
       runScenario("my-scenario")
   } catch (e: RetryExhaustedException) {
       println("Failed after ${e.attempts} attempts")
       e.lastResponse?.let { println("Last status: ${it.statusCode()}") }
       e.lastException?.let { println("Last error: ${it.message}") }
   }

Jitter
------

When ``jitter`` is enabled (default), the retry mechanism adds ±20% randomization
to the calculated delay. This prevents the "thundering herd" problem where many
clients retry simultaneously after a server recovers.

For example, with exponential backoff and a 4-second calculated delay:

.. code-block:: text

   Without jitter: wait exactly 4.0s
   With jitter:    wait 3.2s - 4.8s (random within ±20%)

Best Practices
--------------

1. **Enable for external APIs**: Always enable retry for third-party APIs that
   may experience transient failures.

2. **Use exponential backoff**: Preferred for production environments to avoid
   overwhelming recovering servers.

3. **Keep jitter enabled**: Prevents synchronized retry storms.

4. **Set reasonable maxDelay**: Cap the maximum delay to prevent excessively long
   waits (default 30s is usually appropriate).

5. **Include 429 status code**: Rate limiting is the most common retryable error.

6. **Don't retry on 4xx errors**: Client errors (except 429) typically indicate
   bugs in the request, not transient issues.

Example: Full Configuration
---------------------------

.. code-block:: kotlin

   BerryCrush.configure {
       spec("api.yaml")
       baseUrl("https://api.example.com")
       
       // Enable retry with exponential backoff
       retry {
           maxAttempts = 3
           delay = Duration.ofMillis(500)
           maxDelay = Duration.ofSeconds(10)
           backoff = BackoffStrategy.EXPONENTIAL
           jitter = true
           retryOnStatusCodes = setOf(429, 502, 503, 504)
           retryOnExceptions = setOf(
               SocketTimeoutException::class,
               ConnectException::class,
           )
       }
   }

See Also
--------

- :doc:`error-context` - Error context and reporting
- :doc:`parameters` - Parameter configuration
- :doc:`kotlin-dsl` - Kotlin DSL reference
