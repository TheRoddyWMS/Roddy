package de.dkfz.roddy.config;

import de.dkfz.roddy.core.Project;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The class extends the standard configuration to add analysis related methods and fields.
 */
public class AnalysisConfiguration extends Configuration {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(AnalysisConfiguration.class.getSimpleName());

    private final String workflowClass;
    private final List<String> listOfUsedTools;
    private final List<String> usedToolFolders;

    private final Map<String, TestDataOption> testDataOptions = new HashMap<>();
    private String cleanupScript;
    private String nativeToolID;
    private String targetCommandFactory;
    private String brawlWorkflow;
    private String brawlBaseWorkflow;

    /**
     * For main configurations
     */
    public AnalysisConfiguration(InformationalConfigurationContent informationalConfigurationContent, String workflowClass, Map<String, TestDataOption> testdataOptions, Configuration parentConfiguration, List<String> listOfUsedTools, List<String> usedToolFolders, String cleanupScript) {
        super(informationalConfigurationContent, parentConfiguration);
        this.workflowClass = workflowClass;
        this.listOfUsedTools = listOfUsedTools;
        this.usedToolFolders = usedToolFolders != null ? usedToolFolders : new LinkedList<>();
        if(testdataOptions != null)
            this.testDataOptions.putAll(testdataOptions);
        this.cleanupScript = cleanupScript;
    }


    public String getWorkflowClass() {
        return workflowClass;
    }

    /**
     * Returns a list of all the available test data options
     *
     * @return
     */
    public List<String> getListOfTestdataOptions() {
        LinkedList<String> result = new LinkedList<>();
        for (Configuration c : getContainerParents()) {
            if (!(c instanceof AnalysisConfiguration)) continue;
            AnalysisConfiguration parent = (AnalysisConfiguration) c;//getParent() != null && getParent() instanceof AnalysisConfiguration ? (AnalysisConfiguration) getParent() : null;
            if (parent != null) {
                result.addAll(parent.getListOfTestdataOptions());
            }
        }
        result.addAll(testDataOptions.keySet());
        return result;
    }

    /**
     * Returns a list of all the stored test data options
     *
     * @return
     */
    public List<TestDataOption> getTestdataOptions() {
        LinkedList<TestDataOption> result = new LinkedList<>();
        for (Configuration c : getContainerParents()) {
            if (!(c instanceof AnalysisConfiguration)) continue;
            AnalysisConfiguration parent = (AnalysisConfiguration) c;//getParent() != null && getParent() instanceof AnalysisConfiguration ? (AnalysisConfiguration) getParent() : null;

            if (parent != null) {
                result.addAll(parent.getTestdataOptions());
            }
        }
        result.addAll(testDataOptions.values());
        return result;
    }

    /**
     * Returns a specific test data option
     *
     * @param id
     * @return
     */
    public TestDataOption getTestdataOption(String id) {
        if (!hasTestdataOption(id)) {
            logger.severe("AnalysisConfiguration " + this.getID() + " does not know about test data option " + id);
        }

        for (Configuration c : getContainerParents()) {
            if (!(c instanceof AnalysisConfiguration)) continue;
            AnalysisConfiguration parent = (AnalysisConfiguration) c;//getParent() != null && getParent() instanceof AnalysisConfiguration ? (AnalysisConfiguration) getParent() : null;
            if (!testDataOptions.containsKey(id)) {
                if (parent != null) {
                    return parent.getTestdataOption(id);
                } else {
                    throw new RuntimeException("AnalysisConfiguration " + this.getID() + " has no proper object for test data option " + id);
                }
            }
        }
        return testDataOptions.get(id);
    }

    @Override
    public ToolEntry.ResourceSetSize getResourcesSize() {
        for (Configuration configuration : getContainerParents()) {
            if(configuration instanceof ProjectConfiguration)
                return configuration.getResourcesSize();
        }
        return getContainerParents().get(0).getResourcesSize();
    }

    public boolean hasTestdataOption(String id) {
        if (testDataOptions.containsKey(id)) {

            return true;
        } else {
            for (Configuration c : getContainerParents()) {
                if (!(c instanceof AnalysisConfiguration)) continue;
                if (((AnalysisConfiguration) c).hasTestdataOption(id)) return true;
            }
            return false;
        }
    }

    public void addTestDataOptions(List<TestDataOption> options) {
        for (TestDataOption tdo : options) {
            if (!this.testDataOptions.containsKey(tdo.getId()))
                this.testDataOptions.put(tdo.getId(), tdo);
        }
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

    public void setTargetCommandFactory(String targetCommandFactory) {
        this.targetCommandFactory = targetCommandFactory;
    }

    public String getTargetCommandFactoryClass() {
        return targetCommandFactory;
    }

    public String getBrawlBaseWorkflow() {
        return brawlBaseWorkflow;
    }

    public void setBrawlBaseWorkflow(String brawlBaseWorkflow) {
        this.brawlBaseWorkflow = brawlBaseWorkflow;
    }
}

