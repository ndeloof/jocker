package it.dockins.jocker.model;

import com.google.gson.annotations.SerializedName;
import io.dockins.jocker.model.ContainerConfig;
import io.dockins.jocker.model.ContainerConfigVolumes;
import io.dockins.jocker.model.HealthConfig;
import io.dockins.jocker.model.HostConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ContainerSpec extends ContainerConfig {

    @SerializedName("HostConfig")
    private HostConfig hostConfig;

    @SerializedName("NetworkingConfig")
    private NetworkingConfig networkingConfig;

    @SerializedName("Cmd")
    private List<String> cmd;

    public HostConfig getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(HostConfig hostConfig) {
        this.hostConfig = hostConfig;
    }

    public ContainerSpec hostConfig(HostConfig hostConfig) {
        this.hostConfig = hostConfig;
        return this;
    }

    public NetworkingConfig getNetworkingConfig() {
        return networkingConfig;
    }

    public void setNetworkingConfig(NetworkingConfig networkingConfig) {
        this.networkingConfig = networkingConfig;
    }

    public ContainerSpec networkingConfig(NetworkingConfig networkingConfig) {
        this.networkingConfig = networkingConfig;
        return this;
    }

    public List<String> getCmd() {
        return cmd;
    }


    public ContainerSpec cmd(String ... cmd) {
        for (String s : cmd) {
            addCmdItem(s);
        }
        return this;
    }

    public ContainerSpec cmd(List<String> cmd) {
        this.cmd = cmd;
        return this;
    }

    public ContainerSpec addCmdItem(String cmdItem) {
        if (this.cmd == null) {
            this.cmd = new ArrayList<String>();
        }
        this.cmd.add(cmdItem);
        return this;
    }


    @Override
    public ContainerSpec hostname(String hostname) {
        return (ContainerSpec) super.hostname(hostname);
    }

    @Override
    public ContainerSpec domainname(String domainname) {
        return (ContainerSpec) super.domainname(domainname);
    }

    @Override
    public ContainerSpec attachStdin(Boolean attachStdin) {
        return (ContainerSpec) super.attachStdin(attachStdin);
    }

    @Override
    public ContainerSpec attachStdout(Boolean attachStdout) {
        return (ContainerSpec) super.attachStdout(attachStdout);
    }

    @Override
    public ContainerSpec attachStderr(Boolean attachStderr) {
        return (ContainerSpec) super.attachStderr(attachStderr);
    }

    @Override
    public ContainerSpec exposedPorts(Map<String, Object> exposedPorts) {
        return (ContainerSpec) super.exposedPorts(exposedPorts);
    }

    @Override
    public ContainerSpec putExposedPortsItem(String key, Object exposedPortsItem) {
        return (ContainerSpec) super.putExposedPortsItem(key, exposedPortsItem);
    }

    @Override
    public ContainerSpec openStdin(Boolean openStdin) {
        return (ContainerSpec) super.openStdin(openStdin);
    }

    @Override
    public ContainerSpec env(List<String> env) {
        return (ContainerSpec) super.env(env);
    }

    @Override
    public ContainerSpec addEnvItem(String envItem) {
        return (ContainerSpec) super.addEnvItem(envItem);
    }

    @Override
    public ContainerSpec healthcheck(HealthConfig healthcheck) {
        return (ContainerSpec) super.healthcheck(healthcheck);
    }

    @Override
    public ContainerSpec argsEscaped(Boolean argsEscaped) {
        return (ContainerSpec) super.argsEscaped(argsEscaped);
    }

    @Override
    public ContainerSpec image(String image) {
        return (ContainerSpec) super.image(image);
    }

    @Override
    public ContainerSpec volumes(ContainerConfigVolumes volumes) {
        return (ContainerSpec) super.volumes(volumes);
    }

    @Override
    public ContainerSpec networkDisabled(Boolean networkDisabled) {
        return (ContainerSpec) super.networkDisabled(networkDisabled);
    }

    @Override
    public ContainerSpec macAddress(String macAddress) {
        return (ContainerSpec) super.macAddress(macAddress);
    }

    @Override
    public ContainerSpec onBuild(List<String> onBuild) {
        return (ContainerSpec) super.onBuild(onBuild);
    }

    @Override
    public ContainerSpec addOnBuildItem(String onBuildItem) {
        return (ContainerSpec) super.addOnBuildItem(onBuildItem);
    }

    @Override
    public ContainerSpec labels(Map<String, String> labels) {
        return (ContainerSpec) super.labels(labels);
    }

    @Override
    public ContainerSpec putLabelsItem(String key, String labelsItem) {
        return (ContainerSpec) super.putLabelsItem(key, labelsItem);
    }

    @Override
    public ContainerSpec addShellItem(String shellItem) {
        return (ContainerSpec) super.addShellItem(shellItem);
    }
}
