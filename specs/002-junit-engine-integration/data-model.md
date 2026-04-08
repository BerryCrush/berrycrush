# Data Model: JUnit Engine Integration

**Feature**: 002-junit-engine-integration  
**Date**: 2026-04-07

## JUnit Engine Entities

### LemonCheckTestEngine

The core TestEngine implementation registered with JUnit Platform.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | String | Engine identifier: `"lemoncheck"` |

**Relationships**: Creates `LemonCheckEngineDescriptor` as root descriptor.

---

### LemonCheckEngineDescriptor

Root test descriptor representing the engine in the test tree.

| Attribute | Type | Description |
|-----------|------|-------------|
| uniqueId | UniqueId | JUnit Platform unique identifier |
| displayName | String | Display name: `"LemonCheck"` |

**Relationships**: Contains child `ClassTestDescriptor` nodes.

---

### ClassTestDescriptor

Represents a test class annotated with `@LemonCheckScenarios`.

| Attribute | Type | Description |
|-----------|------|-------------|
| uniqueId | UniqueId | Unique identifier for the class |
| displayName | String | Simple class name |
| testClass | Class | The annotated test class |
| locations | Array<String> | Scenario file location patterns |
| bindings | Class? | Optional custom bindings class |
| configuration | LemonCheckConfiguration? | Configuration annotation instance |

**Relationships**: Contains child `ScenarioTestDescriptor` nodes.

---

### ScenarioTestDescriptor

Represents a single `.scenario` file to execute.

| Attribute | Type | Description |
|-----------|------|-------------|
| uniqueId | UniqueId | Unique identifier including scenario name |
| displayName | String | Scenario file name (without path) |
| scenarioPath | String | Full classpath resource path |
| scenarioSource | URL | URL to the scenario file |

**Relationships**: Belongs to a `ClassTestDescriptor`.

---

### LemonCheckBindings (Interface)

Contract for custom bindings classes that provide runtime values.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getBindings() | Map<String, Any> | Returns binding name-value pairs |
| getOpenApiSpec() | String? | Optional OpenAPI spec path override |
| configure(config: Configuration) | Unit | Optional configuration hook |

---

## Sample Petstore Entities

### Pet (JPA Entity)

Domain entity representing a pet in the store.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | Long | PK, auto-generated | Unique identifier |
| name | String | Not null, 1-100 chars | Pet name |
| status | PetStatus | Not null | Availability status |
| category | String? | Max 100 chars | Pet category (dog, cat, etc.) |
| tags | List<String> | Element collection | Searchable tags |
| price | BigDecimal? | >= 0 | Price in default currency |
| createdAt | Instant | Auto-set | Creation timestamp |
| updatedAt | Instant | Auto-update | Last modification timestamp |

**Enum Values for PetStatus**: `AVAILABLE`, `PENDING`, `SOLD`

---

### NewPet (DTO)

Request body for creating or updating a pet.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| name | String | Required, 1-100 chars | Pet name |
| status | String? | Enum: available/pending/sold | Defaults to "available" |
| category | String? | Max 100 chars | Pet category |
| tags | List<String>? | - | Tags for the pet |
| price | Double? | >= 0 | Pet price |

---

### PetResponse (DTO)

Response body for pet operations.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | Long | Pet identifier |
| name | String | Pet name |
| status | String | Current status |
| category | String? | Pet category |
| tags | List<String> | Tags |
| price | Double? | Price |

---

### ErrorResponse (DTO)

Standard error response format.

| Attribute | Type | Description |
|-----------|------|-------------|
| code | Int | HTTP status or error code |
| message | String | Human-readable error message |
| details | List<String>? | Additional error details |

---

## Entity Relationship Diagram

```text
┌─────────────────────────────────────────────────────────────────┐
│                     JUnit Engine Domain                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  LemonCheckTestEngine                                           │
│         │                                                       │
│         ▼ creates                                               │
│  LemonCheckEngineDescriptor (root)                              │
│         │                                                       │
│         ▼ contains 0..*                                         │
│  ClassTestDescriptor ───────────────┐                           │
│     │   (test class)                │ references                │
│     │                               ▼                           │
│     │                        LemonCheckBindings                 │
│     │                         (interface)                       │
│     ▼ contains 0..*                                             │
│  ScenarioTestDescriptor                                         │
│     (scenario file)                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     Petstore Domain                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│     ┌──────────┐       ┌──────────┐       ┌──────────────┐     │
│     │  NewPet  │──────▶│   Pet    │──────▶│ PetResponse  │     │
│     │  (DTO)   │ maps  │ (Entity) │ maps  │    (DTO)     │     │
│     └──────────┘       └──────────┘       └──────────────┘     │
│                              │                                  │
│                              │ persisted via                    │
│                              ▼                                  │
│                       PetRepository                             │
│                    (Spring Data JPA)                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## State Transitions

### Pet Status

```text
┌───────────┐     sell      ┌───────────┐
│ AVAILABLE │──────────────▶│   SOLD    │
└───────────┘               └───────────┘
      │                           ▲
      │ reserve                   │ complete
      ▼                           │
┌───────────┐                     │
│  PENDING  │─────────────────────┘
└───────────┘
      │
      │ cancel
      ▼
┌───────────┐
│ AVAILABLE │
└───────────┘
```

Valid transitions:
- `AVAILABLE` → `PENDING` (reserve)
- `AVAILABLE` → `SOLD` (direct sale)
- `PENDING` → `SOLD` (complete sale)
- `PENDING` → `AVAILABLE` (cancel reservation)
