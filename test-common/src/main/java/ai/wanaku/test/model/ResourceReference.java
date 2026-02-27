package ai.wanaku.test.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Information about a registered resource (returned from Router API).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceReference {

    private String name;
    private String type;
    private String mimeType;
    private String configurationURI;

    public ResourceReference() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getConfigurationURI() {
        return configurationURI;
    }

    public void setConfigurationURI(String configurationURI) {
        this.configurationURI = configurationURI;
    }

    @Override
    public String toString() {
        return "ResourceReference{name='" + name + "', type='" + type + "'}";
    }
}
