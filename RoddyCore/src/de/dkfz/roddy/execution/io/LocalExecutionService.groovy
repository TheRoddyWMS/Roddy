/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.tools.LoggerWrapper

import java.lang.reflect.Field

/**
 * The local execution service executes commands on the local machine. For this groovy's execute() method is used.
 * @author michael
 */
@groovy.transform.CompileStatic
public class LocalExecutionService extends ExecutionService {
    private static final LoggerWrapper logger = LoggerWrapper.getLogger(LocalExecutionService.class.name);

    @Override
    String getUsername() {
        return System.getProperty("user.name")
    }

    @Override
    void addSpecificSettingsToConfiguration(Configuration configuration) {
        //Disable several things which don't work.
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, "false"));
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERMASK, "false"));
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_QUERY_ID, "false"));
    }

    @Override
    boolean isLocalService() {
        return true;
    }

    // This method actually overrides a base class. But if we keep the @Override, the Groovy (or Java) compiler constantly
    // claims, that the method does not override it's base method.
    // That is, why we keep it in but only as a comment.
//     @Override
    protected ExecutionResult _execute(String command, boolean waitFor, boolean ignoreErrors, OutputStream outputStream = null) {
        if (waitFor) {
            return LocalExecutionHelper.executeCommandWithExtendedResult(command, outputStream);
        } else {
            Thread.start {
                command.execute();
            }
            return new ExecutionResult(true, 0, [], "")
        }
    }

    @Override
    protected FileOutputStream createServiceBasedOutputStream(Command command, boolean waitFor) {
        //Store away process output if this is a local service.
        FileOutputStream outputStream = null;
        if (waitFor && command.isBlockingCommand()) {
            File tmpFile = File.createTempFile("roddy_", "_temporaryLogfileStream")
            tmpFile.deleteOnExit();
            outputStream = new FileOutputStream(tmpFile)
        }
        return outputStream;
    }

    @Override
    protected void finalizeServiceBasedOutputStream(Command command, OutputStream outputStream) {
        if (!outputStream) return

        File logFile = new File(((Job)command.job).context.getExecutionDirectory(), "${command.job.getJobName()}_${command.job.jobCreationCounter}.logfile")

        // Use reflection to get access to the hidden path field :p The stream object does not natively give
        // access to it and I do not want to create a new class just for this.
        Field fieldOfFile = FileOutputStream.class.getDeclaredField("path")
        fieldOfFile.setAccessible(true);
        File tmpFile2 = new File((String) fieldOfFile.get(outputStream))

        FileSystemAccessProvider.getInstance().moveFile(tmpFile2, logFile)
    }

    @Override
    /**
     * Should be the current directory
     */
    File queryWorkingDirectory() {
        return new File("")
    }

    @Override
    boolean isAvailable() {
        return true;
    }

}
