package ai.wanaku.test.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for exposing a resource via the Router REST API.
 * Uses builder pattern (same approach as {@link HttpToolConfig}).
 */
public class ResourceConfig {

    private String name;
    private String location;
    private String type = "file";
    private String description = "Test resource";
    private String mimeType = "text/plain";

    private ResourceConfig() {}

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /**
     * Converts this config to a Map suitable for JSON serialization
     * to the /api/v1/resources/expose endpoint.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("location", location);
        map.put("type", type);
        map.put("mimeType", mimeType);
        map.put("description", description);
        return map;
    }

    public static class Builder {

        private final ResourceConfig config = new ResourceConfig();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder location(String location) {
            config.location = location;
            return this;
        }

        public Builder type(String type) {
            config.type = type;
            return this;
        }

        public Builder description(String description) {
            config.description = description;
            return this;
        }

        public Builder mimeType(String mimeType) {
            config.mimeType = mimeType;
            return this;
        }

        public ResourceConfig build() {
            if (config.name == null || config.name.isEmpty()) {
                throw new IllegalStateException("Resource name is required");
            }
            if (config.location == null || config.location.isEmpty()) {
                throw new IllegalStateException("Resource location is required");
            }
            return config;
        }
    }
}
