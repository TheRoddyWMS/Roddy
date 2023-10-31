/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
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
    public final @Nullable String jobID;

    private final List<BEJobID> parentJobsIds;

    public ReadOutJob(@NotNull ExecutionContext context,
                      @NotNull String jobName,
                      @NotNull ToolIdCommand command,
                      @Nullable String executedJobID,
                      Map<String,String> parameters,   // Not implemented? Wrong Type!
                      List<BEJobID> parentJobsIds) {
        super(context,
              jobName,
              command);
        this.parentJobsIds = parentJobsIds;
        this.readOut = true;
        this.jobID = executedJobID;
    }

    /**
     * Returns the read jobs id.
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
