/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import java.util.LinkedList;
import java.util.List;

/**
 * The class extends the standard configuration to add analysis related methods and fields.
 */
public class AnalysisConfiguration extends Configuration {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(AnalysisConfiguration.class.getSimpleName());

    private final String workflowClass;
    private final List<String> listOfUsedTools;
    private final List<String> usedToolFolders;

    private String cleanupScript;
    private String nativeToolID;
    private String targetJobManager;
    private String brawlWorkflow;
    private String brawlBaseWorkflow;
    private String runtimeServiceClass;

    /**
     * For main configurations
     */
    public AnalysisConfiguration(PreloadedConfiguration preloadedConfiguration, String workflowClass, String runtimeServiceClass, Configuration parentConfiguration, List<String> listOfUsedTools, List<String> usedToolFolders, String cleanupScript) {
        super(preloadedConfiguration, parentConfiguration);
        this.workflowClass = workflowClass;
        this.listOfUsedTools = listOfUsedTools;
        this.usedToolFolders = usedToolFolders != null ? usedToolFolders : new LinkedList<>();
        this.cleanupScript = cleanupScript;
        this.runtimeServiceClass = runtimeServiceClass;
    }


    public String getWorkflowClass() {
        return workflowClass;
    }

    @Override
    public ResourceSetSize getResourcesSize() {
        for (Configuration configuration : getParents()) {
            if(configuration instanceof ProjectConfiguration)
                return configuration.getResourcesSize();
        }
        return getParents().get(0).getResourcesSize();
    }

    /**
     * Returns a list of tool ids which might be used on runtime.
     * As an analysis might have no tools set, the value may be null which means it is not configured.
     * The values in the list are then used i.e. for further checks, like script validation.
     *
     * @return a value other than null if the value was configured.
     */
    public List<String> getListOfUsedTools() {
        return listOfUsedTools;
    }

    /**
     * Returns a list of the folders in which the tools for this analysis are stored.
     * If the list is empty, Roddy will try to detect tool folders automatically in future version.
     * TODO For a future version, detect folders and return this list.
     *
     * @return
     */
    public List<String> getUsedToolFolders() { return usedToolFolders; }

    public boolean hasCleanupScript() {
        return cleanupScript != null;
    }

    public String getCleanupScript() {
        return cleanupScript;
    }

    public void setBrawlWorkflow(String brawlWorkflow) {
        this.brawlWorkflow = brawlWorkflow;
    }

    public String getBrawlWorkflow() {
        return brawlWorkflow;
    }

    public boolean isNative() {
        return nativeToolID != null;
    }

    public void setNativeToolID(String id) {
        this.nativeToolID = id;
    }

    public String getNativeToolID() {
        return nativeToolID;
    }

    public void setJobManagerFactory(String targetJobManager) {
        this.targetJobManager = targetJobManager;
    }

    public String getTargetJobManagerClass() {
        return targetJobManager;
    }

    public String getBrawlBaseWorkflow() {
        return brawlBaseWorkflow;
    }

    public void setBrawlBaseWorkflow(String brawlBaseWorkflow) {
        this.brawlBaseWorkflow = brawlBaseWorkflow;
    }

    public String getRuntimeServiceClass() {
        return runtimeServiceClass;
    }
}

