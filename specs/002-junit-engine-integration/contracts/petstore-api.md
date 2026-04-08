# Petstore API Contract

**Feature**: 002-junit-engine-integration  
**Date**: 2026-04-07  
**Base URL**: `http://localhost:{port}/api/v1`

## Endpoints

### List Pets

```
GET /pets
```

**Query Parameters**:

| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| limit | integer | No | 20 | Maximum pets to return (1-100) |
| status | string | No | - | Filter by status: `available`, `pending`, `sold` |

**Response 200**:
```json
{
  "pets": [
    {
      "id": 1,
      "name": "Max",
      "status": "available",
      "category": "dog",
      "tags": ["friendly", "trained"],
      "price": 299.99
    }
  ],
  "total": 1
}
```

**Response 400** (Invalid parameters):
```json
{
  "code": 400,
  "message": "Invalid limit value",
  "details": ["limit must be between 1 and 100"]
}
```

---

### Get Pet by ID

```
GET /pets/{petId}
```

**Path Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| petId | integer (int64) | Yes | Pet identifier |

**Response 200**:
```json
{
  "id": 1,
  "name": "Max",
  "status": "available",
  "category": "dog",
  "tags": ["friendly", "trained"],
  "price": 299.99
}
```

**Response 404**:
```json
{
  "code": 404,
  "message": "Pet not found",
  "details": ["No pet exists with id: 1"]
}
```

---

### Create Pet

```
POST /pets
```

**Request Body**:
```json
{
  "name": "Buddy",
  "status": "available",
  "category": "dog",
  "tags": ["puppy", "playful"],
  "price": 450.00
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | Yes | 1-100 characters |
| status | string | No | `available` (default), `pending`, `sold` |
| category | string | No | Max 100 characters |
| tags | array[string] | No | - |
| price | number | No | >= 0 |

**Response 201**:
```json
{
  "id": 2,
  "name": "Buddy",
  "status": "available",
  "category": "dog",
  "tags": ["puppy", "playful"],
  "price": 450.00
}
```

**Response 400** (Validation error):
```json
{
  "code": 400,
  "message": "Validation failed",
  "details": [
    "name: must not be blank",
    "price: must be greater than or equal to 0"
  ]
}
```

---

### Update Pet

```
PUT /pets/{petId}
```

**Path Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| petId | integer (int64) | Yes | Pet identifier |

**Request Body**: Same as Create Pet

**Response 200**: Updated pet object

**Response 404**:
```json
{
  "code": 404,
  "message": "Pet not found",
  "details": ["No pet exists with id: 99"]
}
```

---

### Delete Pet

```
DELETE /pets/{petId}
```

**Path Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| petId | integer (int64) | Yes | Pet identifier |

**Response 204**: No content (success)

**Response 404**:
```json
{
  "code": 404,
  "message": "Pet not found",
  "details": ["No pet exists with id: 99"]
}
```

---

### Login (Authentication)

```
POST /auth/login
```

**Request Body**:
```json
{
  "username": "admin",
  "password": "secret"
}
```

**Response 200**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

**Response 401**:
```json
{
  "code": 401,
  "message": "Invalid credentials"
}
```

---

## Data Types

### Pet

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | integer (int64) | No | Auto-generated identifier |
| name | string | No | Pet name (1-100 chars) |
| status | string | No | `available`, `pending`, `sold` |
| category | string | Yes | Pet category |
| tags | array[string] | No | Empty array if none |
| price | number (double) | Yes | Price in default currency |

### Error

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| code | integer | No | HTTP status code |
| message | string | No | Error message |
| details | array[string] | Yes | Additional details |

---

## Sample Scenarios

### list-pets.scenario
```
scenario "List all available pets"
  spec "petstore.yaml"
  
  when calling "listPets"
    with query "status" = "available"
  
  then status is 200
    and body "$.pets" is not empty
    and body "$.pets[*].status" all equal "available"
```

### create-pet.scenario
```
scenario "Create a new pet"
  spec "petstore.yaml"
  
  when calling "createPet"
    with body
      {
        "name": "Fluffy",
        "status": "available",
        "category": "cat",
        "price": 199.99
      }
  
  then status is 201
    and body "$.name" equals "Fluffy"
    and body "$.id" is not null
```

### get-pet.scenario
```
scenario "Get pet by ID"
  spec "petstore.yaml"
  given "petId" = 1
  
  when calling "getPetById"
    with path "petId" = ${petId}
  
  then status is 200
    and body "$.id" equals ${petId}
```

### update-pet.scenario
```
scenario "Update existing pet"
  spec "petstore.yaml"
  given "petId" = 1
  
  when calling "updatePet"
    with path "petId" = ${petId}
    with body
      {
        "name": "Max Updated",
        "status": "sold"
      }
  
  then status is 200
    and body "$.name" equals "Max Updated"
    and body "$.status" equals "sold"
```

### delete-pet.scenario
```
scenario "Delete a pet"
  spec "petstore.yaml"
  given "petId" = 1
  
  when calling "deletePet"
    with path "petId" = ${petId}
  
  then status is 204
```
