/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.jobs;

import de.dkfz.roddy.tools.AppConfig;
import de.dkfz.eilslabs.batcheuphoria.execution.ExecutionService;

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

    private final List<Job> parentJobs;

    public ReadOutJob(String jobName, String executedJobID, Map<String,String> parameters, List<Job> parentJobs) {
        super(jobName, parameters, null, null, parentJobs);
        this.parentJobs = parentJobs;
        this.readOut = true;
        this.jobID = executedJobID;
    }

    @Override
    public JobResult run(ExecutionService executionService, AppConfig configuration, Map<String, String> parameters) {
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
