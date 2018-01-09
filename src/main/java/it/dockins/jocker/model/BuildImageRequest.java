package it.dockins.jocker.model;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class BuildImageRequest {
    private String dockerfile;
    private String tag;
    private String extrahosts;
    private String remote;
    private boolean nocache;
    private boolean quiet;
    private Collection<String> cachefrom;
    private String pull;
    private boolean rm;
    private boolean forcerm;
    private long memory;
    private long memswap;
    private long cpushares;
    private String cpusetcpus;
    private long cpuperiod;
    private long cpuquota;
    private long shmsize;
    private String ulimits;
    private String networkmode;
    private Map<String, String> buildargs;
    private boolean squash;
    private Collection<String> labels;

    public String getDockerfile() {
        return dockerfile;
    }

    public String getTag() {
        return tag;
    }

    public String getExtrahosts() {
        return extrahosts;
    }

    public String getRemote() {
        return remote;
    }

    public boolean isNocache() {
        return nocache;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public Collection<String> getCachefrom() {
        return cachefrom;
    }

    public String getPull() {
        return pull;
    }

    public boolean isRm() {
        return rm;
    }

    public boolean isForcerm() {
        return forcerm;
    }

    public long getMemory() {
        return memory;
    }

    public long getMemswap() {
        return memswap;
    }

    public long getCpushares() {
        return cpushares;
    }

    public String getCpusetcpus() {
        return cpusetcpus;
    }

    public long getCpuperiod() {
        return cpuperiod;
    }

    public long getCpuquota() {
        return cpuquota;
    }

    public long getShmsize() {
        return shmsize;
    }

    public String getUlimits() {
        return ulimits;
    }

    public String getNetworkmode() {
        return networkmode;
    }

    public Map<String, String> getBuildargs() {
        return buildargs;
    }

    public boolean isSquash() {
        return squash;
    }

    public Collection<String> getLabels() {
        return labels;
    }


    public BuildImageRequest dockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
        return this;
    }

    public BuildImageRequest tag(String tag) {
        this.tag = tag;
        return this;
    }

    public BuildImageRequest extrahosts(String extrahosts) {
        this.extrahosts = extrahosts;
        return this;
    }

    public BuildImageRequest remote(String remote) {
        this.remote = remote;
        return this;
    }

    public BuildImageRequest nocache(boolean nocache) {
        this.nocache = nocache;
        return this;
    }

    public BuildImageRequest quiet(boolean quiet) {
        this.quiet = quiet;
        return this;
    }

    public BuildImageRequest cachefrom(Collection<String> cachefrom) {
        this.cachefrom = cachefrom;
        return this;
    }

    public BuildImageRequest pull(String pull) {
        this.pull = pull;
        return this;
    }

    public BuildImageRequest rm(boolean rm) {
        this.rm = rm;
        return this;
    }

    public BuildImageRequest forcerm(boolean forcerm) {
        this.forcerm = forcerm;
        return this;
    }

    public BuildImageRequest memory(long memory) {
        this.memory = memory;
        return this;
    }

    public BuildImageRequest memswap(long memswap) {
        this.memswap = memswap;
        return this;
    }

    public BuildImageRequest cpushares(long cpushares) {
        this.cpushares = cpushares;
        return this;
    }

    public BuildImageRequest cpusetcpus(String cpusetcpus) {
        this.cpusetcpus = cpusetcpus;
        return this;
    }

    public BuildImageRequest cpuperiod(long cpuperiod) {
        this.cpuperiod = cpuperiod;
        return this;
    }

    public BuildImageRequest cpuquota(long cpuquota) {
        this.cpuquota = cpuquota;
        return this;
    }

    public BuildImageRequest shmsize(long shmsize) {
        this.shmsize = shmsize;
        return this;
    }

    public BuildImageRequest ulimits(String ulimits) {
        this.ulimits = ulimits;
        return this;
    }

    public BuildImageRequest networkmode(String networkmode) {
        this.networkmode = networkmode;
        return this;
    }

    public BuildImageRequest buildargs(Map<String, String> buildargs) {
        this.buildargs = buildargs;
        return this;
    }

    public BuildImageRequest squash(boolean squash) {
        this.squash = squash;
        return this;
    }

    public BuildImageRequest labels(Collection<String> labels) {
        this.labels = labels;
        return this;
    }
}
