/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.config.ConfigurationError;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.Project;
import de.dkfz.roddy.execution.jobs.BEJobResult;

import java.io.Serializable;

/**
 * Base class with common methods and members for filebased
 * objects like BaseFile or FileGroup
 */
public abstract class FileObject implements Serializable {
    private transient ExecutionContext executionContext;
    private BEJobResult creatingJobsResult;

    public FileObject(ExecutionContext executionContext) {
        // Note: ExecutionContext can be null!
        this.executionContext = executionContext;
    }

    public abstract void runDefaultOperations() throws ConfigurationError;

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

    public BEJobResult getCreatingJobsResult() {
        return creatingJobsResult;
    }

    public void setCreatingJobsResult(BEJobResult jr) {
        this.creatingJobsResult = jr;
    }

    protected void setExecutionContext(ExecutionContext newContext) { this.executionContext = newContext; }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
