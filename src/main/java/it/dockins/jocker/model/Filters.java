package it.dockins.jocker.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Filters {

    @SerializedName("ancestor")
    private String[] ancestor;

    @SerializedName("before")
    private String[] before;

    @SerializedName("expose")
    private String[] expose;

    @SerializedName("exited")
    private String[] exited;

    @SerializedName("health")
    private String[] health;

    @SerializedName("id")
    private String[] id;

    @SerializedName("isolation")
    private String[] isolation;

    @SerializedName("is-task")
    private String[] isTask;

    @SerializedName("label")
    private String[] label;

    @SerializedName("name")
    private String[] name;

    @SerializedName("network")
    private String[] network;

    @SerializedName("publish")
    private String[] publish;

    @SerializedName("since")
    private String[] since;

    @SerializedName("status")
    private String[] status;

    @SerializedName("volume")
    private String[] volume;

    public String[] getAncestor() {
        return ancestor;
    }

    public void setAncestor(String[] ancestor) {
        this.ancestor = ancestor;
    }

    public String[] getBefore() {
        return before;
    }

    public void setBefore(String[] before) {
        this.before = before;
    }

    public String[] getExpose() {
        return expose;
    }

    public void setExpose(String[] expose) {
        this.expose = expose;
    }

    public String[] getExited() {
        return exited;
    }

    public void setExited(String[] exited) {
        this.exited = exited;
    }

    public String[] getHealth() {
        return health;
    }

    public void setHealth(String[] health) {
        this.health = health;
    }

    public String[] getId() {
        return id;
    }

    public void setId(String[] id) {
        this.id = id;
    }

    public String[] getIsolation() {
        return isolation;
    }

    public void setIsolation(String[] isolation) {
        this.isolation = isolation;
    }

    public String[] getIsTask() {
        return isTask;
    }

    public void setIsTask(String[] isTask) {
        this.isTask = isTask;
    }

    public String[] getLabel() {
        return label;
    }

    public void setLabel(String[] label) {
        this.label = label;
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public String[] getNetwork() {
        return network;
    }

    public void setNetwork(String[] network) {
        this.network = network;
    }

    public String[] getPublish() {
        return publish;
    }

    public void setPublish(String[] publish) {
        this.publish = publish;
    }

    public String[] getSince() {
        return since;
    }

    public void setSince(String[] since) {
        this.since = since;
    }

    public String[] getStatus() {
        return status;
    }

    public void setStatus(String[] status) {
        this.status = status;
    }

    public String[] getVolume() {
        return volume;
    }

    public void setVolume(String[] volume) {
        this.volume = volume;
    }


    public Filters ancestor(String ... ancestor) {
        this.ancestor = ancestor;
        return this;
    }

    public Filters before(String ... before) {
        this.before = before;
        return this;
    }

    public Filters expose(String ... expose) {
        this.expose = expose;
        return this;
    }

    public Filters exited(String ... exited) {
        this.exited = exited;
        return this;
    }

    public Filters health(String ... health) {
        this.health = health;
        return this;
    }

    public Filters id(String ... id) {
        this.id = id;
        return this;
    }

    public Filters isolation(String ... isolation) {
        this.isolation = isolation;
        return this;
    }

    public Filters isTask(String ... isTask) {
        this.isTask = isTask;
        return this;
    }

    public Filters label(String ... label) {
        this.label = label;
        return this;
    }

    public Filters name(String ... name) {
        this.name = name;
        return this;
    }

    public Filters network(String ... network) {
        this.network = network;
        return this;
    }

    public Filters publish(String ... publish) {
        this.publish = publish;
        return this;
    }

    public Filters since(String ... since) {
        this.since = since;
        return this;
    }

    public Filters status(String ... status) {
        this.status = status;
        return this;
    }

    public Filters volume(String ... volume) {
        this.volume = volume;
        return this;
    }
}
