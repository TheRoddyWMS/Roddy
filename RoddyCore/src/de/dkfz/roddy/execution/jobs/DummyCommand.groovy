/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.eilslabs.batcheuphoria.jobs.Command
import de.dkfz.eilslabs.batcheuphoria.jobs.JobDependencyID

/**
 * Created by heinold on 23.02.17.
 */
class DummyCommand extends Command {

    private String jobName;

     DummyCommand(Job job, String jobName, boolean isArray) {
        super(job, run, "dummy_" + getNextIDCountValue(), null);
        this.jobName = jobName;
        if (isArray) {
            setExecutionID(JobDependencyID.getNotExecutedFakeJob(job, true));
        } else {
            setExecutionID(JobDependencyID.getNotExecutedFakeJob(job));
        }
    }

    @Override
    public String toString() {
        return String.format("Command of class %s with id %s and name %s", this.getClass().getName(), getID(), jobName);
    }
}