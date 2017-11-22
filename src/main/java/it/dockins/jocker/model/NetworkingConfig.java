package it.dockins.jocker.model;

import com.google.gson.annotations.SerializedName;
import io.dockins.jocker.model.EndpointSettings;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class NetworkingConfig {

    @SerializedName("EndpointsConfig")
    private EndpointSettings endpointsConfig;

    public EndpointSettings getEndpointsConfig() {
        return endpointsConfig;
    }

    public void setEndpointsConfig(EndpointSettings endpointsConfig) {
        this.endpointsConfig = endpointsConfig;
    }

    public NetworkingConfig endpointsConfig(EndpointSettings endpointsConfig) {
        this.endpointsConfig = endpointsConfig;
        return this;
    }
}
