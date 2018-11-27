/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.util.LinkedHashMap;
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

    private final List<BEJob> parentJobs;

    public ReadOutJob(ExecutionContext context, String jobName, String toolID, String executedJobID, Map<String,String> parameters, List<BEJob> parentJobs) {
        super(context, jobName, toolID, new LinkedHashMap<>(), new LinkedList<BaseFile>());
        this.parentJobs = parentJobs;
        this.readOut = true;
        this.jobID = executedJobID;
    }

    /**
     * Returns the read out jobs id.
     * @return
     */
    @Override
    public BEJobID getJobID() {
        return new BEJobID(jobID);
    }

    @Override
    public String getToolID() {
        String[] split = jobName.split("_");
        return split[split.length - 1];
    }
}
