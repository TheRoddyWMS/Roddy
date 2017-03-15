/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.tools.BufferValue;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.TimeUnit;

/**
 * A resource set describes a list of resource values which are used by a script.
 * The active set is defined in a project configuration with m as the default.
 * The resources don't neccessarily have to be set, all values are objects and not primitives.
 * There are also max values defined but not used or filled yet.
 */
public class ResourceSet {
    private final String queue;
    private ResourceSetSize size;
    /**
     * The target memory value.
     */
    private BufferValue mem;
    private BufferValue memMax;

    private Integer cores;
    private Integer coresMax;
    private Integer nodes;
    private Integer nodesMax;
    private TimeUnit walltime;

    /**
     * Hard disk storage used.
     */
    private BufferValue storage;
    private BufferValue storageMax;
    private String additionalNodeFlag;

    public ResourceSet(ResourceSetSize size, BufferValue mem, Integer cores, Integer nodes, TimeUnit walltime, BufferValue storage, String queue, String additionalNodeFlag) {
        this.size = size;
        this.mem = mem;
        this.cores = cores;
        this.nodes = nodes;
        this.walltime = walltime;
        this.storage = storage;
        this.queue = queue;
        this.additionalNodeFlag = additionalNodeFlag;
    }

    public ResourceSetSize getSize() {
        return size;
    }

    public ResourceSet clone() {
        return new ResourceSet(size, mem, cores, nodes, walltime, storage, queue, additionalNodeFlag);
    }

    public BufferValue getMem() {
        return mem;
    }

    public Integer getCores() {
        return cores;
    }

    public Integer getNodes() {
        return nodes;
    }

    public BufferValue getStorage() {
        return storage;
    }

    public boolean isMemSet() {
        return mem != null;
    }

    public boolean isCoresSet() {
        return cores != null;
    }

    public boolean isNodesSet() {
        return nodes != null;
    }

    public boolean isStorageSet() {
        return storage != null;
    }

    public TimeUnit getWalltime() {
        return walltime;
    }

    public boolean isWalltimeSet() {
        return walltime != null;
    }

    public boolean isQueueSet() {
        return !RoddyConversionHelperMethods.isNullOrEmpty(queue);
    }

    public String getQueue() {
        return queue;
    }

    public boolean isAdditionalNodeFlagSet() {
        return !RoddyConversionHelperMethods.isNullOrEmpty(additionalNodeFlag);
    }

    public String getAdditionalNodeFlag() {
        return additionalNodeFlag;
    }
}
