/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;


import de.dkfz.roddy.execution.jobs.BEJob;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.execution.jobs.ReadOutJob;

import java.io.File;

/**
 */
public class FXLogFileWrapper {
    private final File file;
    private final BEJob job;

    public FXLogFileWrapper(BEJob job, File file) {
        this.file = file;
        this.job = job;
    }

    public String getPath() {
        return file.getPath();
    }

    public String getName() {
        return file.getName();
    }

    public BEJob getJob() {
        return job;
    }

    public String getJobID() {
        return job.getJobID();
    }

    public JobState getJobState() {
        if(job instanceof ReadOutJob)
            return ((ReadOutJob)job).getJobState();
        else
            return JobState.UNKNOWN;
    }
}
