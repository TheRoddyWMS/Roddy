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
import de.dkfz.roddy.execution.jobs.JobManager
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
    // @Override
    protected List<String> _execute(String command, boolean waitFor, boolean ignoreErrors, OutputStream outputStream = null) {
        if (waitFor) {
            ExecutionHelper.ExtendedProcessExecutionResult helper = ExecutionHelper.executeCommandWithExtendedResult(command, outputStream);
            return (["" + helper.exitValue, helper.processID] + helper.lines).asList();
        } else {
            Thread.start {
                command.execute();
            }
            return ["0", "0"];
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
    protected String handleServiceBasedJobExitStatus(Command command, ExecutionResult res, OutputStream outputStream) {
        if (command.isBlockingCommand()) {
            command.setExecutionID(JobManager.getInstance().createJobDependencyID(command.getJob(), res.processID));

            File logFile = command.getExecutionContext().getRuntimeService().getLogFileForCommand(command)

            // Use reflection to get access to the hidden path field :p The stream object does not natively give
            // access to it and I do not want to create a new class just for this.
            Field fieldOfFile = FileOutputStream.class.getDeclaredField("path");
            fieldOfFile.setAccessible(true);
            File tmpFile2 = new File((String)fieldOfFile.get(outputStream));

            FileSystemAccessProvider.getInstance().moveFile(tmpFile2, logFile);
            return "none";
        } else {
            String exID = "none";
            if (res.successful) {
                exID = JobManager.getInstance().parseJobID(res.resultLines[0]);
                command.setExecutionID(JobManager.getInstance().createJobDependencyID(command.getJob(), exID));
                JobManager.getInstance().storeJobStateInfo(command.getJob());
            }
            return exID;
        }
    }

    @Override
    boolean isAvailable() {
        return true;
    }

    @Override
    void releaseCache() {

    }
}
