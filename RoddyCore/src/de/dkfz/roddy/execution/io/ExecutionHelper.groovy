
/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.execution.io.fs.ShellCommandSet
import de.dkfz.roddy.tools.LoggerWrapper

import java.lang.reflect.Field

@groovy.transform.CompileStatic
class ExecutionHelper {
    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionHelper.class.name);

    public static String getProcessID(Process process) {
        Field f = process.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        String processID = f.get(process)
        return processID
    }

    static class ExtendedProcessExecutionResult {
        int exitValue;
        String processID;
        List<String> lines = [];

        ExtendedProcessExecutionResult(int exitValue, String processID, List<String> lines) {
            this.exitValue = exitValue
            this.processID = processID
            this.lines = lines
        }
    }

    public static String executeSingleCommand(String command) {
        //TODO What an windows systems?
        Process process = Roddy.getLocalCommandSet().getShellExecuteCommand(command).execute();

        final String separator = Constants.ENV_LINESEPARATOR;
        process.waitFor();
        if(process.exitValue()) {
            throw new RuntimeException("Process could not be run" + separator + "\tCommand: sh -c "+ command + separator + "\treturn code is: " + process.exitValue())
        }

        def text = process.text
        return text.length() > 0 ? text[0 .. -2] : text; //Cut off trailing "\n"
    }

    /**
     * Execute a command using the local command interpreter (For Linux this might be bash)
     *
     * If outputStream is set, the full output is going to this stream. Otherwise it is stored
     * in the returned object.
     *
     * @param command
     * @param outputStream
     * @return
     */
    public static ExtendedProcessExecutionResult executeCommandWithExtendedResult(String command, OutputStream outputStream = null) {
        Process process = Roddy.getLocalCommandSet().getShellExecuteCommand(command).execute();

        //TODO Put to a custom class which can handle things for Windows as well.
        String processID = getProcessID(process)

        List<String> lines = [];
        if (logger.isVerbosityHigh())
            println("Executing the command ${command} locally.");

        if (outputStream)
            process.waitForProcessOutput(outputStream, outputStream)
        else {
            StringBuilder sstream = new StringBuilder();
            process.waitForProcessOutput(sstream, sstream);
            lines = sstream.readLines().collect { String l -> return l.toString(); };
        }
        return new ExtendedProcessExecutionResult(process.exitValue(), processID, lines);
    }

    public static Process executeNonBlocking(String command) {
        Process process = Roddy.getLocalCommandSet().getShellExecuteCommand("sleep 1; " + command).execute();
        return process;
    }


    static String execute(String cmd) {
        def proc = cmd.execute();
        int res = proc.waitFor();
        if (res == 0) {
            return proc.in.text;
        }
        return "";
    }
}