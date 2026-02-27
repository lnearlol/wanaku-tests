# Data Model: Resources Tests

**Feature**: 002-resources-tests | **Date**: 2026-02-19

## Entities

### ResourceReference

Represents a resource exposed through the Wanaku Router. Deserialized from the Router REST API responses.

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| name | String | required | Brief name for the resource (unique identifier) |
| location | String | required | URL or file path (e.g., `file:///tmp/test.txt`) |
| type | String | required | Resource type, maps to provider (e.g., `"file"`) |
| description | String | optional | Longer description of the resource |
| mimeType | String | optional | MIME type (e.g., `"text/plain"`, `"application/json"`) |
| params | List\<Param\> | optional | Name-value parameter pairs |
| configurationURI | String | server-managed | URI to non-sensitive config (set by provisioning) |
| secretsURI | String | server-managed | URI to sensitive credentials (set by provisioning) |
| namespace | String | server-managed | Namespace scope |
| labels | Map\<String, String\> | optional | Metadata labels for filtering |

**JSON representation** (from Router API):
```json
{
  "name": "test-file",
  "location": "file:///tmp/test.txt",
  "type": "file",
  "description": "A test file resource",
  "mimeType": "text/plain",
  "params": [],
  "configurationURI": null,
  "secretsURI": null,
  "namespace": null,
  "labels": {}
}
```

**Jackson annotations**: `@JsonIgnoreProperties(ignoreUnknown = true)` — same pattern as `ToolInfo`.

### ResourceConfig

Builder for creating resource expose requests. Used by test code to construct `ResourceReference` objects for the expose API.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| name | String | yes | Resource name |
| location | String | yes | File path or URI |
| type | String | yes (default: `"file"`) | Provider type |
| description | String | no | Resource description |
| mimeType | String | no (default: `"text/plain"`) | MIME type |

**Builder pattern** (matches `HttpToolConfig`):
```java
ResourceConfig.builder()
    .name("test-file")
    .location("file:///tmp/test.txt")
    .type("file")
    .mimeType("text/plain")
    .description("Test file resource")
    .build();
```

### ResourcePayload

Request body for `/exposeWithPayload` endpoint. Wraps a `ResourceReference` with configuration data.

| Field | Type | Notes |
|-------|------|-------|
| resourceReference | ResourceReference (inline) | The resource to expose |
| configurationData | String | Non-sensitive config (properties format) |
| secretsData | String | Sensitive credentials (properties format) |

**JSON representation**:
```json
{
  "payload": {
    "name": "configured-file",
    "location": "file:///tmp/data.txt",
    "type": "file",
    "mimeType": "text/plain"
  },
  "configurationData": "some.property=value",
  "secretsData": null
}
```

Note: The `exposeWithPayload` endpoint uses `"payload"` as the key for the resource reference, matching the `ProvisionAwarePayload` interface in Wanaku.

## Relationships

```
ResourceConfig ──builds──> ResourceReference (JSON body)
                              │
                              ├── POST /expose (direct)
                              └── POST /exposeWithPayload (wrapped in ResourcePayload)

RouterClient ──manages──> ResourceReference (CRUD operations)

McpTestClient ──reads──> ResourcesPage (resources/list)
              ──reads──> ResourceResponse (resources/read)
```

## Router API Response Wrapper

All Router responses use the `WanakuResponse` wrapper:

```json
{
  "data": <ResourceReference or List<ResourceReference>>,
  "error": null
}
```

The `RouterClient` must unwrap this envelope, same pattern as tool operations.

## MCP Response Structures (from McpAssured)

### ResourcesPage (from `resources/list`)
| Field | Type |
|-------|------|
| resources | List\<ResourceInfo\> |
| nextCursor | String (nullable) |

### ResourceInfo (individual resource in MCP list)
| Field | Type |
|-------|------|
| uri | String |
| mimeType | String |
| name | String |
| title | String |
| description | String |

### ResourceResponse (from `resources/read`)
Contains resource content as text or blob. Exact structure depends on provider response.
