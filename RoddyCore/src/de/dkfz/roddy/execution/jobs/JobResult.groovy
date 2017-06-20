/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https:..www.github.com.eilslabs.Roddy.LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.ExecutionContext

/**
 * Created by heinold on 19.06.17.
 */
@Deprecated
class JobResult extends BEJobResult {
    JobResult() {
    }

    JobResult(BEJobResult jr) {
        // Some sort of deprecated copy constructor.
        super(null, jr.command, jr.jobID, jr.wasExecuted, jr.toolID, jr.jobParameters, jr.parentJobs)
    }

    JobResult(ExecutionContext context, de.dkfz.roddy.execution.jobs.Command command, de.dkfz.roddy.execution.jobs.JobDependencyID.FakeJobID jobID, java.io.File toolID, java.util.Map jobParameters, java.util.List parentJobs) {
        super(context, command, jobID, false, toolID, jobParameters, parentJobs)
    }
    
    JobResult(ExecutionContext context, Command command, JobDependencyID.FakeJobID jobID, boolean wasExecuted, File toolID, Map<String, String> jobParameters, List<BEJob> parentJobs) {
        super(context, command, jobID, wasExecuted, toolID, jobParameters, parentJobs)
    }

    JobResult(ExecutionContext context, Command command, JobDependencyID jobID, boolean wasExecuted, File toolID, Map<String, String> jobParameters, List<BEJob> parentJobs) {
        super(context, command, jobID, wasExecuted, toolID, jobParameters, parentJobs)
    }

    JobResult(Command command, JobDependencyID jobID, boolean wasExecuted, File toolID, Map<String, String> jobParameters, List<BEJob> parentJobs) {
        super(command, jobID, wasExecuted, toolID, jobParameters, parentJobs)
    }

    JobResult(Command command, JobDependencyID jobID, boolean wasExecuted, boolean wasArray, File toolID, Map<String, String> jobParameters, List<BEJob> parentJobs) {
        super(command, jobID, wasExecuted, wasArray, toolID, jobParameters, parentJobs)
    }
}
