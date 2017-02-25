/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.eilslabs.batcheuphoria.jobs.JobResult;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.Project;

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
        // TODO Deprecated. Remove if not needed in any workflow! Possibly move to plugin.
        return getDataSet();
    }

    public DataSet getDataSet() {
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

    protected void setExecutionContext(ExecutionContext newContext) { this.executionContext = newContext; }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
