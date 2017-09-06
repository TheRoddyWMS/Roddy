/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.InfoObject

/**
 * Class to keep backward compatibility
 * Created by heinold on 09.06.17.
 */
@Deprecated
abstract class JobDependencyID {

    @Deprecated
    static class FakeJobID extends BEFakeJobID {
        FakeJobID(BEJob job, BEFakeJobID.FakeJobReason fakeJobReason, boolean isArray) {
            super(job, fakeJobReason, isArray)
        }

        FakeJobID(BEJob job, boolean isArray) {
            super(job, isArray)
        }

        FakeJobID(BEJob job, BEFakeJobID.FakeJobReason fakeJobReason) {
            super(job, fakeJobReason)
        }

        FakeJobID(BEJob job) {
            super(job)
        }
    }
}
