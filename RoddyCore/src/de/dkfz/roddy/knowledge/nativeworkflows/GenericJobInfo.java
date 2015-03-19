package de.dkfz.roddy.knowledge.nativeworkflows;

import de.dkfz.roddy.core.BufferUnit;
import de.dkfz.roddy.core.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Created by michael on 06.02.15.
 */
public class GenericJobInfo {

    private final ExecutionContext executionContext;
    private final String jobName;
    private final String toolID;
    private final String id;
    private final Map<String, String> parameters;
    private List<String> parentJobIDs;
    private String walltime;
    private String memory;
    private BufferUnit memoryBufferUnit;
    private String cpus;
    private String nodes;
    private String queue;
    private String otherSettings;

    public GenericJobInfo(ExecutionContext executionContext, String jobName, String toolID, String id, Map<String, String> parameters, List<String> parentJobIDs) {

        this.executionContext = executionContext;
        this.jobName = jobName;
        this.toolID = toolID;
        this.id = id;
        this.parameters = parameters;
        this.parentJobIDs = parentJobIDs;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public String getJobName() {
        return jobName;
    }

    public String getToolID() {
        return toolID;
    }

    public String getID() {
        return id;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public List<String> getParentJobIDs() {
        return parentJobIDs;
    }

    public void setWalltime(String walltime) {
        this.walltime = walltime;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public void setMemoryBufferUnit(BufferUnit memoryBufferUnit) {
        this.memoryBufferUnit = memoryBufferUnit;
    }

    public void setCpus(String cpus) {
        this.cpus = cpus;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public void setOtherSettings(String otherSettings) {
        this.otherSettings = otherSettings;
    }

    public void setParentJobIDs(List<String> parentJobIDs) {
        this.parentJobIDs = parentJobIDs;
    }

    public String getId() {
        return id;
    }

    public String getWalltime() {
        return walltime;
    }

    public String getMemory() {
        return memory;
    }

    public BufferUnit getMemoryBufferUnit() {
        return memoryBufferUnit;
    }

    public String getCpus() {
        return cpus;
    }

    public String getNodes() {
        return nodes;
    }

    public String getQueue() {
        return queue;
    }

    public String getOtherSettings() {
        return otherSettings;
    }

    @Override
    public String toString() {
        return "GenericJobInfo{" +
                "executionContext=" + executionContext +
                ", jobName='" + jobName + '\'' +
                ", toolID='" + toolID + '\'' +
                ", id='" + id + '\'' +
                ", parameters=" + parameters +
                ", parentJobIDs=" + parentJobIDs +
                '}';
    }
}
