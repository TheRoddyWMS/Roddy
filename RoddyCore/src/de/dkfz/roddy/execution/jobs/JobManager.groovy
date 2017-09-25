/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

/**
 * (Proxy) Compatibility class for 2.3.x until 2.4
 * Created by heinold on 06.03.17.
 */
@Deprecated()
@CompileStatic
class JobManager {

    private BatchEuphoriaJobManager jobManager
    
    static JobManager getInstance() {
        return new JobManager(Roddy.getJobManager())
    }

    JobManager(BatchEuphoriaJobManager jobManager) {
        this.jobManager = jobManager
    }

    static String createJobName(BaseFile baseFile, String toolID, boolean reduceLevel) {
        return RuntimeService._createJobName(baseFile.executionContext, baseFile, toolID, reduceLevel)
    }

    boolean executesWithoutJobSystem() {
        return jobManager.executesWithoutJobSystem()
    }
}
