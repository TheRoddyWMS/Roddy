/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Result of a job run.
 * <p/>
 * Stores different information related to a job run. i.e. if the job was
 * executed.
 *
 * @author michael
 */
public class JobResult implements Serializable {
    /**
     * The command which was used to create this result.
     */
    private final Command command;
    /**
     * The run in which this result was created.
     */
    public transient final ExecutionContext run;
    /**
     * The current job's id, i.e. qsub id.
     * Used for dependencies.
     */
    private final JobDependencyID jobID;
    /**
     * Was the job executed?
     */
    private final boolean wasExecuted;
    /**
     * Was the job an array job?
     */
    private final boolean wasArray;
    /**
     * The tool which was run for this job.
     */
    private final File toolID;
    /**
     * Parameters for the job.
     */
    private final Map<String, String> jobParameters;
    /**
     * Files which were used as some sort of input for the job.
     */
    public transient final List<BaseFile> parentFiles;

    public JobResult(ExecutionContext run, Command command, JobDependencyID jobID, boolean wasExecuted, File toolID, Map<String, String> jobParameters, List<BaseFile> parentFiles) {
        this(run, command, jobID, wasExecuted, false, toolID, jobParameters, parentFiles);
    }

    public JobResult(ExecutionContext run, Command command, JobDependencyID jobID, boolean wasExecuted, boolean wasArray, File toolID, Map<String, String> jobParameters, List<BaseFile> parentFiles) {
        this.command = command;
        this.jobID = jobID;
        this.wasExecuted = wasExecuted;
        this.wasArray = wasArray;
        this.toolID = toolID;
        this.jobParameters = jobParameters;
        this.parentFiles = parentFiles;
        this.run = run;
    }


    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
        try {
            s.defaultWriteObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        try {
            s.defaultReadObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Command getCommand() {
        return command;
    }

    public ExecutionContext getRun() {
        return run;
    }

    public JobDependencyID getJobID() {
        return jobID;
    }

    public boolean isWasExecuted() {
        return wasExecuted;
    }

    public boolean isWasArray() {
        return wasArray;
    }

    public File getToolID() {
        return toolID;
    }

    public Job getJob() {
        return jobID.job;
    }

    public Map<String, String> getJobParameters() {
        return jobParameters;
    }

    public List<BaseFile> getParentFiles() {
        return parentFiles;
    }
}
