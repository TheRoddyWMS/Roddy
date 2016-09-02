/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Objects of this class are created if information on previous jobs are read out from the file system.
 * They represent executed jobs!
 */
public class ReadOutJob extends Job {

    /**
     * Defines, that the job was read from disk and cannot be executed again!
     */
    public final boolean readOut;
    /**
     * The clusters job id, only for readOut jobs.
     */
    public final String jobID;

    private final List<String> parentJobs;

    public ReadOutJob(ExecutionContext process, String jobName, String toolID, String executedJobID, Map<String,String> parameters, List<String> parentJobs) {
        super(jobName, process, toolID, parameters, null, null, new LinkedList<BaseFile>());
        this.parentJobs = parentJobs;
        this.readOut = true;
        this.jobID = executedJobID;
    }

    @Override
    public JobResult run() {
        throw new RuntimeException("A read out job cannot be run!");
    }

    /**
     * Returns the read out jobs id.
     * @return
     */
    @Override
    public String getJobID() {
        return jobID;
    }

    @Override
    public String getToolID() {
        String[] split = jobName.split("_");
        return split[split.length - 1];
    }
}
