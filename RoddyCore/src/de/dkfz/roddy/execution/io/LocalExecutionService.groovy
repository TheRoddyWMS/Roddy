/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io

import de.dkfz.roddy.SystemProperties
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The local execution service executes commands on the local machine. For this groovy's execute() method is used.
 * All commands are executed using Bash as interpreter!
 */
@CompileStatic
class LocalExecutionService extends ExecutionService {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(LocalExecutionService.class.name)

    // Note that the thread pool is unbounded.
    private ExecutorService executorService = Executors.newCachedThreadPool()

    @Override
    String getUsername() {
        return SystemProperties.userName
    }

    @Override
    void addSpecificSettingsToConfiguration(Configuration configuration) {
        //Disable several things which don't work.
        configuration.getConfigurationValues().
                add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, "false"))
        configuration.getConfigurationValues().
                add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERMASK, "false"))
        configuration.getConfigurationValues().
                add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_QUERY_ID, "false"))
    }

    @Override
    boolean isLocalService() {
        return true
    }

    /**
     * @param command        The command to execute. Note, this is always executed with Bash as the command interpreter.
     * @param waitFor        Whether to do synchronous (true) or asynchronous execution. Asynchronous execution
     *                       will be done on the class-internal thread-pool. Don't do this with mutually blocking
     *                       processes.
     * @param ignoreErrors   Ignored. No execution errors are reported. This is really nasty, but that's the way it
     *                       is in the moment that doesn't break dependencies. Instead the method logs errors to the
     *                       logging output.
     * @param outputStream   Ignored.
     * @return
     */
    @Override
    protected ExecutionResult _execute(String command,
                                       boolean waitFor = true,
                                       Duration timeout = Duration.ZERO,
                                       OutputStream outputStream = null) {
        return LocalExecutionHelper.executeCommandWithExtendedResult(
                command, waitFor, timeout)
    }

    /**
     * Should be the current directory
     */
    @Override
    File queryWorkingDirectory() {
        return new File("")
    }

    @Override
    boolean isAvailable() {
        return true
    }

}
