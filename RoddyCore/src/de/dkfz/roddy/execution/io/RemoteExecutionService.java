/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io;

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
