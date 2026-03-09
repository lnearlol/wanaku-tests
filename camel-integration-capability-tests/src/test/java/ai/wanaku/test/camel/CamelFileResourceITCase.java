package ai.wanaku.test.camel;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.quarkus.test.junit.QuarkusTest;
import ai.wanaku.test.model.ResourceReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests for CIC with file-reading Camel routes exposed as MCP resources.
 * One fixture ({@code file-resource/}) with a file route and resource definition.
 * Uses {@code ${FILE_DIR}} and {@code ${FILE_NAME}} placeholders substituted at runtime.
 *
 * <p>CIC registers resources with URI format: {@code {serviceName}://{resourceName}}.
 *
 * <p>Covers: list, read, non-existent file, DataStore loading.
 */
@QuarkusTest
class CamelFileResourceITCase extends CamelCapabilityTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CamelFileResourceITCase.class);

    private static final String SERVICE_NAME = "file-resource-svc";
    private static final String RESOURCE_NAME = "test-file-resource";
    private static final String RESOURCE_URI = SERVICE_NAME + "://" + RESOURCE_NAME;

    @BeforeEach
    void assumeInfrastructureAvailable() {
        assumeThat(isRouterAvailable()).as("Router must be available").isTrue();
        assumeThat(isCamelCapabilityAvailable()).as("CIC JAR must be available").isTrue();
        assumeThat(isMcpClientAvailable()).as("MCP client must be connected").isTrue();
    }

    @DisplayName("Expose a file resource via CIC and verify it appears via MCP and REST API")
    @Test
    void shouldListFileResourceViaMcp() throws Exception {
        Path testFile = createTestFile("test-list.txt", "Content for listing test");
        startFileResourceCapability(testFile);

        mcpClient
                .when()
                .resourcesList()
                .withAssert(page -> {
                    assertThat(page.resources()).isNotEmpty();
                    assertThat(page.resources()).anyMatch(r -> RESOURCE_NAME.equals(r.name()));
                })
                .send();

        List<ResourceReference> resources = routerClient.listResources();
        assertThat(resources).anyMatch(r -> RESOURCE_NAME.equals(r.getName()));
    }

    @DisplayName("Read a file resource via MCP and verify content matches source file")
    @Test
    void shouldReadFileResourceViaMcp() throws Exception {
        Path testFile = createTestFile("test-read.txt", "Hello Wanaku from CIC resource");
        startFileResourceCapability(testFile);

        mcpClient
                .when()
                .resourcesRead(RESOURCE_URI)
                .withAssert(response -> {
                    LOG.debug("Resource read response: {}", response.contents());
                    assertThat(response.contents()).isNotEmpty();
                    assertThat(response.contents().get(0).asText().text()).contains("Hello Wanaku from CIC resource");
                })
                .send();
    }

    @DisplayName("Read resource pointing to non-existent file and verify empty or error response")
    @Test
    void shouldHandleNonExistentFileResource() throws Exception {
        Path fakeFile = tempDataDir.resolve("nonexistent-" + System.nanoTime() + ".txt");
        startCapability(
                SERVICE_NAME,
                "file-resource",
                Map.of(
                        "FILE_DIR", fakeFile.getParent().toString(),
                        "FILE_NAME", fakeFile.getFileName().toString()));

        mcpClient
                .when()
                .resourcesRead(RESOURCE_URI)
                .withAssert(response -> {
                    LOG.debug("Non-existent file response: {}", response.contents());
                    assertThat(response.contents()).isEmpty();
                })
                .send();
    }

    @DisplayName("Load file resource config from Data Store and verify it works")
    @Test
    void shouldLoadFileResourceFromDataStore() throws Exception {
        assumeThat(isDataStoreAvailable())
                .as("Data Store API must be available")
                .isTrue();

        Path testFile = createTestFile("test-ds.txt", "DataStore resource content");

        String routesContent = readFixtureFromClasspath("file-resource/routes.camel.yaml")
                .replace("${FILE_DIR}", testFile.getParent().toString())
                .replace("${FILE_NAME}", testFile.getFileName().toString());
        String rulesContent = readFixtureFromClasspath("file-resource/rules.yaml");

        dataStoreClient.upload("test-res-routes.camel.yaml", routesContent);
        dataStoreClient.upload("test-res-rules.yaml", rulesContent);

        String dsServiceName = "ds-resource-svc";
        String dsResourceUri = dsServiceName + "://" + RESOURCE_NAME;

        startCapabilityFromDataStore(
                dsServiceName, "datastore://test-res-routes.camel.yaml", "datastore://test-res-rules.yaml", null);

        mcpClient
                .when()
                .resourcesRead(dsResourceUri)
                .withAssert(response -> {
                    assertThat(response.contents()).isNotEmpty();
                    assertThat(response.contents().get(0).asText().text()).contains("DataStore resource content");
                })
                .send();
    }

    // -- Helpers --

    private void startFileResourceCapability(Path testFile) throws Exception {
        startCapability(
                SERVICE_NAME,
                "file-resource",
                Map.of(
                        "FILE_DIR", testFile.getParent().toString(),
                        "FILE_NAME", testFile.getFileName().toString()));
    }

    private Path createTestFile(String filename, String content) throws Exception {
        Path file = tempDataDir.resolve(filename);
        Files.writeString(file, content);
        LOG.debug("Created test file: {}", file);
        return file;
    }

    private String readFixtureFromClasspath(String relativePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + relativePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture not found on classpath: fixtures/" + relativePath);
            }
            return new String(is.readAllBytes());
        }
    }
}
