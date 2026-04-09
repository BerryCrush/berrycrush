Multi-Spec OpenAPI Support
==========================

LemonCheck supports working with multiple OpenAPI specifications in a single test suite.
This is useful when your API is split across multiple spec files or when you need to
test different API versions.

Configuring Multiple Specs
--------------------------

Default Spec
^^^^^^^^^^^^

The primary spec is configured via ``getOpenApiSpec()`` in your bindings class:

.. code-block:: kotlin

    class MyBindings : LemonCheckBindings {
        override fun getOpenApiSpec(): String? {
            return "petstore.yaml"  // Default spec
        }
    }

Additional Named Specs
^^^^^^^^^^^^^^^^^^^^^^

To register additional specs, override the ``getAdditionalSpecs()`` method:

.. code-block:: java

    @Component
    @Lazy
    public class PetstoreBindings implements LemonCheckBindings {
        
        @LocalServerPort
        private int port;
        
        @Override
        public Map<String, Object> getBindings() {
            return Map.of("baseUrl", "http://localhost:" + port + "/api/v1");
        }
        
        @Override
        public String getOpenApiSpec() {
            return "petstore.yaml";  // Default spec for pet operations
        }
        
        @Override
        public Map<String, String> getAdditionalSpecs() {
            return Map.of(
                "auth", "auth.yaml",       // Authentication APIs
                "admin", "admin.yaml"      // Admin APIs
            );
        }
    }

Using Named Specs in Scenarios
------------------------------

Default Operations
^^^^^^^^^^^^^^^^^^

Operations from the default spec can be called directly:

.. code-block:: gherkin

    scenario: List all pets
      when I request pets
        call ^listPets
      then I get a response
        assert status 200

Named Spec Operations
^^^^^^^^^^^^^^^^^^^^^

To call operations from a named spec, use the ``using`` keyword:

.. code-block:: gherkin

    scenario: Authenticate and list pets
      given I have valid credentials
        call using auth ^login
          body: {"username": "test", "password": "test"}
      then authentication returns a token
        assert status 200
        extract $.token => authToken
      
      when I request pets
        call ^listPets
      then I get a response
        assert status 200

The syntax is: ``call using <spec-name> ^<operationId>``

Example: Separating Auth from Main API
--------------------------------------

**auth.yaml:**

.. code-block:: yaml

    openapi: 3.0.3
    info:
      title: Authentication API
      version: 1.0.0
    paths:
      /auth/login:
        post:
          operationId: login
          requestBody:
            required: true
            content:
              application/json:
                schema:
                  type: object
                  properties:
                    username:
                      type: string
                    password:
                      type: string
          responses:
            '200':
              description: Login successful
              content:
                application/json:
                  schema:
                    type: object
                    properties:
                      token:
                        type: string
      /auth/logout:
        post:
          operationId: logout
          responses:
            '200':
              description: Logout successful

**petstore.yaml:**

.. code-block:: yaml

    openapi: 3.0.3
    info:
      title: Pet Store API
      version: 1.0.0
    paths:
      /pets:
        get:
          operationId: listPets
          responses:
            '200':
              description: List of pets

**auth.scenario:**

.. code-block:: gherkin

    # Authentication tests using the 'auth' spec
    
    scenario: Successful Login
      given I have valid credentials
        call using auth ^login
          body: {"username": "test", "password": "test"}
      then authentication returns a token
        assert status 200
        extract $.token => authToken
    
    scenario: Logout
      when I log out
        call using auth ^logout
      then logout is successful
        assert status 200

Auto-Resolution
---------------

When an ``operationId`` is unique across all registered specs, LemonCheck can
automatically resolve which spec to use without the ``using`` keyword.

.. warning::
   If the same ``operationId`` exists in multiple specs, you must use ``using``
   to disambiguate. LemonCheck will throw an ``AmbiguousOperationException`` if
   it cannot determine which spec to use.

Best Practices
--------------

1. **Organize by domain**: Split specs by functional area (auth, admin, public API)
2. **Use unique operationIds**: Avoid duplicate operationIds across specs
3. **Document spec dependencies**: Note which tests require which specs
4. **Keep default spec focused**: Use the default for your main API operations
