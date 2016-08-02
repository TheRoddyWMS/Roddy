/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io;

import java.io.File;
import java.util.List;

public abstract class RemoteExecutionService extends ExecutionService {

    @Override
    public boolean isLocalService() {
        return false;
    }

    @Override
    public boolean needsPassword() {
        return true;
    }
}
