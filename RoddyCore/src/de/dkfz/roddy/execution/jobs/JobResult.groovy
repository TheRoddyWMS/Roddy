/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https:..www.github.com.eilslabs.Roddy.LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.ExecutionContext
import groovy.transform.CompileStatic

/**
 * Created by heinold on 19.06.17.
 */
@Deprecated
@CompileStatic
class JobResult extends BEJobResult {
    JobResult() {
    }

    JobResult(BEJobResult jr) {
        // Some sort of deprecated copy constructor.
        super(null, jr.command, jr.job, jr.executionResult, jr.toolID, jr.jobParameters, jr.parentJobs)
    }

    static JobResult getFileExistedFakeJobResult(ExecutionContext context) {
        return new JobResult(new BEJobResult(context, null, new FakeBEJob(new BEFakeJobID(BEFakeJobID.FakeJobReason.FILE_EXISTED)), null, null, null, null))
    }

}
