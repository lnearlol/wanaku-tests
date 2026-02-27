# Tasks: Resources Tests

**Input**: Design documents from `/specs/002-resources-tests/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/resources-api.md

**Tests**: Tests ARE the deliverable for this feature — the module produces integration test classes.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Project Scaffolding)

**Purpose**: Create the `resources-tests` Maven module and directory structure

- [x] T001 Create `resources-tests/pom.xml` with test-common dependency, JUnit 5, AssertJ, and surefire plugin configuration (copy pattern from `http-capability-tests/pom.xml`)
- [x] T002 Add `<module>resources-tests</module>` to root `pom.xml`
- [x] T003 Create directory structure: `resources-tests/src/test/java/ai/wanaku/test/resources/` and `resources-tests/src/test/resources/`

---

## Phase 2: Foundational (test-common Extensions)

**Purpose**: Extend shared infrastructure with resource support. MUST complete before any user story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 [P] Add resource constants to `test-common/src/main/java/ai/wanaku/test/WanakuTestConstants.java`: `ROUTER_RESOURCES_PATH = "/api/v1/resources"`, `PROP_FILE_PROVIDER_JAR = "wanaku.test.file-provider.jar"`
- [x] T005 [P] Create `ResourceReference` model in `test-common/src/main/java/ai/wanaku/test/model/ResourceReference.java` with fields: name, location, type, description, mimeType, params (List of inner Param class), configurationURI, secretsURI, namespace, labels. Use `@JsonIgnoreProperties(ignoreUnknown = true)` (same pattern as `ToolInfo`)
- [x] T006 [P] Create `ResourceConfig` builder in `test-common/src/main/java/ai/wanaku/test/model/ResourceConfig.java` with builder pattern (same pattern as `HttpToolConfig`). Fields: name (required), location (required), type (default "file"), description, mimeType (default "text/plain"). Builder validates name and location are set. Build produces a Map suitable for JSON serialization to the `/expose` endpoint
- [x] T007 Extend `TestConfiguration` in `test-common/src/main/java/ai/wanaku/test/config/TestConfiguration.java`: add `fileProviderJarPath` field, getter, builder method. Extend `fromSystemProperties()` to auto-discover `wanaku-provider-file` JAR using `findJar(artifactsDir, "wanaku-provider-file")` and check `PROP_FILE_PROVIDER_JAR` system property
- [x] T008 Add resource methods to `RouterClient` in `test-common/src/main/java/ai/wanaku/test/client/RouterClient.java`: `exposeResource(ResourceConfig)` (POST /expose), `listResources()` (GET /list, returns `List<ResourceReference>`), `removeResource(String name)` (PUT /remove?resource={name}), `getResourceInfo(String name)` (list + filter by name, throws ResourceNotFoundException), `clearAllResources()` (list + remove each), `resourceExists(String name)` (list + filter), `exposeResourceWithConfig(ResourceConfig, String configurationData)` (POST /exposeWithPayload). All methods follow existing tool method patterns with WanakuResponse unwrapping. Add `ResourceExistsException` and `ResourceNotFoundException` inner exception classes
- [x] T009 Create `ResourceProviderManager` in `test-common/src/main/java/ai/wanaku/test/managers/ResourceProviderManager.java`. Extends `ProcessManager`. Has `prepare(routerHost, routerHttpPort, routerGrpcPort, oidcCredentials)` method that configures: `quarkus.http.port=0`, `quarkus.grpc.server.port={dynamic}`, `wanaku.service.registration.uri`, `wanaku.service.registration.delay-seconds=0`, OIDC client properties. `getJarPath()` returns `config.getFileProviderJarPath()`. `performHealthCheck()` waits for gRPC port. Pattern: copy `HttpCapabilityManager` and adapt

**Checkpoint**: Foundation ready — test-common has all resource infrastructure

---

## Phase 3: User Story 1 — Extend Common Test Infrastructure for Resources (Priority: P1) 🎯 MVP

**Goal**: Verify that the test-common extensions work: ResourceProviderManager starts the file provider, RouterClient can expose/list/remove resources, cleanup works.

**Independent Test**: Start Keycloak + Router + File Provider, expose a resource, list it, remove it, verify cleanup.

### Implementation for User Story 1

- [x] T010 [US1] Create `ResourceTestBase` in `resources-tests/src/test/java/ai/wanaku/test/resources/ResourceTestBase.java`. Extends `BaseIntegrationTest`. Adds: `ResourceProviderManager` field (test-scoped), `@BeforeEach` that starts file provider and waits for registration with Router (Awaitility polling `routerClient.isCapabilityRegistered("file")`), `@AfterEach` that calls `routerClient.clearAllResources()` then stops provider. Adds helper method `createTestFile(String filename, String content)` that creates file in `tempDataDir` and returns the absolute Path. Override `getLogProfile()` to return `"file-provider"`. Add `isFileProviderAvailable()` check method
- [x] T011 [US1] Create `ResourceRegistrationITCase` in `resources-tests/src/test/java/ai/wanaku/test/resources/ResourceRegistrationITCase.java`. Extends `ResourceTestBase`. Implement test: `shouldExposeAndListFileResource()` — creates a test file, builds `ResourceConfig`, calls `routerClient.exposeResource()`, calls `routerClient.listResources()`, asserts resource appears with correct name, type, mimeType. This validates FR-001, FR-002, FR-003, FR-004, FR-007, FR-008
- [x] T012 [US1] Add test to `ResourceRegistrationITCase`: `shouldRemoveResource()` — exposes a resource, verifies it exists, removes it via `routerClient.removeResource()`, verifies it no longer appears in list. Validates FR-010
- [x] T013 [US1] Add test to `ResourceRegistrationITCase`: `shouldCleanResourcesBetweenTests()` — exposes a resource, verifies list is not empty. The @AfterEach cleanup will clear it. A second test method `shouldStartWithEmptyResourceList()` verifies list is empty at start. Validates FR-013 and acceptance scenario 5 of US1

**Checkpoint**: User Story 1 complete — `mvn clean install -pl resources-tests` runs with basic expose/list/remove/cleanup working

---

## Phase 4: User Story 2 — Resource Registration and Listing (Priority: P2)

**Goal**: Comprehensive registration and listing tests including multiple resources, MCP protocol listing, and CLI operations.

**Independent Test**: Expose multiple resources, verify listing via REST and MCP, verify CLI operations.

### Implementation for User Story 2

- [x] T014 [US2] Add test to `ResourceRegistrationITCase`: `shouldListMultipleResources()` — exposes 3 file resources with different names/mimeTypes, lists all, asserts all 3 are returned with correct metadata. Validates US2 acceptance scenario 2
- [x] T015 [US2] Add test to `ResourceRegistrationITCase`: `shouldListResourcesViaMcp()` — exposes a file resource, uses `mcpClient.when().resourcesList().withAssert(page -> ...)` to verify the resource appears in the MCP response with correct uri and name. Validates FR-008 and US2 acceptance scenario 4
- [x] T016 [US2] Add test to `ResourceRegistrationITCase`: `shouldExposeResourceViaCli()` — uses `CLIExecutor` to run `wanaku resources expose --name test-cli-resource --location file:///tmp/test.txt --type file`. Then lists via REST API to verify it appears. Guard with `@EnabledIf("isCliAvailable")`. Validates FR-011 and US2 acceptance scenario 5

**Checkpoint**: User Stories 1 AND 2 complete — registration, listing (REST + MCP), removal, and CLI all working

---

## Phase 5: User Story 3 — Resource Reading (Priority: P3)

**Goal**: Verify end-to-end resource reading via MCP protocol — file content flows through Router → gRPC → File Provider → filesystem → response.

**Independent Test**: Create test files with known content, expose as resources, read via MCP, verify content matches.

### Implementation for User Story 3

- [x] T017 [US3] Create `ResourceReadingITCase` in `resources-tests/src/test/java/ai/wanaku/test/resources/ResourceReadingITCase.java`. Extends `ResourceTestBase`. Implement test: `shouldReadTextFileViaMcp()` — creates test file with content "Hello Wanaku Resources", exposes it, reads via `mcpClient.when().resourcesRead(uri).withAssert(...)`, asserts response contains the expected text content. Validates FR-009 and US3 acceptance scenario 1
- [x] T018 [US3] Add test to `ResourceReadingITCase`: `shouldReadJsonFileViaMcp()` — creates a JSON test file (`{"key": "value"}`), exposes with mimeType `application/json`, reads via MCP, asserts response contains valid JSON content. Validates US3 acceptance scenario 2
- [x] T019 [US3] Add test to `ResourceReadingITCase`: `shouldHandleNonExistentFile()` — exposes a resource pointing to a non-existent file path, attempts to read via MCP, asserts error response is returned. Validates FR-012 and US3 acceptance scenario 3

**Checkpoint**: User Stories 1, 2, AND 3 complete — full CRUD + MCP read working

---

## Phase 6: User Story 4 — Resource Expose with Configuration (Priority: P4)

**Goal**: Verify that resources can be exposed with configuration data via the `/exposeWithPayload` endpoint.

**Independent Test**: Expose a resource with configurationData, verify the resource is created and configurationURI is populated.

### Implementation for User Story 4

- [x] T020 [US4] Add test to `ResourceRegistrationITCase`: `shouldExposeResourceWithConfiguration()` — calls `routerClient.exposeResourceWithConfig(config, "some.property=value")`, verifies resource is created in list, checks that configurationURI field is populated in the returned/listed resource. Validates FR-014 and US4 acceptance scenarios

**Checkpoint**: User Stories 1–4 complete — all resource operations including provisioning working

---

## Phase 7: User Story 5 — Test Isolation Verification (Priority: P5)

**Goal**: Verify that resource tests are properly isolated — no state leaks between tests.

**Independent Test**: Run multiple tests that create different resources, verify each starts clean.

### Implementation for User Story 5

- [x] T021 [US5] The isolation tests are already covered by T013 (`shouldCleanResourcesBetweenTests` + `shouldStartWithEmptyResourceList`). Verify these tests pass when run in any order by running: `mvn clean install -pl resources-tests -Dsurefire.runOrder=random`. If additional isolation verification is needed, add explicit test `shouldNotSeeResourcesFromPreviousTest()` to `ResourceRegistrationITCase`

**Checkpoint**: All user stories complete — full test suite passes in any order

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and compatibility checks

- [x] T022 Run full project build `mvn clean install` from project root — verify both `http-capability-tests` and `resources-tests` pass (RouterClient extensions must not break existing tool methods, SC-008)
- [x] T023 Verify `resources-tests` runs independently: `mvn clean install -pl resources-tests` with only Docker and file provider JAR available (SC-001, SC-007)
- [x] T024 Verify test failure messages are actionable: temporarily break a test (wrong file path, missing JAR) and confirm error messages clearly identify the root cause (SC-003)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001-T003) — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational (Phase 2) completion
  - US1 (Phase 3): No dependencies on other stories — **this is the MVP**
  - US2 (Phase 4): Depends on US1 (uses same test class and base class)
  - US3 (Phase 5): Depends on US1 (uses ResourceTestBase); independent of US2
  - US4 (Phase 6): Depends on US1 (uses same test class); independent of US2/US3
  - US5 (Phase 7): Depends on US1 (verifies cleanup from US1 base class)
- **Polish (Phase 8)**: Depends on all user stories being complete

### Within Each User Story

- Models before services (T005/T006 before T008)
- Services before test base (T007/T008/T009 before T010)
- Test base before test classes (T010 before T011-T013)
- Core tests before advanced tests (T011 before T014-T016)

### Parallel Opportunities

- Phase 2: T004, T005, T006 can run in parallel (different files)
- Phase 2: T007, T008, T009 can run in parallel after T004-T006 (different files, but T007 depends on T004 for constants, T009 depends on T007 for config)
- Phase 4: T014, T015, T016 can run in parallel (different test methods, same file but independent)
- Phase 5: T017, T018, T019 can run in parallel (separate test methods)

---

## Parallel Example: Phase 2 (Foundational)

```
# These can run in parallel (different files):
T004: Add resource constants to WanakuTestConstants.java
T005: Create ResourceReference model
T006: Create ResourceConfig builder

# After above complete, these run sequentially:
T007: Extend TestConfiguration (depends on T004 for constant name)
T008: Add resource methods to RouterClient (depends on T004, T005, T006)
T009: Create ResourceProviderManager (depends on T007 for config)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T009)
3. Complete Phase 3: User Story 1 (T010-T013)
4. **STOP and VALIDATE**: `mvn clean install -pl resources-tests`
5. Verify: provider starts, resource exposed, listed, removed, cleaned

### Incremental Delivery

1. Setup + Foundational → infrastructure ready
2. Add US1 (T010-T013) → basic CRUD working → **MVP**
3. Add US2 (T014-T016) → MCP listing + CLI tests
4. Add US3 (T017-T019) → MCP resource reading (end-to-end)
5. Add US4 (T020) → provisioning/configuration
6. Add US5 (T021) → isolation verification
7. Polish (T022-T024) → full validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- McpTestClient does NOT need extension — McpAssured's `when().resourcesList()` and `when().resourcesRead(uri)` are already available on the `McpStreamableAssert` interface
- All test classes follow `*ITCase.java` naming convention (surefire plugin configuration)
- ResourceProviderManager follows HttpCapabilityManager pattern exactly — same `prepare()` → `start()` → `stop()` lifecycle
- RouterClient resource methods follow existing tool method patterns — WanakuResponse unwrapping, exception handling
