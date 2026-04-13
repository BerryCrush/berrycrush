# Scenario File Format Contract

**Feature**: 001-openapi-bdd-testing  
**Date**: 2026-04-07  
**Updated**: 2026-04-10  
**Version**: 3.0.0

This document defines the human-readable BDD scenario file format (`.scenario` files).

> **Note**: Version 3.0.0 adds support for tags, feature blocks, and background steps.

---

## File Extension

`.scenario` - Plain text UTF-8 encoded files

---

## Complete Grammar (EBNF)

```ebnf
(* Top-level structure *)
scenario_file     = [ parameters_block ] , { feature | scenario | scenario_outline | fragment } ;

(* Parameters block - file-level configuration *)
parameters_block  = "parameters:" , NEWLINE , { parameter_entry } ;
parameter_entry   = INDENT , parameter_name , ":" , parameter_value , NEWLINE ;

(* Tags - prefix any scenario or feature *)
tags              = { tag } ;
tag               = "@" , identifier ;

(* Feature block - groups related scenarios with optional background *)
feature           = tags , "feature:" , feature_name , NEWLINE , 
                    [ background ] , { feature_scenario } ;
background        = INDENT , "background:" , NEWLINE , { step } ;
feature_scenario  = INDENT , tags , ( "scenario:" | "outline:" ) , 
                    scenario_name , NEWLINE , { step } , [ examples_block ] ;

(* Scenario definition *)
scenario          = tags , "scenario:" , scenario_name , NEWLINE , { step } ;

(* Scenario outline - parameterized scenarios *)
scenario_outline  = tags , "outline:" , scenario_name , NEWLINE , { step } , examples_block ;

(* Fragment definition - reusable steps *)
fragment          = "fragment:" , fragment_name , NEWLINE , { step } ;

(* Step definition *)
step              = INDENT , step_keyword , step_description , NEWLINE , { directive } ;
step_keyword      = "given " | "when " | "then " | "and " | "but " ;

(* Step directives *)
directive         = INDENT , INDENT , ( call_directive | assert_directive | extract_directive 
                                       | body_directive | include_directive | param_directive ) , NEWLINE ;

call_directive    = "call" , [ "using" , spec_name ] , "^" , operation_id ;
assert_directive  = "assert" , assertion_expression ;
extract_directive = "extract" , json_path , "=>" , variable_name ;
body_directive    = "body:" , json_body ;
include_directive = "include" , fragment_name ;
param_directive   = identifier , ":" , value ;

(* Examples for scenario outline *)
examples_block    = INDENT , "examples:" , NEWLINE , 
                    INDENT , INDENT , header_row , 
                    { INDENT , INDENT , data_row } ;

header_row        = "|" , { cell , "|" } , NEWLINE ;
data_row          = "|" , { cell , "|" } , NEWLINE ;

(* Terminals *)
feature_name      = text ;
scenario_name     = text ;
fragment_name     = text ;
step_description  = text ;
operation_id      = identifier ;
spec_name         = identifier ;
variable_name     = identifier ;
json_path         = "$" , { "." , identifier | "[" , index , "]" } ;
assertion_expression = status_assertion | jsonpath_assertion ;
status_assertion  = "status" , ( number | number_range ) ;
jsonpath_assertion = json_path , operator , value ;
operator          = "equals" | "notEmpty" | "exists" | "greaterThan" | "lessThan" 
                  | "contains" | "in" | "hasSize" | "matches" ;
cell              = { character - "|" } ;
text              = { character - NEWLINE } ;
value             = quoted_string | number | boolean | json_body ;
identifier        = letter , { letter | digit | "_" | "-" } ;
INDENT            = "  " ;  (* 2 spaces *)
NEWLINE           = "\n" | "\r\n" ;
```

---

## Syntax Reference

### 0. Tags

Tags categorize and filter scenarios. They begin with `@` and must appear on lines before the element they annotate.

**Syntax**:
```
@tag1 @tag2
scenario: Tagged scenario
  when test
    call ^test
```

**Built-in Tags**:
| Tag | Description |
|-----|-------------|
| `@ignore` | Skip this scenario during execution |
| `@wip` | Work in progress marker |
| `@slow` | Marks slow-running tests |

**Tag Filtering** (JUnit):
```java
@BerryCrushTags(exclude = {"ignore", "wip"})
@BerryCrushTags(include = {"smoke"})
```

### 0.1. Feature Blocks

Features group related scenarios and can define shared background steps.

**Syntax**:
```
@api
feature: Pet Store Operations
  background:
    given: setup
      call ^createPet
        body: {"name": "Test"}
      assert status 201
      extract $.id => petId

  scenario: list pets
    when: list
      call ^listPets
    then: success
      assert status 200

  @ignore
  scenario: disabled test
    when: skip
      call ^skip
```

**Behavior**:
- Background steps run before **each** scenario in the feature
- Feature tags are inherited by all scenarios within
- Scenario tags are merged with feature tags

### 1. Parameters Block (File-Level Configuration)

For simplified scenario files, a `parameters:` section provides
file-level configuration that overrides bindings configuration for all scenarios in that file.

**Syntax**:
```
parameters:
  baseUrl: "http://localhost:8080"
  timeout: 60
  shareVariablesAcrossScenarios: true
  header.Authorization: "Bearer test-token"

scenario: My test scenario
  when I call the API
    call ^listPets
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `baseUrl` | String | Override the base URL for API requests |
| `timeout` | Number | Request timeout in seconds |
| `environment` | String | Environment name for reporting |
| `strictSchemaValidation` | Boolean | Fail on schema validation warnings |
| `followRedirects` | Boolean | Follow HTTP redirects (default: true) |
| `logRequests` | Boolean | Log HTTP requests |
| `logResponses` | Boolean | Log HTTP responses |
| `shareVariablesAcrossScenarios` | Boolean | Share extracted variables across scenarios in this file |
| `header.<name>` | String | Add/override a default header |
| `autoAssertions.enabled` | Boolean | Enable/disable all auto-assertions |
| `autoAssertions.statusCode` | Boolean | Auto-assert correct status code |
| `autoAssertions.contentType` | Boolean | Auto-assert Content-Type header |
| `autoAssertions.schema` | Boolean | Auto-assert response matches schema |

**Example (Cross-Scenario Variable Sharing)**:
```
parameters:
  shareVariablesAcrossScenarios: true

scenario: Create a resource
  when I create a pet
    call ^createPet
      body: {"name": "Fluffy"}
    extract $.id => petId

scenario: Use the resource
  when I get the pet
    call ^getPetById
      petId: {{petId}}
  then I see the pet
    assert $.name equals "Fluffy"
```

---

### 2. Scenario Definition

The core element of a scenario file. Each scenario starts with `scenario:` keyword.

```
scenario: List all available pets
  given the API is available
  when I request the list of pets
    call ^listPets
  then I get a successful response
    assert status 200
    assert $.length notEmpty
```

**Structure:**
- **Line 1**: `scenario:` followed by the scenario name
- **Following lines**: Steps (indented with 2 spaces)
- **Directives**: Under steps (indented with 4 spaces)

---

### 3. Scenario Outline (Parameterized)

Use `outline:` for parameterized scenarios with multiple data sets.

```
outline: Test multiple pet retrieval
  when I get a pet by ID
    call ^getPetById
      petId: {{petId}}
  then I see the expected name
    assert $.name equals {{expectedName}}
  examples:
    | petId | expectedName |
    | 1     | "Fluffy"     |
    | 2     | "Buddy"      |
    | 3     | "Charlie"    |
```

- `{{placeholder}}` in directives are replaced with values from the Examples table
- Each row generates a separate test execution

---

### 4. Step Keywords

| Keyword | Purpose |
|---------|---------|
| `given` | Precondition setup |
| `when`  | Action to perform |
| `then`  | Expected outcome/assertion |
| `and`   | Continuation of previous step type |
| `but`   | Exception to previous step |

---

### 5. Step Directives

#### `call` - API Call

Invokes an OpenAPI operation by its operationId.

**Basic syntax:**
```
call ^operationId
```

**With named spec (multi-spec):**
```
call using auth ^login
```

**With parameters:**
```
when I create a pet
  call ^createPet
    petId: 123
    status: "available"
    header_Authorization: "Bearer {{token}}"
    body: {"name": "Fluffy", "category": "dog"}
```

#### Parameter Types

| Parameter | Description | Example |
|-----------|-------------|---------|
| Path param | Named path variable | `petId: 123` |
| Query param | Query string parameter | `status: "available"` |
| Header | HTTP header (prefix with `header_`) | `header_Authorization: "Bearer token"` |
| Body | Request body (JSON) | `body: {"name": "Fluffy"}` |

#### `assert` - Assertions

**Status code:**
```
assert status 200
assert status 2xx         # 200-299
assert status 201-204     # Range
```

**JSONPath assertions:**
```
assert $.name equals "Fluffy"
assert $.id exists
assert $.pets notEmpty
assert $.count greaterThan 0
assert $.tags contains "urgent"
assert $.status in ["available", "pending"]
assert $.items hasSize 5
assert $.email matches ".*@.*"
```

| Operator | Description |
|----------|-------------|
| `equals` | Exact equality |
| `not` | Negation prefix |
| `exists` | Field exists |
| `notEmpty` | Array/string not empty |
| `greaterThan` | Numeric > |
| `lessThan` | Numeric < |
| `contains` | Array contains or string includes |
| `in` | Value in list |
| `hasSize` | Array/string length |
| `matches` | Regex match |

#### `extract` - Value Extraction

Extracts values from the response for use in subsequent steps.

```
extract $.id => petId
extract $.user.name => userName
extract $.items[0].id => firstItemId
```

- Left side: JSONPath expression
- Right side: Variable name

#### `include` - Fragment Inclusion

Includes steps from a named fragment.

```
given I am authenticated
  include authenticate
```

#### `body:` - Request Body

Inline JSON body:
```
body: {"name": "Fluffy", "status": "available"}
```

---

### 6. Variable Substitution

Variables are referenced using double curly braces: `{{variableName}}`

**Sources:**
1. Bindings (from `BerryCrushBindings.getBindings()`)
2. Extracted values (from `extract`)
3. Cross-scenario (when `shareVariablesAcrossScenarios: true`)
4. Example rows (from `examples:` table)

**Example:**
```
when I get the pet
  call ^getPetById
    petId: {{createdPetId}}
    header_Authorization: "Bearer {{authToken}}"
```

---

### 7. Fragment Files

Fragments (`.fragment` files) define reusable step sequences.

**fragments/auth.fragment:**
```
fragment: authenticate
  given I have valid credentials
    call using auth ^login
      body: {"username": "test", "password": "test"}
  then authentication succeeds
    assert status 200
    extract $.token => authToken

fragment: logout
  when I log out
    call using auth ^logout
      header_Authorization: "Bearer {{authToken}}"
  then session is terminated
    assert status 200
```

**Usage:**
```
scenario: Access protected resource
  given I am authenticated
    include authenticate
  when I access the resource
    call ^getProfile
      header_Authorization: "Bearer {{authToken}}"
```

---

### 8. Comments

Lines starting with `#` are comments:

```
# This is a comment
scenario: My test
  # This describes the step
  when I do something
    call ^operation
```

---

## Complete Example

```
# Petstore CRUD Test Suite
# Tests basic CRUD operations for the Pet Store API

parameters:
  shareVariablesAcrossScenarios: true
  logRequests: true

scenario: Create a new pet
  when I create a pet
    call ^createPet
      body: {"name": "Fluffy", "status": "available", "category": "dog"}
  then the pet is created
    assert status 201
    assert $.id exists
    extract $.id => petId

scenario: Retrieve the created pet
  when I get the pet
    call ^getPetById
      petId: {{petId}}
  then I see the correct pet
    assert status 200
    assert $.name equals "Fluffy"
    assert $.status equals "available"

scenario: Update the pet
  when I update the pet
    call ^updatePet
      petId: {{petId}}
      body: {"name": "Fluffy Updated", "status": "pending"}
  then the update succeeds
    assert status 200
    assert $.name equals "Fluffy Updated"

scenario: Delete the pet
  when I delete the pet
    call ^deletePet
      petId: {{petId}}
  then the deletion succeeds
    assert status 204
```

---

## Migration from Legacy Syntax

If you have scenario files using the legacy `Feature:` syntax with table-based directives,
update them to the current simplified syntax:

**Legacy:**
```gherkin
Feature: Pet API
  Scenario: List pets
    When I request pets
      | operation | listPets |
      | assert    | status = 200 |
```

**Current:**
```
scenario: List pets
  when I request pets
    call ^listPets
    assert status 200
```

---

## Versioning

Current format version: **2.0**

Changes from 1.0:
- Removed `Feature:` block requirement
- Simplified syntax with `scenario:` as top-level
- New `call ^operationId` syntax replacing `| operation |`
- New `assert` directive replacing `| assert |`
- New `extract $.path => var` syntax replacing `| extract |`
