# Feature Specification: Resources Tests

**Feature Branch**: `002-resources-tests`
**Created**: 2026-02-19
**Status**: Draft
**Input**: User description: "Create test code for the resources tests. This module should test the Wanaku resource providers — services that give AI agents read-only access to data (files, S3, FTP). The existing test-common infrastructure should be reused and extended."

## Clarifications

### Session 2026-02-19

- Q: What resource provider to use for testing? → A: `wanaku-provider-file` (from wanaku-examples). Simplest provider, no external dependencies (no S3, no FTP server needed). Reads local files.
- Q: How are resources different from tools? → A: Tools perform processing (request/reply). Resources provide read-only data access. Different API endpoints, different MCP operations (`resources/list`, `resources/read` vs `tools/list`, `tools/call`).
- Q: What REST API endpoints exist for resources? → A: `/api/v1/resources/expose`, `/api/v1/resources/list`, `/api/v1/resources/remove`, `/api/v1/resources/update`, `/api/v1/resources/exposeWithPayload`. Different from tools API naming (`expose` vs `add`).
- Q: What is the ResourceReference data model? → A: Fields: location, type, name, description, mimeType, params, configurationURI, secretsURI, namespace, labels. More complex than ToolInfo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extend Common Test Infrastructure for Resources (Priority: P1)

As a test developer, I need the test-common module extended with resource management utilities (ResourceClient, ResourceProviderManager, resource models) so that I can write resource provider tests without duplicating infrastructure code.

**Why this priority**: The existing test-common handles tools only. Resources have different API endpoints, different data models, and require a resource provider process (analogous to HTTP capability for tools).

**Independent Test**: Can be tested by starting Keycloak, Router, and File Resource Provider, verifying health checks pass, exposing a resource, and confirming it appears in the resources list.

**Acceptance Scenarios**:

1. **Given** the test-common module, **When** ResourceProviderManager starts the wanaku-provider-file process, **Then** the provider registers with the Router and health check passes.

2. **Given** Router is running, **When** I expose a file resource via REST API, **Then** the resource appears in the resources list with correct name, location, type, and mimeType.

3. **Given** Router is running, **When** I list resources, **Then** I receive a list of ResourceReference objects with all fields populated.

4. **Given** the test-common module, **When** I call RouterClient.removeResource(name), **Then** the resource is removed from the Router.

5. **Given** a test completes, **When** @AfterEach runs, **Then** all resources are cleaned and the next test starts with empty state.

---

### User Story 2 - Resource Registration and Listing (Priority: P2)

As a test developer, I need to verify that file resources can be exposed to the Wanaku Router and listed via REST API and MCP protocol, so that I can confirm the file resource provider works correctly for basic resource management.

**Why this priority**: Resource expose and list are the fundamental operations that must work before resource reading can be tested.

**Independent Test**: Can be tested by exposing a file resource via REST API, listing resources via REST API and MCP, and verifying the resource appears with correct metadata.

**Acceptance Scenarios**:

1. **Given** Router and File Provider are running, **When** I expose a file resource via REST API with name, location, type, and mimeType, **Then** the resource appears in the resources list.

2. **Given** multiple resources are exposed, **When** I list all resources, **Then** all resources are returned with their metadata (name, location, type, mimeType, description).

3. **Given** a resource is exposed, **When** I remove the resource via REST API, **Then** it no longer appears in the resources list.

4. **Given** a resource is exposed, **When** I list resources via MCP protocol (`resources/list`), **Then** the resource appears in the MCP response.

5. **Given** Router and File Provider are running, **When** I expose a file resource via CLI command (`wanaku resources expose`), **Then** the resource appears in the resources list (CLI-specific validation test).

---

### User Story 3 - Resource Reading (Priority: P3)

As a test developer, I need to verify that file resources can be read via the MCP protocol and return correct file contents, so that I can confirm end-to-end resource provider functionality.

**Why this priority**: Resource reading is the primary use case for resource providers but depends on registration working correctly. This validates the complete data flow: MCP → Router → gRPC → File Provider → File System → Response.

**Independent Test**: Can be tested by creating a test file, exposing it as a resource, reading it via MCP protocol, and verifying the response matches the file contents.

**Acceptance Scenarios**:

1. **Given** a text file exists with known content, **When** I expose it as a resource and read it via MCP (`resources/read`), **Then** I receive the file content in the response.

2. **Given** a JSON file exists, **When** I expose it with mimeType `application/json` and read it via MCP, **Then** I receive valid JSON content.

3. **Given** a resource points to a non-existent file, **When** I attempt to read it via MCP, **Then** I receive an error response indicating the file was not found.

4. **Given** a resource is exposed, **When** I read it with the file provider running, **Then** the response contains the content as a string (the provider's coerceResponse converts files to strings).

---

### User Story 4 - Resource Expose with Configuration (Priority: P4)

As a test developer, I need to verify that resources can be exposed with configuration data (via `/exposeWithPayload`), so that I can confirm the provisioning system works for resource providers.

**Why this priority**: Some resource providers require configuration (e.g., S3 credentials, FTP connection settings). Even for the file provider, testing the payload mechanism validates Router-side provisioning logic.

**Independent Test**: Can be tested by exposing a resource with configurationData, verifying the resource is created, and confirming the configuration is passed to the provider.

**Acceptance Scenarios**:

1. **Given** Router and File Provider are running, **When** I expose a resource via `/exposeWithPayload` with configurationData, **Then** the resource is created and the configuration is stored.

2. **Given** a resource exposed with configuration, **When** I retrieve the resource info, **Then** the configurationURI field is populated.

---

### User Story 5 - Test Isolation Verification (Priority: P5)

As a test developer, I need to verify that resource tests are properly isolated and don't affect each other, so that tests can run in any order without flaky behavior.

**Why this priority**: Same as for tools — isolation is non-negotiable per the constitution.

**Independent Test**: Can be tested by running multiple tests that expose different resources, verifying each test starts with clean state.

**Acceptance Scenarios**:

1. **Given** Test A exposes resource "file-a" and completes, **When** Test B runs, **Then** resource "file-a" is not present in the resources list.

2. **Given** resources are cleared between tests, **When** a new test starts, **Then** no resources from previous tests are present.

---

### Edge Cases

- What happens when the File Provider process crashes mid-test? The framework must detect this and fail the test with a clear error.
- What happens when a resource is exposed with a duplicate name? The Router should return an appropriate error.
- How does the system handle resources with special characters in names or file paths?
- What happens when the file location path is relative vs absolute?
- What happens when the resource type doesn't match any registered provider? The read should fail with a clear error.

## Requirements *(mandatory)*

### Functional Requirements

#### Test-Common Extensions

- **FR-001**: The framework MUST provide a ResourceProviderManager that starts wanaku-provider-file as a local Java process with dynamic gRPC port, configured to register with the Router. Default lifecycle: test-scoped (fresh per test).
- **FR-002**: The framework MUST extend RouterClient (or provide a ResourceClient) with methods for resource operations: exposeResource, listResources, removeResource, getResourceInfo, clearAllResources, exposeResourceWithConfig.
- **FR-003**: The framework MUST provide a ResourceReference model class with fields: name, location, type, description, mimeType, params, configurationURI, secretsURI, namespace, labels.
- **FR-004**: The framework MUST provide a ResourceConfig model class for building resource expose requests.
- **FR-005**: The framework MUST support MCP resource operations (`resources/list`, `resources/read`) via McpAssured's native `resourcesList()` and `resourcesRead()` methods on the existing `McpTestClient.when()` chain. No McpTestClient extension is needed.
- **FR-006**: The framework MUST provide test file management utilities: create temporary test files with known content, clean up after tests.

#### Resources Tests Module

- **FR-007**: The test module MUST be able to expose file resources with the Wanaku Router via REST API.
- **FR-008**: The test module MUST be able to list exposed resources via REST API and MCP protocol.
- **FR-009**: The test module MUST be able to read file resources via MCP protocol and validate content matches the source file.
- **FR-010**: The test module MUST be able to remove resources from the Router.
- **FR-011**: The test module MUST include CLI-specific tests for resource operations (`wanaku resources expose`, `wanaku resources list`).
- **FR-012**: The test module MUST validate error handling for non-existent files.
- **FR-013**: The test module MUST clean all exposed resources between tests.
- **FR-014**: The test module MUST support exposing resources with configurationData via `/exposeWithPayload`.

### Key Entities

- **ResourceProviderManager**: Manages the wanaku-provider-file process lifecycle. Configures gRPC connection to Router and waits for registration. Lifecycle: test-scoped by default. Analogous to HttpCapabilityManager.
- **ResourceClient** (or RouterClient extension): REST API client for resource operations on the Wanaku Router. Methods: exposeResource, listResources, removeResource, getResourceInfo, clearAllResources, exposeResourceWithConfig.
- **ResourceReference**: Data model representing a resource in Wanaku. Fields: name, location, type, description, mimeType, params, configurationURI, secretsURI, namespace, labels.
- **ResourceConfig**: Builder/config object for creating resource expose requests. Fields: name, location, type, description, mimeType, params.
- **McpTestClient**: No extension needed. McpAssured natively provides `resourcesList()` and `resourcesRead()` on the `McpStreamableAssert` interface returned by `mcpClient.when()`.

### Assumptions

- Pre-built wanaku-provider-file JAR is available at `artifacts/wanaku-provider-file/quarkus-run.jar`.
- The file provider uses Quarkus fast-jar format (quarkus-app directory with quarkus-run.jar + lib/).
- File provider registers with Router via gRPC (same pattern as HTTP capability).
- Resource REST API uses `/api/v1/resources/` base path.
- Resource CLI commands use `wanaku resources expose/list/remove` pattern.
- Test files can be created in temporary directories managed by the framework.
- MCP protocol supports `resources/list` and `resources/read` operations via MCPAssured.

## Reuse from 001-http-capability-tests

### Reuse as-is
- KeycloakManager
- RouterManager
- ProcessManager (base class)
- PortUtils, HealthCheckUtils, LogUtils
- BaseIntegrationTest lifecycle pattern
- CLIExecutor, CLIResult
- WanakuTestConstants (extend with resource paths)
- OidcCredentials, TestConfiguration

### Extend
- **RouterClient**: Add resource methods (exposeResource, listResources, removeResource, etc.)
- **McpTestClient**: No changes needed — McpAssured natively supports `resourcesList()` and `resourcesRead()` on the `when()` chain
- **WanakuTestConstants**: Add ROUTER_RESOURCES_PATH = "/api/v1/resources"

### Create new
- **ResourceProviderManager**: New manager for file provider process (based on HttpCapabilityManager pattern)
- **ResourceReference**: New model class
- **ResourceConfig**: New builder/config class
- **ResourceTestBase**: New test base class extending BaseIntegrationTest with resource-specific setup

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Complete resources test suite runs with `mvn clean install -pl resources-tests` and no manual intervention.
- **SC-002**: All tests pass when run in any order.
- **SC-003**: Test failures provide actionable error messages identifying the root cause.
- **SC-004**: Adding a new resource test requires only creating a new test method and optional test files (no infrastructure code changes).
- **SC-005**: Framework correctly detects and reports missing prerequisites (File Provider JAR) before attempting to run tests.
- **SC-006**: No test data (temp files, resources) persists after the test suite completes.
- **SC-007**: Resource tests can run independently from HTTP capability tests.
- **SC-008**: RouterClient resource extensions work alongside existing tool methods without breaking 001 tests.
