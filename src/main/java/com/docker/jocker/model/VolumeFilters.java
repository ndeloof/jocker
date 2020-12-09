package com.docker.jocker.model;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class VolumeFilters extends Filters {

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

    public VolumeFilters dangling(boolean dangling) {
        setDangling(dangling);
        return this;
    }

    public VolumeFilters driver(String driver) {
        setDriver(driver);
        return this;
    }

    public VolumeFilters label(String label) {
        setLabel(label);
        return this;
    }

    public VolumeFilters name(String name) {
        setName(name);
        return this;
    }

}
