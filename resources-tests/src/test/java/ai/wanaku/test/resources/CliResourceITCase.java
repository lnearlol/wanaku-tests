package ai.wanaku.test.resources;

import java.nio.file.Path;
import io.quarkus.test.junit.QuarkusTest;
import ai.wanaku.test.client.CLIExecutor;
import ai.wanaku.test.client.CLIResult;
import ai.wanaku.test.model.ResourceConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for resource operations via CLI.
 * CLI availability is asserted (not assumed) — tests fail if CLI is unavailable.
 */
@QuarkusTest
class CliResourceITCase extends ResourceTestBase {

    @BeforeEach
    void checkInfrastructureAvailable() {
        assertThat(isRouterAvailable()).as("Router must be available").isTrue();
        assertThat(isFileProviderAvailable())
                .as("File provider must be available")
                .isTrue();
    }

    @DisplayName("Expose a resource via CLI and verify it appears in the list")
    @Test
    void shouldExposeResourceViaCli() throws Exception {
        CLIExecutor cliExecutor = CLIExecutor.createDefault();
        assertThat(cliExecutor.isAvailable()).as("CLI must be available").isTrue();

        // Given
        String routerHost = routerManager.getBaseUrl();

        // When
        CLIResult result = cliExecutor.execute(
                "resources",
                "expose",
                "--host",
                routerHost,
                "--name",
                "test-cli-resource",
                "--description",
                "CLI test resource",
                "--location",
                "file:///tmp/test.txt",
                "--type",
                "file");

        // Then
        assertThat(result.isSuccess())
                .as("CLI command should succeed: %s", result.getCombinedOutput())
                .isTrue();

        assertThat(routerClient.resourceExists("test-cli-resource")).isTrue();
    }

    @DisplayName("Remove a resource via CLI and verify it no longer exists")
    @Test
    void shouldRemoveResourceViaCli() throws Exception {
        CLIExecutor cliExecutor = CLIExecutor.createDefault();
        assertThat(cliExecutor.isAvailable()).as("CLI must be available").isTrue();

        // Given - expose a resource first via REST
        Path testFile = createTestFile("test-cli-remove.txt", "CLI remove test");
        ResourceConfig config = ResourceConfig.builder()
                .name("cli-remove-resource")
                .location(testFile.toUri().toString())
                .build();

        routerClient.exposeResource(config);
        assertThat(routerClient.resourceExists("cli-remove-resource")).isTrue();

        // When
        String routerHost = routerManager.getBaseUrl();
        CLIResult result =
                cliExecutor.execute("resources", "remove", "--host", routerHost, "--name", "cli-remove-resource");

        // Then
        assertThat(result.isSuccess())
                .as("CLI command should succeed: %s", result.getCombinedOutput())
                .isTrue();
        assertThat(routerClient.resourceExists("cli-remove-resource")).isFalse();
    }
}
