package com.docker.jocker.model;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class NetworkFilters extends Filters {

    public void setDangling(boolean dangling) {
        add("dangling", String.valueOf(dangling));
    }

    public void setDriver(String driver) {
        add("driver", driver);
    }

    public void setLabel(String label) {
        add("label", label);
    }

    public void setName(String name) {
        add("name", name);
    }

    public void setId(String name) {
        add("id", name);
    }

    public void setScope(String scope) {
        add("scope", scope);
    }

    public void setType(String type) {
        add("type", type);
    }

    public NetworkFilters dangling(boolean dangling) {
        setDangling(dangling);
        return this;
    }

    public NetworkFilters driver(String driver) {
        setDriver(driver);
        return this;
    }

    public NetworkFilters label(String label) {
        setLabel(label);
        return this;
    }

    public NetworkFilters name(String name) {
        setName(name);
        return this;
    }

    public NetworkFilters id(String id) {
        setId(id);
        return this;
    }

    public NetworkFilters scope(String scope) {
        setScope(scope);
        return this;
    }

    public NetworkFilters type(String type) {
        setType(type);
        return this;
    }

}
