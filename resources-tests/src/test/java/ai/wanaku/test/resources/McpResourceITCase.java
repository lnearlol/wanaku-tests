package ai.wanaku.test.resources;

import java.nio.file.Path;
import io.quarkus.test.junit.QuarkusTest;
import ai.wanaku.test.model.ResourceConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for resource operations via MCP protocol.
 * MCP client availability is asserted (not assumed) — tests fail if MCP is unavailable.
 */
@QuarkusTest
class McpResourceITCase extends ResourceTestBase {

    @BeforeEach
    void checkInfrastructureAvailable() {
        assertThat(isRouterAvailable()).as("Router must be available").isTrue();
        assertThat(isFileProviderAvailable())
                .as("File provider must be available")
                .isTrue();
        assertThat(isMcpClientAvailable()).as("MCP client must be available").isTrue();
    }

    @DisplayName("Expose a resource and verify it appears via MCP resourcesList")
    @Test
    void shouldListResourcesViaMcp() throws Exception {
        // Given
        Path testFile = createTestFile("test-mcp-list.txt", "MCP list test");
        ResourceConfig config = ResourceConfig.builder()
                .name("mcp-list-resource")
                .location(testFile.toUri().toString())
                .mimeType("text/plain")
                .build();

        routerClient.exposeResource(config);

        // When / Then
        mcpClient
                .when()
                .resourcesList()
                .withAssert(page -> {
                    assertThat(page.resources()).isNotEmpty();
                    assertThat(page.resources()).anyMatch(r -> r.name().equals("mcp-list-resource"));
                })
                .send();
    }

    @DisplayName("Read a text file resource via MCP and verify content matches")
    @Test
    void shouldReadTextFileViaMcp() throws Exception {
        // Given
        Path testFile = createTestFile("read-test.txt", "Hello Wanaku Resources");
        String fileUri = testFile.toUri().toString();

        ResourceConfig config = ResourceConfig.builder()
                .name("readable-text-resource")
                .location(fileUri)
                .mimeType("text/plain")
                .build();

        routerClient.exposeResource(config);

        // When / Then
        mcpClient
                .when()
                .resourcesRead(fileUri)
                .withAssert(response -> {
                    assertThat(response.contents()).isNotEmpty();
                    String text = response.contents().get(0).asText().text();
                    assertThat(text).contains("Hello Wanaku Resources");
                })
                .send();
    }

    @DisplayName("Read a resource pointing to a non-existent file and verify error is returned")
    @Test
    void shouldHandleNonExistentFile() throws Exception {
        // Given
        String nonExistentUri = "file:///tmp/wanaku-does-not-exist-" + System.nanoTime() + ".txt";

        ResourceConfig config = ResourceConfig.builder()
                .name("missing-file-resource")
                .location(nonExistentUri)
                .mimeType("text/plain")
                .build();

        routerClient.exposeResource(config);

        // When / Then — reading a non-existent file should return empty or error content
        mcpClient
                .when()
                .resourcesRead(nonExistentUri)
                .withAssert(response -> {
                    // Provider returns empty contents for non-existent files
                    assertThat(response.contents()).isEmpty();
                })
                .send();
    }
}
