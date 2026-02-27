# Quickstart: Resources Tests

**Feature**: 002-resources-tests | **Date**: 2026-02-19

## Prerequisites

1. Docker running (for Keycloak Testcontainer)
2. Java 21
3. Pre-built Wanaku artifacts in `artifacts/` directory:
   - `artifacts/wanaku-router/quarkus-run.jar` — Wanaku Router
   - `artifacts/wanaku-provider-file/quarkus-run.jar` — File Resource Provider

## Build & Run

```bash
# Run all tests (from project root)
mvn clean install

# Run only resources tests
mvn clean install -pl resources-tests

# With custom artifacts directory
mvn clean install -pl resources-tests -Dwanaku.test.artifacts.dir=../artifacts
```

## Key Classes

### test-common (shared infrastructure)

| Class | Purpose |
|-------|---------|
| `ResourceProviderManager` | Manages wanaku-provider-file Java process lifecycle |
| `RouterClient` (extended) | REST client with resource methods: `exposeResource()`, `listResources()`, `removeResource()`, `clearAllResources()` |
| `McpTestClient` (extended) | MCP client — `when().resourcesList()`, `when().resourcesRead(uri)` |
| `ResourceReference` | Data model for resource API responses |
| `ResourceConfig` | Builder for resource expose requests |
| `TestConfiguration` (extended) | Adds `fileProviderJarPath` |
| `WanakuTestConstants` (extended) | Adds `ROUTER_RESOURCES_PATH` |

### resources-tests (test module)

| Class | Purpose |
|-------|---------|
| `ResourceTestBase` | Base class with file provider lifecycle, temp file helpers |
| `ResourceRegistrationITCase` | Tests: expose, list, remove, CLI operations |
| `ResourceReadingITCase` | Tests: MCP resources/read, file content validation |

## Test Lifecycle

```
@BeforeAll (suite-scoped, from BaseIntegrationTest)
├── Create temp directory
├── Load TestConfiguration
├── Start Keycloak (Testcontainer)
└── Start Router (Java process)

@BeforeEach (test-scoped, from ResourceTestBase)
├── Create RouterClient + McpTestClient
├── Start ResourceProviderManager (wanaku-provider-file)
├── Wait for file provider registration with Router
└── Create test files in temp directory

@Test
└── Execute test scenario

@AfterEach (test-scoped, from ResourceTestBase)
├── Clear all resources from Router
├── Stop ResourceProviderManager
├── Disconnect McpTestClient
└── Clean up test files

@AfterAll (suite-scoped, from BaseIntegrationTest)
├── Stop Router
├── Stop Keycloak
└── Delete temp directory
```

## Writing a New Test

```java
class MyResourceITCase extends ResourceTestBase {

    @Test
    void shouldExposeAndListResource() {
        // Expose a file resource
        ResourceConfig config = ResourceConfig.builder()
                .name("my-test-file")
                .location("file://" + createTestFile("hello.txt", "Hello World"))
                .type("file")
                .mimeType("text/plain")
                .build();

        routerClient.exposeResource(config);

        // Verify it appears in the list
        List<ResourceReference> resources = routerClient.listResources();
        assertThat(resources)
                .extracting(ResourceReference::getName)
                .contains("my-test-file");
    }

    @Test
    void shouldReadResourceViaMcp() {
        // Create and expose a file
        Path testFile = createTestFile("data.txt", "Expected content");
        ResourceConfig config = ResourceConfig.builder()
                .name("readable-file")
                .location("file://" + testFile)
                .type("file")
                .mimeType("text/plain")
                .build();

        routerClient.exposeResource(config);

        // Read via MCP
        mcpClient.when()
                .resourcesRead("file://" + testFile)
                .withAssert(response -> {
                    // Verify content matches
                })
                .send();
    }
}
```

## Configuration Properties

| System Property | Default | Description |
|----------------|---------|-------------|
| `wanaku.test.artifacts.dir` | `artifacts` | Path to pre-built JARs |
| `wanaku.test.router.jar` | auto-discovered | Router JAR path |
| `wanaku.test.file-provider.jar` | auto-discovered | File provider JAR path |
| `wanaku.test.cli.path` | `wanaku` | CLI executable path |
| `wanaku.test.timeout` | `60` | Default timeout in seconds |
