/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https:..www.github.com.eilslabs.Roddy.LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

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
        super(jr.command, jr.job, jr.executionResult, jr.toolID, jr.jobParameters, jr.parentJobs)
    }

}
