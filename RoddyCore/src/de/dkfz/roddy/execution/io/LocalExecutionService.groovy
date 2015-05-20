/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package de.dkfz.roddy.execution.io

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.tools.LoggerWrapper

import java.lang.reflect.Field
import java.util.logging.Logger

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
//        configuration.getConfigurationValues().setValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, false)
    }

    @Override
    boolean isLocalService() {
        return true;
    }
//
//    protected List<String> _execute(String command, boolean waitFor) {
//        return _execute(command, waitFor, false, null);
//    }
//
//    protected List<String> _execute(String command, boolean waitFor, OutputStream outputStream) {
//        return _execute(command, waitFor, false, outputStream);
//    }

//    @Override
    protected List<String> _execute(String command, boolean waitFor, boolean ignoreErrors, OutputStream outputStream = null) {
        if (waitFor) {
            Process execute = ["bash", "-c", command].execute();

            //TODO Put to a custom class which can handle things for Windows as well.
            Field f = execute.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            String processID = f.get(execute)

            List<String> lines = [];
            if (logger.isVerbosityHigh())
                println("Executing the command ${command} locally.");

            if (outputStream)
                execute.waitForProcessOutput(outputStream, outputStream)
            else {
                StringBuilder sstream = new StringBuilder();
                execute.waitForProcessOutput(sstream, sstream);
                lines = sstream.readLines().collect { CharSequence l -> return l.toString(); };
            }

            return (["" + execute.exitValue(), processID] + lines).asList();
        } else {
            Thread.start {
                command.execute();
            }
            return ["0", "0"];
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
