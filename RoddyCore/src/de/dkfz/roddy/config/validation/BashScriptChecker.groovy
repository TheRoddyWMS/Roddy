package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContextError

import static de.dkfz.roddy.StringConstants.*
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_PREFIX_BASEPATH
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_PREFIX_TOOL
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_SUFFIX_BINARY
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_SUFFIX_BINARY_SHORT

/**
 * Checks linux shell scripts.
 */
@groovy.transform.CompileStatic
public class BashScriptChecker extends ScriptChecker {

    private List<String> specialChars = Arrays.asList(SPLIT_PERCENT, SPLIT_COLON, SPLIT_SLASH, SPLIT_MINUS, SPLIT_PLUS, SPLIT_HASH);

    public BashScriptChecker(File file, String toolID, ScriptValidator scriptValidator) {
        super(file, toolID, scriptValidator);
    }

    @Override
    public void validateScript() {
        List<String> allLinesInFile = file.readLines();

        List<String> alreadyChecked = [];
        List<String> foundVariableNames = [];

        for (String line in allLinesInFile) {
            //Skip comments
            line = line.trim();
            if (line.startsWith(HASH)) continue;
            if (line.length() == 0) continue;
            //First step, find variable assignments
            //These can either be declared using declare or by just assigning them
            //Variables can be created in a [[ ]] && clause!
            //Split by " " and parse every value.
            String[] splitLine = line.split(SPLIT_WHITESPACE);
            for (String splitVar : splitLine) {
                if (!splitVar.contains(EQUALS))
                    continue;
                if (splitVar.size() <= 3) //Leave out things like "==" or " = " which are not used for declarations
                    continue;
                try {
                    String variableName = splitVar.split(SPLIT_EQUALS)[0];
                    alreadyChecked << variableName;
                } catch (Exception ex) {
                    scriptValidator.addErrorToList(new ValidationError(configuration, "Cannot extract variable name", line + " - " + splitVar, ex));
                }
//                    foundVariableNames << variableName;

            }

            //Second step, find variable usages
            //Maybe also keep line number so you see if a variable was used before assignment if it was created in the script
            String[] splitted = line.split(SPLIT_DOLLAR);
            for (String splitVar in splitted) {
                if (splitVar.contains(BRACE_LEFT) && splitVar.contains(BRACE_RIGHT)) {
                    if (splitVar.indexOf(BRACE_LEFT) > splitVar.indexOf(BRACE_RIGHT))
                        continue;
                    try {
                        String variableName = splitVar.split(SPLIT_BRACE_LEFT)[1].split(SPLIT_BRACE_RIGHT)[0].toString();
                        foundVariableNames << variableName;
                    } catch (Exception ex) {
                        scriptValidator.addErrorToList(new ValidationError(configuration, "Cannot extract variable name", line + " - " + splitVar, ex));
                    }
                }
            }
        }
        //Third step, check all found variables
        for (String variableName in foundVariableNames) {

            if (variableName.endsWith(CVALUE_SUFFIX_BINARY) || variableName.endsWith(CVALUE_SUFFIX_BINARY_SHORT) || variableName.startsWith(CVALUE_PREFIX_TOOL) || variableName.startsWith(CVALUE_PREFIX_BASEPATH))
                continue;

            specialChars.each { String chr -> variableName = variableName.split(chr)[0] }

            if (alreadyChecked.contains(variableName))
                continue;
            alreadyChecked << variableName;
            boolean skip = false;
            for (String entry in ScriptValidator.blacklist) {
                if (entry.endsWith("*")) {
                    if (variableName.startsWith(entry[0..-2]))
                        skip = true;
                } else {
                    if (entry == variableName) {
                        skip = true;
                    }
                }
                if (skip)
                    break;
            }
            if (skip)
                continue;

            //Check if the variable is in the configuration
            if (configuration.getConfigurationValues().hasValue(variableName))
                continue;

            //Check if variable was passed as a parameter
            ToolEntry toolEntry = configuration.getTools().getValue(toolID, null);
            if (toolEntry) {
                boolean foundAsParameter;
                for (ToolEntry.ToolParameter toolParameter : toolEntry.inputParameters) {
                    if (toolParameter.scriptParameterName.equals(variableName)) {
                        foundAsParameter = true;
                        break;
                    }
                }
                for (ToolEntry.ToolParameter toolParameter : toolEntry.outputParameters) {
                    if (toolParameter.scriptParameterName.equals(variableName)) {
                        foundAsParameter = true;
                        break;
                    }
                }
                if (foundAsParameter)
                    continue;
            } else {
                scriptValidator.addErrorToList(new ValidationError(configuration, ExecutionContextError.EXECUTION_SCRIPT_NOTFOUND.description, "The tool with id ${toolID} could not be found in configuration.", null));
            }

            scriptValidator.addErrorToList(new ValidationError(configuration, ExecutionContextError.EXECUTION_SCRIPT_INVALID.description, "The variable ${variableName} is possibly not defined correctly: ${file.getAbsolutePath()}", null));
        }
    }
}
