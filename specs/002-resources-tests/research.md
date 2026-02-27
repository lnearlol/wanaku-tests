# Research: Resources Tests

**Feature**: 002-resources-tests | **Date**: 2026-02-19

## R1: Resources REST API Endpoints

**Decision**: Use the following REST API endpoints on the Wanaku Router for resource management.

| Operation | Method | Path | Query Params | Body |
|-----------|--------|------|-------------|------|
| Expose resource | POST | `/api/v1/resources/expose` | — | `ResourceReference` JSON |
| Expose with config | POST | `/api/v1/resources/exposeWithPayload` | — | `ResourcePayload` JSON (contains resourceReference + configurationData + secretsData) |
| List resources | GET | `/api/v1/resources/list` | `labelFilter` (optional) | — |
| Remove resource | PUT | `/api/v1/resources/remove` | `resource={name}` | — |
| Update resource | POST | `/api/v1/resources/update` | — | `ResourceReference` JSON |

**Rationale**: These endpoints are defined in `ResourcesService.java` and implemented in `ResourcesResource.java` in the Wanaku Router. The naming differs from tools API (`expose` vs `add`, `resource=` vs `tool=`).

**Alternatives considered**: None — this is the actual API.

## R2: ResourceReference Data Model

**Decision**: ResourceReference has the following structure (from `ResourceReference.java` in wanaku-capabilities-java-sdk):

```
ResourceReference extends LabelsAwareEntity<String>
├── id: String (UUID, server-generated)
├── name: String (brief name)
├── location: String (URL or file path)
├── type: String (resource type — maps to provider, e.g., "file")
├── description: String (longer description)
├── mimeType: String (e.g., "text/plain", "application/json")
├── params: List<Param> (name-value pairs)
│   └── Param { name: String, value: String }
├── configurationURI: String (URI to non-sensitive config)
├── secretsURI: String (URI to sensitive credentials)
├── namespace: String (namespace scope)
└── labels: Map<String, String> (metadata for filtering)
```

**Rationale**: Directly matches the Wanaku source. The test model doesn't need all fields — `id` and `namespace` are server-managed. Test code will use: name, location, type, description, mimeType.

**Alternatives considered**: Could use the actual Wanaku SDK dependency, but the test framework intentionally avoids coupling to Wanaku implementation classes (same approach as ToolInfo — lightweight POJO with Jackson annotations).

## R3: McpAssured Resource Operations

**Decision**: Use McpAssured's built-in resource operations. Confirmed available in quarkus-mcp-server-test 1.8.1+:

- `resourcesList()` → returns `ResourcesPage` with `List<ResourceInfo> resources`
- `resourcesRead(String uri)` → returns `ResourceResponse` with content

**Usage pattern** (same as tools):
```java
mcpClient.when()
    .resourcesList()
    .withAssert(page -> assertThat(page.resources()).isNotEmpty())
    .send();

mcpClient.when()
    .resourcesRead("file:///path/to/file.txt")
    .withAssert(response -> assertThat(response).isNotNull())
    .send();
```

**Rationale**: McpAssured natively supports resource operations via `McpAssert` interface. `ResourcesPage` provides `findByUri(uri)` for targeted assertions. No custom MCP implementation needed.

**Alternatives considered**: Building custom WebSocket/HTTP MCP client — rejected because McpAssured already handles the protocol.

## R4: File Resource Provider

**Decision**: Use `wanaku-provider-file` from wanaku-examples as the test resource provider.

- **JAR location**: `artifacts/wanaku-provider-file/quarkus-run.jar` (Quarkus fast-jar format)
- **Registration**: Auto-registers with Router via REST API (same pattern as HTTP capability)
- **gRPC**: Exposes gRPC service for resource acquisition (ResourceAcquirer)
- **Process**: Runs as local Java process managed by `ResourceProviderManager`
- **Configuration properties**:
  - `quarkus.grpc.server.port` — gRPC port (dynamic)
  - `quarkus.http.port` — HTTP port (set to 0, disabled)
  - `wanaku.service.registration.uri` — Router registration URL
  - `wanaku.service.registration.delay-seconds` — Set to 0 for fast test startup

**Rationale**: File provider is the simplest resource provider — reads local files, no external dependencies (no S3 bucket, no FTP server). Same process management pattern as HttpCapabilityManager.

**Alternatives considered**: S3 provider (needs LocalStack), FTP provider (needs FTP server container) — both add complexity without testing additional framework functionality.

## R5: Resource Provider Registration Detection

**Decision**: Detect file provider registration by checking capabilities list. The file provider registers as `serviceType: "RESOURCES"` with `serviceName: "file"`.

Use existing `RouterClient.isCapabilityRegistered()` method — it already queries `GET /api/v1/capabilities` and searches by `serviceName`. The file provider registers with `serviceName = "file"`.

**Rationale**: Same pattern as HTTP capability detection. Awaitility polling until capability appears.

**Alternatives considered**: Health-check the gRPC port only — insufficient because port listening doesn't mean registration with Router is complete.

## R6: Test File Management

**Decision**: Use JUnit 5 `@TempDir` or `Files.createTempDirectory()` for test files. Create known-content files in @BeforeEach, clean up in @AfterEach.

Test files:
- `test-file.txt` — plain text with known content ("Hello Wanaku Resources")
- `test-data.json` — JSON file with known structure

**Rationale**: Temporary files avoid polluting the working directory and are automatically cleaned up. Known content enables exact assertion matching.

**Alternatives considered**: Files in `src/test/resources/` — works for static content but doesn't test dynamic file paths. Use both: static files for content validation, temp files for isolation testing.

## R7: TestConfiguration Extension

**Decision**: Add `fileProviderJarPath` to `TestConfiguration` alongside existing `httpToolServiceJarPath`.

- System property: `wanaku.test.file-provider.jar`
- Auto-discovery: `findJar(artifactsDir, "wanaku-provider-file")`
- Constant: `PROP_FILE_PROVIDER_JAR = "wanaku.test.file-provider.jar"`

**Rationale**: Same pattern as HTTP service JAR path. Allows override via system property or auto-discovery from artifacts directory.

**Alternatives considered**: Hardcode path — rejected, violates constitution principle IV (Configuration Flexibility).

## R8: Resource-specific Base Test Class

**Decision**: Create `ResourceTestBase` extending `BaseIntegrationTest`. Adds:
- `ResourceProviderManager` lifecycle (test-scoped)
- Resource cleanup in @AfterEach (`clearAllResources()`)
- Temp file management
- Helper methods: `exposeTestFile()`, `createTestFile()`

**Rationale**: Resources need different per-test setup than tools (file provider instead of HTTP capability, temp file management). A dedicated base class avoids cluttering `BaseIntegrationTest` with resource-specific logic while reusing suite-scoped infrastructure (Keycloak, Router).

**Alternatives considered**: Modify `BaseIntegrationTest` directly — rejected because it would couple tool and resource lifecycle, making the base class unwieldy and violating single responsibility.
