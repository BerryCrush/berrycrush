Fragments
=========

Fragments are reusable scenario steps that can be included in multiple scenarios.
They help reduce duplication and improve maintainability of your test suites.

Creating Fragments
------------------

Fragment files use the ``.fragment`` extension and are placed in the
``src/test/resources/lemoncheck/fragments/`` directory by default.

**lemoncheck/fragments/auth.fragment:**

.. code-block:: gherkin

    # Authentication Fragment
    # Reusable authentication steps for protected endpoints
    
    fragment: authenticate
      given I have valid credentials
        call using auth ^login
          body: {"username": "test", "password": "test"}
        extract $.token => authToken
      then authentication is successful
        assert status 200
    
    fragment: logout
      when I log out
        call using auth ^logout
      then the session is terminated
        assert status 200

Fragment Structure
^^^^^^^^^^^^^^^^^^

Each fragment starts with ``fragment: <name>`` followed by indented steps:

.. code-block:: gherkin

    fragment: <fragment-name>
      <step-type> <description>
        <actions>
      <step-type> <description>
        <actions>

Using Fragments
---------------

Include fragments in your scenarios using the ``include`` directive:

**scenarios/list-pets.scenario:**

.. code-block:: gherkin

    scenario: Authenticated list pets
      given I authenticate first
        include authenticate
      when I request the list of pets
        call ^listPets
      then I get a successful response
        assert status 200

The steps from the ``authenticate`` fragment are expanded inline during execution.

Fragment Discovery
------------------

LemonCheck discovers fragment files using patterns specified in the
``@LemonCheckScenarios`` annotation.

Default Discovery
^^^^^^^^^^^^^^^^^

By default, LemonCheck searches for fragment files matching the pattern
``lemoncheck/fragments/*.fragment``:

.. code-block:: kotlin

    @LemonCheckScenarios(locations = ["lemoncheck/scenarios/*.scenario"])
    // fragments = ["lemoncheck/fragments/*.fragment"] is the default
    class MyApiTest

Custom Fragment Locations
^^^^^^^^^^^^^^^^^^^^^^^^^

You can specify custom fragment locations using the ``fragments`` property:

.. code-block:: kotlin

    @LemonCheckScenarios(
        locations = ["scenarios/*.scenario"],
        fragments = ["shared/fragments/*.fragment", "auth/*.fragment"]
    )
    class MyApiTest

The ``fragments`` property accepts an array of glob patterns, allowing you to:

* Search multiple directories
* Use wildcard patterns (``*``, ``**``)
* Specify exact file paths

Examples:

.. code-block:: kotlin

    // Single directory
    fragments = ["my-fragments/*.fragment"]
    
    // Multiple directories
    fragments = ["auth/*.fragment", "common/*.fragment", "api/*.fragment"]
    
    // Recursive search
    fragments = ["**/*.fragment"]
    
    // Specific files
    fragments = ["fragments/auth.fragment", "fragments/setup.fragment"]

Directory Structure
^^^^^^^^^^^^^^^^^^^

Typical project structure with custom fragment locations:

.. code-block:: text

    src/test/resources/
    ├── petstore.yaml
    ├── auth/
    │   └── auth.fragment
    ├── common/
    │   └── setup.fragment
    └── scenarios/
        ├── pet-crud.scenario
        └── user-api.scenario

With annotation:

.. code-block:: kotlin

    @LemonCheckScenarios(
        locations = ["scenarios/*.scenario"],
        fragments = ["auth/*.fragment", "common/*.fragment"]
    )
    class MyApiTest

Variables in Fragments
----------------------

Fragments can extract values that become available in the calling scenario:

.. code-block:: gherkin

    fragment: authenticate
      given I have valid credentials
        call using auth ^login
          body: {"username": "test", "password": "test"}
        extract $.token => authToken

The ``authToken`` variable is now available in the scenario that included this fragment:

.. code-block:: gherkin

    scenario: Access protected resource
      given I am authenticated
        include authenticate
      when I access the protected endpoint
        call ^getSecretData
          header_Authorization: Bearer {{authToken}}
      then I get the data
        assert status 200

Multi-Spec in Fragments
-----------------------

Fragments can use the ``using`` keyword to call operations from named specs:

.. code-block:: gherkin

    fragment: admin-login
      given I have admin credentials
        call using auth ^login
          body: {"username": "admin", "password": "admin123"}
        extract $.token => adminToken
      then admin authentication is successful
        assert status 200

Example: Complete Test Suite
----------------------------

**Directory structure:**

.. code-block:: text

    src/test/resources/
    ├── petstore.yaml
    ├── auth.yaml
    ├── fragments/
    │   └── auth.fragment
    └── scenarios/
        ├── 01-setup.scenario
        ├── pet-crud.scenario
        └── 99-cleanup.scenario

**fragments/auth.fragment:**

.. code-block:: gherkin

    fragment: authenticate
      given I have valid credentials
        call using auth ^login
          body: {"username": "test", "password": "test"}
        extract $.token => authToken
      then authentication is successful
        assert status 200
    
    fragment: authenticate-admin
      given I have admin credentials
        call using auth ^login
          body: {"username": "admin", "password": "admin123"}
        extract $.token => adminToken
      then admin authentication is successful
        assert status 200

**scenarios/pet-crud.scenario:**

.. code-block:: gherkin

    scenario: Create pet as authenticated user
      given I am authenticated
        include authenticate
      when I create a new pet
        call ^createPet
          header_Authorization: Bearer {{authToken}}
          body: {"name": "Fluffy", "status": "available"}
      then the pet is created
        assert status 201
        extract $.id => petId
    
    scenario: Delete pet as admin
      given I am an admin
        include authenticate-admin
      when I delete the pet
        call ^deletePet
          header_Authorization: Bearer {{adminToken}}
          petId: {{petId}}
      then the pet is deleted
        assert status 204

Best Practices
--------------

1. **Keep fragments focused**: Each fragment should do one thing well
2. **Use descriptive names**: Fragment names should clearly indicate their purpose
3. **Document variables**: Comment which variables are extracted by the fragment
4. **Organize by domain**: Group related fragments together
5. **Avoid deep nesting**: Don't include fragments within fragments
