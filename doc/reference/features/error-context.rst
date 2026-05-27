Enhanced Error Context
======================

When tests fail, BerryCrush provides rich error context including HTTP request
and response details, making it easier to diagnose issues without adding debug
logging.

Sample Error Output
-------------------

When an assertion fails, you'll see detailed context:

.. code-block:: text

   Scenario 'Create pet' failed:
     Step 1 (then): : pet is created at pet-api.scenario:15:5
       - Expected status 201 but got 400
       ━━━ HTTP Response ━━━
       Status: 400
       content-type: application/json
       date: Tue, 26 May 2026 15:53:21 GMT
       Body: {"error": "name cannot be empty", "field": "name"}

Configuration
-------------

Control error context output using the ``errorContext`` parameters at file,
feature, or scenario level:

.. code-block:: berrycrush

   parameters:
     errorContext.includeRequestBody: true
     errorContext.includeResponseBody: true
     errorContext.maxBodySize: 4096

Available Options
~~~~~~~~~~~~~~~~~

+-----------------------------------+-----------+--------------------------------+
| Parameter                         | Default   | Description                    |
+===================================+===========+================================+
| ``errorContext.includeRequestBody``| ``true`` | Include request body in errors |
+-----------------------------------+-----------+--------------------------------+
| ``errorContext.includeResponseBody``| ``true``| Include response body in errors|
+-----------------------------------+-----------+--------------------------------+
| ``errorContext.maxBodySize``      | ``1024``  | Max body characters to display |
+-----------------------------------+-----------+--------------------------------+

Header Masking
~~~~~~~~~~~~~~

Sensitive headers are automatically masked for security:

- ``Authorization``
- ``Cookie``
- ``X-Api-Key``

Example output:

.. code-block:: text

   Authorization: ***
   Cookie: ***

Scenario-Level Override
-----------------------

Override error context settings for specific scenarios:

.. code-block:: berrycrush

   scenario: Debug failing request
     parameters:
       errorContext.includeRequestBody: true
       errorContext.includeResponseBody: true
       errorContext.maxBodySize: 8192
     when I create a pet
       call ^createPet
         body: {"name": ""}
       assert status 201

Feature-Level Defaults
----------------------

Set defaults for all scenarios in a feature:

.. code-block:: berrycrush

   feature: Pet API
     parameters:
       errorContext.includeResponseBody: true
       errorContext.maxBodySize: 2048

     scenario: Create pet
       when I create a pet
         call ^createPet
         assert status 201

Disabling Body Logging
----------------------

For security-sensitive operations, disable body logging:

.. code-block:: berrycrush

   scenario: Authenticate user
     parameters:
       errorContext.includeRequestBody: false
       errorContext.includeResponseBody: false
     when I authenticate
       call ^authenticate
         body: {"username": "admin", "password": "secret"}
       assert status 200

Body Truncation
---------------

Large response bodies are automatically truncated. When truncation occurs,
the output indicates this:

.. code-block:: text

   Body: {"items": [{"id": 1, "name": "Item 1"}, {"id": 2, ... (truncated)
