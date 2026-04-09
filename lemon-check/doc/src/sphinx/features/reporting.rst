Reporting
=========

LemonCheck provides multiple report formats to integrate with your CI/CD pipeline
and development workflow.

Report Formats
--------------

LemonCheck includes four built-in report formats:

Text Report
^^^^^^^^^^^

Human-readable console output, ideal for development and debugging.

.. code-block:: text

    === Scenario: List all pets ===
      ✓ Given the API is available: PASSED
      ✓ When I request GET /api/pets: PASSED
      ✓ Then the response status should be 200: PASSED
      Result: PASSED (123ms)

Configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        pluginClasses = [TextReportPlugin::class]
    )

JSON Report
^^^^^^^^^^^

Machine-parseable JSON format, suitable for custom integrations.

.. code-block:: json

    {
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "report": {
        "name": "LemonCheck Test Report",
        "timestamp": "2026-04-09T10:30:00Z",
        "duration": 1234,
        "summary": {
          "total": 5,
          "passed": 4,
          "failed": 1,
          "skipped": 0,
          "errors": 0
        },
        "scenarios": [...]
      }
    }

Configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        plugins = ["report:json:build/reports/lemon-check.json"]
    )

Or programmatically:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        pluginClasses = [JsonReportPlugin::class]
    )

JUnit XML Report
^^^^^^^^^^^^^^^^

JUnit XML format for CI/CD integration (Jenkins, GitHub Actions, GitLab CI, etc.).

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <testsuites name="LemonCheck" tests="5" failures="1" errors="0" time="1.234">
      <testsuite name="Pet API" tests="3" failures="1">
        <testcase name="List all pets" classname="scenarios.pet-api" time="0.123"/>
        <testcase name="Create a pet" classname="scenarios.pet-api" time="0.456">
          <failure message="Expected status 201 but got 400" type="AssertionError">
            Expected: 201
            Actual: 400
            
            Request: POST /api/pets
            Response: 400 Bad Request
          </failure>
        </testcase>
      </testsuite>
    </testsuites>

Configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        plugins = ["report:junit:build/test-results/lemon-check.xml"]
    )

XML Report
^^^^^^^^^^

Generic XML format for custom processing.

Configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        pluginClasses = [XmlReportPlugin::class]
    )

Configuring Reports
-------------------

Via Annotation
^^^^^^^^^^^^^^

.. code-block:: kotlin

    @LemonCheckConfiguration(
        pluginClasses = [
            TextReportPlugin::class,   // Console output
            JunitReportPlugin::class   // CI/CD integration
        ],
        plugins = [
            "report:json:reports/test-results.json"
        ]
    )

Via Bindings
^^^^^^^^^^^^

.. code-block:: kotlin

    class MyBindings : LemonCheckBindings {
        override fun getPlugins(): List<LemonCheckPlugin> {
            return listOf(
                TextReportPlugin(),
                JsonReportPlugin("build/reports/lemon-check.json"),
                JunitReportPlugin("build/test-results/TEST-lemon-check.xml")
            )
        }
    }

Report Output Locations
-----------------------

By default, reports are written to:

* JSON: ``build/reports/lemon-check-report.json``
* JUnit: ``build/test-results/TEST-lemon-check.xml``
* Text: Console output (stdout)

Customize paths via constructor arguments:

.. code-block:: kotlin

    JsonReportPlugin("custom/path/report.json")
    JunitReportPlugin("custom/path/junit.xml")

CI/CD Integration
-----------------

GitHub Actions
^^^^^^^^^^^^^^

.. code-block:: yaml

    - name: Run API tests
      run: ./gradlew test

    - name: Publish Test Results
      uses: EnricoMi/publish-unit-test-result-action@v2
      if: always()
      with:
        files: build/test-results/**/*.xml

Jenkins
^^^^^^^

.. code-block:: groovy

    post {
        always {
            junit 'build/test-results/**/*.xml'
        }
    }

GitLab CI
^^^^^^^^^

.. code-block:: yaml

    test:
      script:
        - ./gradlew test
      artifacts:
        reports:
          junit: build/test-results/**/*.xml

Custom Report Plugins
---------------------

Create custom reports by extending ``ReportPlugin``:

.. code-block:: kotlin

    class HtmlReportPlugin(
        private val outputPath: String = "build/reports/lemon-check.html"
    ) : ReportPlugin("html") {

        override fun formatReport(report: TestReport): String {
            return buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html>")
                appendLine("<head><title>LemonCheck Report</title></head>")
                appendLine("<body>")
                
                for (scenario in report.scenarios) {
                    appendLine("<h2>${scenario.name}</h2>")
                    appendLine("<ul>")
                    for (step in scenario.steps) {
                        val icon = when (step.status) {
                            ResultStatus.PASSED -> "✓"
                            ResultStatus.FAILED -> "✗"
                            else -> "○"
                        }
                        appendLine("<li>$icon ${step.description}</li>")
                    }
                    appendLine("</ul>")
                }
                
                appendLine("</body>")
                appendLine("</html>")
            }
        }

        override fun onReportGenerated(content: String) {
            File(outputPath).writeText(content)
        }
    }

Best Practices
--------------

1. **Use JUnit XML for CI/CD**: Most CI tools understand this format
2. **Generate JSON for dashboards**: Easy to parse and visualize
3. **Use Text for debugging**: Immediate feedback during development
4. **Set output paths explicitly**: Avoid conflicts with other tools
5. **Include timing data**: Helps identify slow tests
