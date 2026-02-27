# API Contract: Resources REST API

**Feature**: 002-resources-tests | **Date**: 2026-02-19
**Base path**: `/api/v1/resources`

## Endpoints

### POST /expose

Exposes a resource to the Wanaku Router.

**Request**:
```
Content-Type: application/json
Authorization: Bearer <token> (if auth enabled)
```

**Body**:
```json
{
  "name": "test-file",
  "location": "file:///tmp/test.txt",
  "type": "file",
  "description": "A test file",
  "mimeType": "text/plain"
}
```

**Response 200**:
```json
{
  "data": {
    "name": "test-file",
    "location": "file:///tmp/test.txt",
    "type": "file",
    "description": "A test file",
    "mimeType": "text/plain",
    "params": [],
    "configurationURI": null,
    "secretsURI": null,
    "namespace": null,
    "labels": {}
  },
  "error": null
}
```

---

### POST /exposeWithPayload

Exposes a resource with provisioning configuration.

**Body**:
```json
{
  "payload": {
    "name": "configured-file",
    "location": "file:///tmp/data.txt",
    "type": "file",
    "mimeType": "text/plain"
  },
  "configurationData": "some.key=some-value",
  "secretsData": null
}
```

**Response 200**: Same wrapper as `/expose`.

---

### GET /list

Lists all exposed resources. Supports optional label filtering.

**Query parameters**:
- `labelFilter` (optional): Filter resources by labels

**Response 200**:
```json
{
  "data": [
    {
      "name": "test-file",
      "location": "file:///tmp/test.txt",
      "type": "file",
      "description": "A test file",
      "mimeType": "text/plain",
      "params": [],
      "configurationURI": null,
      "secretsURI": null,
      "namespace": null,
      "labels": {}
    }
  ],
  "error": null
}
```

---

### PUT /remove

Removes a resource by name.

**Query parameters**:
- `resource` (required): Resource name to remove

**Example**: `PUT /api/v1/resources/remove?resource=test-file`

**Response 200/204**: Empty body on success.

---

### POST /update

Updates an existing resource.

**Body**: Same as `/expose` — full `ResourceReference` JSON.

**Response 200**: Updated `ResourceReference` in wrapper.

---

## MCP Protocol Operations

### resources/list

Lists resources via MCP Streamable HTTP transport.

**MCP Request** (handled by McpAssured):
```json
{
  "jsonrpc": "2.0",
  "method": "resources/list",
  "params": {},
  "id": 1
}
```

**MCP Response** (parsed by McpAssured as `ResourcesPage`):
```json
{
  "jsonrpc": "2.0",
  "result": {
    "resources": [
      {
        "uri": "file:///tmp/test.txt",
        "name": "test-file",
        "mimeType": "text/plain",
        "description": "A test file"
      }
    ]
  },
  "id": 1
}
```

### resources/read

Reads a resource's content via MCP.

**MCP Request**:
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "file:///tmp/test.txt"
  },
  "id": 2
}
```

**MCP Response** (parsed by McpAssured as `ResourceResponse`):
```json
{
  "jsonrpc": "2.0",
  "result": {
    "contents": [
      {
        "uri": "file:///tmp/test.txt",
        "mimeType": "text/plain",
        "text": "Hello Wanaku Resources"
      }
    ]
  },
  "id": 2
}
```

---

## RouterClient Method Mapping

| Test Operation | RouterClient Method | HTTP |
|---------------|-------------------|------|
| Expose resource | `exposeResource(ResourceConfig)` | POST /expose |
| Expose with config | `exposeResourceWithConfig(ResourceConfig, String)` | POST /exposeWithPayload |
| List resources | `listResources()` | GET /list |
| Remove resource | `removeResource(String name)` | PUT /remove?resource={name} |
| Clear all | `clearAllResources()` | list + remove each |
| Check exists | `resourceExists(String name)` | list + filter |

## McpTestClient Method Mapping

| Test Operation | McpTestClient Method | MCP Operation |
|---------------|---------------------|---------------|
| List resources | `when().resourcesList()` | resources/list |
| Read resource | `when().resourcesRead(uri)` | resources/read |
