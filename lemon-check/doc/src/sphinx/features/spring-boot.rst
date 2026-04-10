Spring Boot Integration
=======================

LemonCheck provides seamless integration with Spring Boot, enabling you to:

- Run scenarios against a live Spring Boot application
- Use dependency injection in bindings classes
- Access dynamically allocated ports via ``@LocalServerPort``
- Leverage Spring's test context caching

Setup
-----

Add the Spring module to your dependencies:

**Gradle (Kotlin DSL):**

.. code-block:: kotlin

    dependencies {
        testImplementation("io.github.ktakashi.lemoncheck:spring:0.1.0")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

Configuration
-------------

Test Class
^^^^^^^^^^

Annotate your test class with the required annotations:

.. code-block:: java

    @Suite
    @IncludeEngines("lemoncheck")
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @LemonCheckContextConfiguration
    @LemonCheckScenarios(locations = {"scenarios/*.scenario"})
    @LemonCheckConfiguration(bindings = MyBindings.class)
    public class MyApiTest {
    }

**Required Annotations:**

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Annotation
     - Purpose
   * - ``@SpringBootTest``
     - Starts the embedded Spring Boot application
   * - ``@LemonCheckContextConfiguration``
     - Enables Spring context integration with LemonCheck
   * - ``@LemonCheckScenarios``
     - Specifies scenario file locations
   * - ``@LemonCheckConfiguration``
     - Configures bindings, plugins, and step classes

Bindings Class
^^^^^^^^^^^^^^

Create a Spring-managed bindings class:

.. code-block:: java

    @Component
    @Lazy  // Recommended for port injection timing
    public class MyBindings implements LemonCheckBindings {
        
        @LocalServerPort
        private int port;
        
        @Override
        public Map<String, Object> getBindings() {
            return Map.of(
                "baseUrl", "http://localhost:" + port + "/api/v1"
            );
        }
        
        @Override
        public String getOpenApiSpec() {
            return "my-api.yaml";
        }
    }

**Key Points:**

- Use ``@Component`` to make it a Spring bean
- Use ``@Lazy`` to ensure port is injected before bean creation
- ``@LocalServerPort`` provides the dynamically allocated port
- Can inject other Spring beans via ``@Autowired``

Multi-Spec Support
------------------

For APIs with multiple OpenAPI specs:

.. code-block:: java

    @Component
    @Lazy
    public class MyBindings implements LemonCheckBindings {
        
        @LocalServerPort
        private int port;
        
        @Override
        public Map<String, Object> getBindings() {
            return Map.of(
                "baseUrl", "http://localhost:" + port + "/api/v1"
            );
        }
        
        @Override
        public String getOpenApiSpec() {
            return "petstore.yaml";  // Default spec
        }
        
        @Override
        public Map<String, String> getAdditionalSpecs() {
            return Map.of(
                "auth", "auth.yaml",    // Authentication APIs
                "admin", "admin.yaml"   // Admin APIs
            );
        }
    }

Use named specs in scenarios:

.. code-block:: text

    scenario: Authenticate and access pets
      given I authenticate
        call using auth ^login
          body: {"username": "test", "password": "test"}
      then I have a token
        extract $.token => authToken
      
      when I list pets
        call ^listPets
          header_Authorization: "Bearer {{authToken}}"
      then I see pets
        assert status 200

Custom Steps with Spring Injection
----------------------------------

Create step definitions that use Spring beans:

.. code-block:: java

    @Component
    public class MySteps {
        
        @Autowired
        private UserRepository userRepository;
        
        @Step("a user exists with email {string}")
        public void createUser(String email) {
            User user = new User();
            user.setEmail(email);
            userRepository.save(user);
        }
        
        @Step("the database should have {int} users")
        public void verifyUserCount(int expected) {
            long actual = userRepository.count();
            assert actual == expected : 
                "Expected " + expected + " users but found " + actual;
        }
    }

Register step classes in the configuration:

.. code-block:: java

    @LemonCheckConfiguration(
        bindings = MyBindings.class,
        stepClasses = {MySteps.class}
    )

How It Works
------------

The Spring integration uses a ``BindingsProvider`` SPI implementation:

1. **Discovery**: ``SpringBindingsProvider`` is loaded via ServiceLoader
2. **Detection**: Checks for ``@LemonCheckContextConfiguration`` annotation
3. **Initialization**: Starts Spring ApplicationContext via ``SpringContextAdapter``
4. **Bean Retrieval**: Gets bindings instance from Spring's bean container
5. **Cleanup**: Releases context after test completion

Architecture
^^^^^^^^^^^^

.. code-block:: text

    @SpringBootTest
    @LemonCheckContextConfiguration
            │
            ▼
    SpringBindingsProvider (SPI)
            │
            ▼
    SpringContextAdapter
            │
            ├── Starts ApplicationContext
            ├── Manages lifecycle
            └── Retrieves beans
            │
            ▼
    LemonCheckBindings instance
    (Spring-managed with @Autowired, @LocalServerPort, etc.)

Best Practices
--------------

1. **Use @Lazy for bindings**: Ensures port injection timing is correct

   .. code-block:: java

       @Component
       @Lazy
       public class MyBindings implements LemonCheckBindings { ... }

2. **Separate concerns**: Keep bindings focused on configuration, use step classes for logic

3. **Context caching**: Spring automatically caches context across tests with same configuration

4. **Profile support**: Use ``@ActiveProfiles`` for environment-specific configuration

   .. code-block:: java

       @SpringBootTest
       @ActiveProfiles("test")
       @LemonCheckContextConfiguration
       public class MyApiTest { ... }

5. **Database reset**: Use ``@DirtiesContext`` if tests modify shared state

Troubleshooting
---------------

**Port not injected (returns 0)**

Ensure bindings class has ``@Lazy`` annotation and ``@SpringBootTest`` uses ``RANDOM_PORT``.

**Bean not found**

Verify bindings class has ``@Component`` and is in a scanned package.

**Context not starting**

Check that ``@SpringBootTest`` is present alongside ``@LemonCheckContextConfiguration``.

**Missing @SpringBootTest annotation**

.. code-block:: text

    ConfigurationException: Test class 'MyTest' has @LemonCheckContextConfiguration 
    but is missing @SpringBootTest. Add @SpringBootTest annotation to enable 
    Spring context integration.
