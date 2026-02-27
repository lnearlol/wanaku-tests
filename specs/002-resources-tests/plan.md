# Implementation Plan: Resources Tests

**Branch**: `002-resources-tests` | **Date**: 2026-02-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-resources-tests/spec.md`

## Summary

Extend the Wanaku test framework to support resource provider testing. The `test-common` module gets new infrastructure: `ResourceProviderManager` (manages wanaku-provider-file process), resource methods on `RouterClient`, resource MCP operations on `McpTestClient`, and resource data models (`ResourceReference`, `ResourceConfig`). A new `resources-tests` Maven module validates resource expose/list/remove/read operations against the file resource provider.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: JUnit 5, Testcontainers (Keycloak), McpAssured (quarkus-mcp-server-test 1.8.1), Jackson, Awaitility, AssertJ
**Storage**: N/A (tests use temporary files on filesystem)
**Testing**: JUnit 5 with maven-surefire-plugin (`*ITCase.java` pattern)
**Target Platform**: macOS/Linux (local Java processes + Docker containers)
**Project Type**: Multi-module Maven project
**Performance Goals**: Full test suite completes within 5 minutes
**Constraints**: No external dependencies beyond Docker and pre-built Wanaku JARs
**Scale/Scope**: ~10-15 test methods across 2-3 test classes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hybrid Execution Model | ✅ PASS | Keycloak via Testcontainers, wanaku-provider-file as local Java process, dynamic ports |
| II. Test Isolation | ✅ PASS | Per-test resource provider lifecycle, `clearAllResources()` in @AfterEach, temp file cleanup |
| III. Fail-Fast with Clear Errors | ✅ PASS | Health checks for provider, missing JAR detection, log capture to target/ |
| IV. Configuration Flexibility | ✅ PASS | File provider JAR path via system property, test files in src/test/resources/ |
| V. Performance-Aware Resources | ✅ PASS | Keycloak + Router shared (suite-scoped), provider test-scoped |
| VI. Layered Isolation | ✅ PASS | Suite: Keycloak+Router, Test: ResourceProvider+McpClient+RouterClient |

No violations. All constitution gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/002-resources-tests/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── resources-api.md # REST API contract for resources
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
test-common/src/main/java/ai/wanaku/test/
├── WanakuTestConstants.java          # EXTEND: add ROUTER_RESOURCES_PATH
├── config/
│   └── TestConfiguration.java        # EXTEND: add fileProviderJarPath
├── managers/
│   └── ResourceProviderManager.java  # NEW: manages wanaku-provider-file process
├── client/
│   ├── RouterClient.java             # EXTEND: add resource methods
│   └── McpTestClient.java            # NO CHANGES (McpAssured natively supports resourcesList(), resourcesRead())
├── model/
│   ├── ResourceReference.java        # NEW: resource data model
│   └── ResourceConfig.java           # NEW: resource expose config builder
└── base/
    └── BaseIntegrationTest.java      # NO CHANGES (resource tests use own base)

resources-tests/
├── pom.xml                           # NEW: Maven module
└── src/test/java/ai/wanaku/test/resources/
    ├── ResourceTestBase.java         # NEW: base class with resource provider lifecycle
    ├── ResourceRegistrationITCase.java  # NEW: expose/list/remove tests
    └── ResourceReadingITCase.java    # NEW: MCP resource read tests
```

**Structure Decision**: Multi-module Maven project (existing pattern). New `resources-tests` module follows same structure as `http-capability-tests`. Shared infrastructure in `test-common` is extended, not duplicated.

## Complexity Tracking

No violations to justify.
