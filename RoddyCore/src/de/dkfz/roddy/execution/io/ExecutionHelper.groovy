
package de.dkfz.roddy.execution.io
import de.dkfz.roddy.Constants

class ExecutionHelper {
    @groovy.transform.CompileStatic
    public static String executeSingleCommand(String command) {
        //TODO What an windows systems?
        def process = ["bash", "-c", command].execute();
        final String separator = Constants.ENV_LINESEPARATOR;
        process.waitFor();
        if(process.exitValue()) {
            throw new RuntimeException("Process could not be run" + separator + "\tCommand: sh -c "+ command + separator + "\treturn code is: " + process.exitValue())
        }

        def text = process.text
        return text.length() > 0 ? text[0 .. -2] : text; //Cut off trailing "\n"
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