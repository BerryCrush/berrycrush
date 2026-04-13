# Contract: Report Formats

**Version**: 1.0.0  
**Date**: 2026-04-09  
**Type**: Output Specification

## Overview

This contract defines the four built-in report formats provided by berrycrush:
1. **Text** - Human-readable console output
2. **JSON** - Machine-parseable structured data
3. **XML** - Generic XML structure
4. **JUnit** - JUnit-compatible XML for CI/CD integration

All formats represent the same underlying test execution data but optimize for different consumers (humans, scripts, CI tools).

---

## Format 1: Text Report

**File Extension**: `.txt`  
**Default Path**: `build/reports/berrycrush/report.txt`  
**MIME Type**: `text/plain`

### Format Specification

```
================================================================================
BerryCrush Test Report
================================================================================
Execution Date: {ISO-8601 timestamp}
Duration: {total duration in seconds with 3 decimals}
Scenarios: {total} total, {passed} passed, {failed} failed, {skipped} skipped

{for each scenario}
[{STATUS}] {scenario name} ({duration}s)
  {for each step}
  {symbol} {step description}
    {if failed}
    Step: {step description}
    Expected: {expected value}
    Actual: {actual value}
    {if diff available}
    Diff:
    {diff output, indented}
    
    {if request available}
    Request:
      {method} {url}
      {if body}Body: {body, potentially truncated}
    
    {if response available}
    Response:
      {status code} {status message}
      {if body}Body: {body, potentially truncated}
    {endif}
  {endfor}

{endfor}

================================================================================
Summary: {passed}/{total} scenarios passed ({percentage}%)
{if failures}
Failed Scenarios:
  - {scenario name 1}
  - {scenario name 2}
{endif}
================================================================================
```

### Symbol Legend

- `✓` - PASSED
- `✗` - FAILED
- `⊘` - SKIPPED
- `⚠` - ERROR

### Example Output

```
================================================================================
BerryCrush Test Report
================================================================================
Execution Date: 2026-04-09T14:23:15.123Z
Duration: 2.456s
Scenarios: 3 total, 2 passed, 1 failed

[PASS] List all pets (0.123s)
  ✓ when I request all pets
  ✓ then I get a successful response
    JSONPath: $.pets
    Expected: notEmpty
    Actual: [{"id":1,"name":"Fluffy"},{"id":2,"name":"Buddy"}]

[FAIL] Create a new pet (1.234s)
  ✓ when I create a pet
  ✗ then the pet is created
    Step: assert status 201
    Expected: 201
    Actual: 400
    
    Request:
      POST http://localhost:8080/api/pets
      Body: {"name":"Fluffy","category":"invalid","status":"available"}
    
    Response:
      400 Bad Request
      Body: {"error":"Invalid category","details":"Category 'invalid' not recognized"}

[PASS] Get pet by ID (1.099s)
  ✓ when I request the pet
  ✓ then I see the pet details
    JSONPath: $.name
    Expected: "Fluffy"
    Actual: "Fluffy"

================================================================================
Summary: 2/3 scenarios passed (66.7%)
Failed Scenarios:
  - Create a new pet
================================================================================
```

---

## Format 2: JSON Report

**File Extension**: `.json`  
**Default Path**: `build/reports/berrycrush/report.json`  
**MIME Type**: `application/json`

### Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://github.com/ktakashi/berrycrush/schemas/test-report.json",
  "type": "object",
  "required": ["timestamp", "duration", "summary", "scenarios"],
  "properties": {
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "ISO-8601 execution timestamp"
    },
    "duration": {
      "type": "number",
      "description": "Total execution time in seconds"
    },
    "summary": {
      "type": "object",
      "required": ["total", "passed", "failed", "skipped"],
      "properties": {
        "total": {"type": "integer"},
        "passed": {"type": "integer"},
        "failed": {"type": "integer"},
        "skipped": {"type": "integer"}
      }
    },
    "environment": {
      "type": "object",
      "description": "Test environment metadata",
      "additionalProperties": {"type": "string"}
    },
    "scenarios": {
      "type": "array",
      "items": {"$ref": "#/definitions/Scenario"}
    }
  },
  "definitions": {
    "Scenario": {
      "type": "object",
      "required": ["name", "status", "duration", "steps"],
      "properties": {
        "name": {"type": "string"},
        "status": {"enum": ["PASSED", "FAILED", "SKIPPED", "ERROR"]},
        "duration": {"type": "number"},
        "tags": {
          "type": "array",
          "items": {"type": "string"}
        },
        "steps": {
          "type": "array",
          "items": {"$ref": "#/definitions/Step"}
        }
      }
    },
    "Step": {
      "type": "object",
      "required": ["description", "status", "duration"],
      "properties": {
        "description": {"type": "string"},
        "status": {"enum": ["PASSED", "FAILED", "SKIPPED", "ERROR"]},
        "duration": {"type": "number"},
        "request": {"$ref": "#/definitions/HttpRequest"},
        "response": {"$ref": "#/definitions/HttpResponse"},
        "failure": {"$ref": "#/definitions/Failure"}
      }
    },
    "HttpRequest": {
      "type": "object",
      "properties": {
        "method": {"type": "string"},
        "url": {"type": "string"},
        "headers": {"type": "object"},
        "body": {"type": "string"}
      }
    },
    "HttpResponse": {
      "type": "object",
      "properties": {
        "status": {"type": "integer"},
        "statusMessage": {"type": "string"},
        "headers": {"type": "object"},
        "body": {"type": "string"},
        "duration": {"type": "number"}
      }
    },
    "Failure": {
      "type": "object",
      "required": ["message"],
      "properties": {
        "message": {"type": "string"},
        "expected": {},
        "actual": {},
        "diff": {"type": "string"},
        "stepDescription": {"type": "string"},
        "assertionType": {"type": "string"}
      }
    }
  }
}
```

### Example Output

```json
{
  "timestamp": "2026-04-09T14:23:15.123Z",
  "duration": 2.456,
  "summary": {
    "total": 3,
    "passed": 2,
    "failed": 1,
    "skipped": 0
  },
  "environment": {
    "java.version": "21.0.1",
    "os.name": "macOS",
    "berrycrush.version": "0.1.0"
  },
  "scenarios": [
    {
      "name": "List all pets",
      "status": "PASSED",
      "duration": 0.123,
      "tags": ["smoke", "pets"],
      "steps": [
        {
          "description": "when I request all pets",
          "status": "PASSED",
          "duration": 0.045,
          "request": {
            "method": "GET",
            "url": "http://localhost:8080/api/pets",
            "headers": {
              "Accept": ["application/json"]
            }
          },
          "response": {
            "status": 200,
            "statusMessage": "OK",
            "headers": {
              "Content-Type": ["application/json"]
            },
            "body": "[{\"id\":1,\"name\":\"Fluffy\"},{\"id\":2,\"name\":\"Buddy\"}]",
            "duration": 0.043
          }
        },
        {
          "description": "then I get a successful response",
          "status": "PASSED",
          "duration": 0.078
        }
      ]
    },
    {
      "name": "Create a new pet",
      "status": "FAILED",
      "duration": 1.234,
      "tags": ["pets"],
      "steps": [
        {
          "description": "when I create a pet",
          "status": "PASSED",
          "duration": 0.567
        },
        {
          "description": "then the pet is created",
          "status": "FAILED",
          "duration": 0.667,
          "failure": {
            "message": "Expected status 201 but got 400",
            "expected": "201",
            "actual": "400",
            "stepDescription": "assert status 201",
            "assertionType": "status",
            "request": {
              "method": "POST",
              "url": "http://localhost:8080/api/pets",
              "body": "{\"name\":\"Fluffy\",\"category\":\"invalid\",\"status\":\"available\"}"
            },
            "response": {
              "status": 400,
              "statusMessage": "Bad Request",
              "body": "{\"error\":\"Invalid category\",\"details\":\"Category 'invalid' not recognized\"}"
            }
          }
        }
      ]
    }
  ]
}
```

---

## Format 3: XML Report

**File Extension**: `.xml`  
**Default Path**: `build/reports/berrycrush/report.xml`  
**MIME Type**: `application/xml`

### XML Schema (XSD)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  
  <xs:element name="testReport">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="summary" type="SummaryType"/>
        <xs:element name="environment" type="EnvironmentType" minOccurs="0"/>
        <xs:element name="scenarios">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="scenario" type="ScenarioType" maxOccurs="unbounded"/>
            </xs:sequence>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
      <xs:attribute name="timestamp" type="xs:dateTime" use="required"/>
      <xs:attribute name="duration" type="xs:decimal" use="required"/>
    </xs:complexType>
  </xs:element>
  
  <xs:complexType name="SummaryType">
    <xs:attribute name="total" type="xs:int" use="required"/>
    <xs:attribute name="passed" type="xs:int" use="required"/>
    <xs:attribute name="failed" type="xs:int" use="required"/>
    <xs:attribute name="skipped" type="xs:int" use="required"/>
  </xs:complexType>
  
  <xs:complexType name="ScenarioType">
    <xs:sequence>
      <xs:element name="step" type="StepType" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="name" type="xs:string" use="required"/>
    <xs:attribute name="status" type="StatusType" use="required"/>
    <xs:attribute name="duration" type="xs:decimal" use="required"/>
  </xs:complexType>
  
  <xs:complexType name="StepType">
    <xs:sequence>
      <xs:element name="request" type="HttpRequestType" minOccurs="0"/>
      <xs:element name="response" type="HttpResponseType" minOccurs="0"/>
      <xs:element name="failure" type="FailureType" minOccurs="0"/>
    </xs:sequence>
    <xs:attribute name="description" type="xs:string" use="required"/>
    <xs:attribute name="status" type="StatusType" use="required"/>
    <xs:attribute name="duration" type="xs:decimal" use="required"/>
  </xs:complexType>
  
  <xs:simpleType name="StatusType">
    <xs:restriction base="xs:string">
      <xs:enumeration value="PASSED"/>
      <xs:enumeration value="FAILED"/>
      <xs:enumeration value="SKIPPED"/>
      <xs:enumeration value="ERROR"/>
    </xs:restriction>
  </xs:simpleType>
  
</xs:schema>
```

### Example Output

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testReport timestamp="2026-04-09T14:23:15.123Z" duration="2.456">
  <summary total="3" passed="2" failed="1" skipped="0"/>
  <environment>
    <property name="java.version" value="21.0.1"/>
    <property name="os.name" value="macOS"/>
    <property name="berrycrush.version" value="0.1.0"/>
  </environment>
  <scenarios>
    <scenario name="List all pets" status="PASSED" duration="0.123">
      <step description="when I request all pets" status="PASSED" duration="0.045">
        <request method="GET" url="http://localhost:8080/api/pets">
          <headers>
            <header name="Accept" value="application/json"/>
          </headers>
        </request>
        <response status="200" statusMessage="OK" duration="0.043">
          <headers>
            <header name="Content-Type" value="application/json"/>
          </headers>
          <body><![CDATA[[{"id":1,"name":"Fluffy"},{"id":2,"name":"Buddy"}]]]></body>
        </response>
      </step>
      <step description="then I get a successful response" status="PASSED" duration="0.078"/>
    </scenario>
    
    <scenario name="Create a new pet" status="FAILED" duration="1.234">
      <step description="when I create a pet" status="PASSED" duration="0.567"/>
      <step description="then the pet is created" status="FAILED" duration="0.667">
        <failure message="Expected status 201 but got 400" assertionType="status">
          <expected>201</expected>
          <actual>400</actual>
          <request method="POST" url="http://localhost:8080/api/pets">
            <body><![CDATA[{"name":"Fluffy","category":"invalid","status":"available"}]]></body>
          </request>
          <response status="400" statusMessage="Bad Request">
            <body><![CDATA[{"error":"Invalid category","details":"Category 'invalid' not recognized"}]]></body>
          </response>
        </failure>
      </step>
    </scenario>
  </scenarios>
</testReport>
```

---

## Format 4: JUnit XML Report

**File Extension**: `.xml`  
**Default Path**: `build/test-results/berrycrush/TEST-berrycrush.xml`  
**MIME Type**: `application/xml`  
**Standard**: JUnit XML (Ant JUnit task format)

### Mapping Rules

| BerryCrush Concept | JUnit XML Element |
|---------------------|-------------------|
| Scenario | `<testcase>` |
| Step failure | `<failure>` within testcase |
| Scenario file | `<testsuite name>` |
| Collection of files | `<testsuites>` |

### Format Specification

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="BerryCrush" tests="{total}" failures="{failures}" errors="{errors}" time="{duration}">
  <testsuite name="{scenario-file-name}" tests="{scenarios-in-file}" failures="{}" errors="{}" skipped="{}" time="{}" timestamp="{ISO-8601}">
    <testcase name="{scenario-name}" classname="{scenario-file-path}" time="{duration}">
      <!-- If scenario passed: empty element -->
      
      <!-- If scenario failed: -->
      <failure message="{first-failure-message}" type="AssertionError">
{full-failure-details-with-context}
      </failure>
      
      <!-- If scenario had error: -->
      <error message="{error-message}" type="{exception-class}">
{stack-trace}
      </error>
      
      <!-- If scenario skipped: -->
      <skipped message="{skip-reason}"/>
    </testcase>
  </testsuite>
</testsuites>
```

### Example Output

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="BerryCrush" tests="3" failures="1" errors="0" time="2.456">
  <testsuite name="pet-api-scenarios" tests="3" failures="1" errors="0" skipped="0" 
             time="2.456" timestamp="2026-04-09T14:23:15.123Z">
    
    <testcase name="List all pets" classname="scenarios.pet-api" time="0.123">
      <!-- Passed: empty element -->
    </testcase>
    
    <testcase name="Create a new pet" classname="scenarios.pet-api" time="1.234">
      <failure message="Expected status 201 but got 400" type="StatusAssertionError">
Scenario: Create a new pet
Step: then the pet is created
  assert status 201

Expected: 201
Actual: 400

Request:
  POST http://localhost:8080/api/pets
  Headers:
    Content-Type: application/json
  Body:
    {"name":"Fluffy","category":"invalid","status":"available"}

Response:
  400 Bad Request
  Headers:
    Content-Type: application/json
  Body:
    {"error":"Invalid category","details":"Category 'invalid' not recognized"}
      </failure>
    </testcase>
    
    <testcase name="Get pet by ID" classname="scenarios.pet-api" time="1.099">
      <!-- Passed: empty element -->
    </testcase>
    
    <system-out><![CDATA[
# Additional console output (if any)
    ]]></system-out>
    
    <system-err><![CDATA[
# Error output (if any)
    ]]></system-err>
    
  </testsuite>
</testsuites>
```

### CI/CD Tool Compatibility

| Tool | Supported | Notes |
|------|-----------|-------|
| Jenkins | ✅ | Parses standard JUnit XML |
| GitHub Actions | ✅ | Requires `dorny/test-reporter` action |
| GitLab CI | ✅ | Native JUnit report support |
| CircleCI | ✅ | Store as test results artifact |
| TeamCity | ✅ | Auto-detects JUnit XML |
| Azure DevOps | ✅ | Publish Test Results task |

---

## Configuration

### Selecting Report Formats

#### Via Configuration File

Create `berrycrush.properties`:

```properties
berrycrush.reports.enabled=text,json,junit
berrycrush.reports.outputDir=build/reports/berrycrush
```

Or `berrycrush.yml`:

```yaml
berrycrush:
  reports:
    enabled: [text, json, junit]
    outputDir: build/reports/berrycrush
    formats:
      junit:
        includeSystemOut: true
        includeStackTraces: true
```

#### Via Annotation

```kotlin
@BerryCrushConfiguration(
    reports = [
        ReportFormat.TEXT,
        ReportFormat.JSON,
        ReportFormat.JUNIT
    ]
)
class MyApiTest
```

#### Per-Test Override

```kotlin
@BerryCrushReports([ReportFormat.JUNIT])
@Test
fun testCriticalFlow() {
    // Only JUnit report for this test
}
```

---

## Report Plugin Implementation

All four formats are implemented as plugins (see [plugin-spi.md](plugin-spi.md)):

```kotlin
class JunitReportPlugin(
    val outputPath: Path = Paths.get("build/test-results/berrycrush/TEST-berrycrush.xml"),
    val includeSystemOut: Boolean = false
) : BerryCrushPlugin {
    
    override val priority: Int = 100  // Run after tests
    override val name: String = "JUnit XML Report"
    
    private val scenarios = mutableListOf<ScenarioResult>()
    
    override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        scenarios.add(result)
    }
    
    // After all scenarios, generate report
    // (Actual implementation would detect end of test run)
}
```

---

## Custom Report Formats

Users can create custom report formats by implementing `BerryCrushPlugin`:

```kotlin
class CsvReportPlugin : BerryCrushPlugin {
    override val priority: Int = 100
    override val name: String = "CSV Report"
    
    override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        // Collect data
    }
    
    // Generate CSV file
}
```

Register custom format:

```kotlin
@BerryCrushConfiguration(
    plugins = [CsvReportPlugin::class]
)
```

---

## Output Guarantees

**All report formats guarantee**:
1. **Completeness**: All executed scenarios and steps are included
2. **Accuracy**: Timing data matches actual execution (monotonic clock)
3. **Consistency**: Same test run produces identical reports (deterministic)
4. **Validity**: Output is valid according to format specification (JSON schema, XML XSD, etc.)
5. **Atomicity**: Report files are written atomically (no partial files on failure)

**Failure Handling**:
- If report generation fails, library logs error but does not fail the test run
- Built-in report plugins handle I/O errors gracefully (no exceptions)
- Invalid directory paths are created if possible, otherwise report is skipped with warning

---

## Performance Characteristics

| Format | Generation Time (1000 scenarios) | File Size (1000 scenarios) |
|--------|-----------------------------------|---------------------------|
| Text   | ~50ms                             | ~500 KB                   |
| JSON   | ~100ms                            | ~800 KB                   |
| XML    | ~120ms                            | ~1.2 MB                   |
| JUnit  | ~80ms                             | ~600 KB                   |

All formats scale linearly with scenario count.

---

## Versioning & Compatibility

**Format Stability**:
- Text format: Informational only, format may change (human readers adapt easily)
- JSON format: Schema versioned, backward-compatible additions only
- XML format: Schema versioned, backward-compatible additions only
- JUnit XML: Follows standard, high stability

**Breaking Change Policy**:
- JSON/XML schema changes trigger minor version bump
- Removing fields triggers major version bump
