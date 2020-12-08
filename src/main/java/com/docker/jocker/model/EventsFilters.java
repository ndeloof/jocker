package com.docker.jocker.model;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class EventsFilters extends Filters {

    public void setConfig(String config) {
        add("config", config);
        add("type", "config");
    }

    public EventsFilters config(String config) {
        setConfig(config);
        return this;
    }

    public void setContainer(String container) {
        add("container", container);
        add("type", "container");
    }

    public EventsFilters container(String container) {
        setContainer(container);
        return this;
    }

    public void setDaemon(String deamon) {
        add("deamon", deamon);
        add("type", "daemon");
    }

    public EventsFilters daemon(String daemon) {
        setDaemon(daemon);
        return this;
    }

    public void setEvent(String event) {
        add("event", event);
    }

    public EventsFilters event(String event) {
        setEvent(event);
        return this;
    }

    public void setImage(String image) {
        add("image", image);
        add("type", "image");
    }

    public EventsFilters image(String image) {
        setImage(image);
        return this;
    }

    public void setLabel(String label) {
        add("label", label);
    }

    public EventsFilters label(String label) {
        setLabel(label);
        return this;
    }

    public void setNetwork(String network) {
        add("network", network);
        add("type", "network");
    }

    public EventsFilters network(String network) {
        setNetwork(network);
        return this;
    }

    public void setNode(String node) {
        add("node", node);
        add("type", "node");
    }

    public EventsFilters node(String node) {
        setNode(node);
        return this;
    }

    public void setScope(String scope) {
        add("scope", scope);
    }

    public EventsFilters scope(String scope) {
        setScope(scope);
        return this;
    }

    public void setSecret(String secret) {
        add("secret", secret);
        add("type", "secret");
    }

    public EventsFilters secret(String secret) {
        setSecret(secret);
        return this;
    }

    public void setService(String service) {
        add("service", service);
        add("type", "service");
    }

    public EventsFilters service(String service) {
        setService(service);
        return this;
    }

    public void setVolume(String volume) {
        add("volume", volume);
        add("type", "volume");
    }

    public EventsFilters volume(String volume) {
        setVolume(volume);
        return this;
    }
}
