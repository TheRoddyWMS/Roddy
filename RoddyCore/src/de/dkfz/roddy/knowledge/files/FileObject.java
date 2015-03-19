package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.Project;
import de.dkfz.roddy.execution.jobs.JobResult;

import java.io.Serializable;

/**
 * Base class with common methods and members for filebased
 * objects like BaseFile or FileGroup
 */
public abstract class FileObject implements Serializable {
    private transient ExecutionContext executionContext;
    private JobResult creatingJobsResult;

    public FileObject(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public abstract void runDefaultOperations();

    public DataSet getPid() {
        return executionContext.getDataSet();
    }

    public Project getProject() {
        return executionContext.getProject();
    }

    public JobResult getCreatingJobsResult() {
        return creatingJobsResult;
    }

    public void setCreatingJobsResult(JobResult jr) {
        this.creatingJobsResult = jr;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
