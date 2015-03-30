package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for validators on the configuration level
 */
public abstract class ConfigurationValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ConfigurationValidator.class.getName());

    protected final Configuration configuration;

    private final List<ValidationError> listOfValidationErrors = new LinkedList<>();

    public ConfigurationValidator(Configuration cfg) {
        configuration = cfg;
    }

    public abstract boolean validate();

    public boolean performRuntimeChecks() { return true; }

    public void addErrorToList(ValidationError error) {
        this.listOfValidationErrors.add(error);
    }

    public List<ValidationError> getValidationErrors() {
        return new LinkedList<>(listOfValidationErrors);
    }

    public void addAllErrorsToList(List<ValidationError> listOfErrors) {
        listOfValidationErrors.addAll(listOfErrors);
    }

    public Configuration getConfiguration() {
        return configuration;
    }


//TODO Check the following code if it is still of use and already available?
    //Create a script which contains a list of tool scripts and binary files which are used by the workflow.
//        //TODO Move some of those checks to the configuration validation. Especially missing tools, scripts and variables.
//        ExecutionContext clone = context.clone();
//        Configuration cfg = clone.getConfiguration();
//        clone.setExecutionContextLevel(ExecutionContextLevel.QUERY_STATUS);
//        clone.execute();
//        int contextErrorCountStart = context.getErrors().size();

//        Collection<String> calledTools = clone.getExecutedJobs().collect { Job j -> j.getToolID(); }.unique();
//        Collection<File> calledToolPaths = calledTools.collect { String toolID -> context.getConfiguration().getSourceToolPath(toolID) };
//        Collection<File> calledToolPathsOriginalList = new LinkedList<>(calledToolPaths);
//
//        // Parse each file and extract binary and tool entries this will surely be incomplete du to missing applied naming conventions!
//        // Tools / Scripts need to be named like TOOL_[name], binaries need to contain BINARY
//        List<String> allLines = [];
//        calledToolPaths.each { File f -> allLines += f.readLines() }
//
//        //TODO Skipping commentary lines should be based on the target file system
//        Collection<String> allUsedTools = allLines.findAll { String line -> !line.startsWith("#") && line.contains("{TOOL_") }; //Skip commentary lines
//        Collection<String> allUsedBinaries = allLines.findAll { String line -> !line.startsWith("#") && line.contains("BINARY}") };
//
//        List<String> extractedToolIDs = [];
//        List<String> extractedBinaries = [];
//        allUsedTools.each { String it -> it.split("[\\{,\\}]").each { String it2 -> if (it2.contains("TOOL_")) extractedToolIDs << it2 } }
//        allUsedBinaries.each { String it -> it.split("[\\{,\\}]").each { String it2 -> if (it2.contains("_BINARY")) extractedBinaries << it2 } }
//        extractedToolIDs = extractedToolIDs.unique().sort();
//        extractedBinaries = extractedBinaries.unique().sort();
//
//        //Convert back a TOOL_ID_NAME to idName
//        for (int i = 0; i < extractedToolIDs.size(); i++) {
//            String toolID = extractedToolIDs[i];
//            String oName = "";
//            String[] split = toolID.split("_");
//            for (int p = 1; p < split.length; p++) {
//                oName += split[p][0].toUpperCase() + split[p][1..-1].toLowerCase();
//            }
//            toolID = oName[0].toLowerCase() + oName[1..-1];
//            extractedToolIDs[i] = toolID;
//            File path = null;
//            try {
//                path = context.getConfiguration().getSourceToolPath(toolID);
//                calledToolPaths << path;
//            } catch (Exception ex) {
//                context.addErrorEntry(ExecutionContextError.EXECUTION_SCRIPT_NOTFOUND.expand("${toolID} - ${context.getConfiguration().getID()}"));
//            }
//        }
//
//        //Binaries are stored as configuration values, tools as tool entries.
//        //For tools, see if they exist and if they are in the roddy directory? So they will be available on the cluster.
//        calledToolPaths.each {
//            File file ->
//
//                boolean canRead = file.canRead();
//                boolean sizeIsGood = file.size() > 0;
//                if (!canRead && !sizeIsGood)
//                    context.addErrorEntry(ExecutionContextError.EXECUTION_SCRIPT_INVALID.expand(file.getAbsolutePath()));
//        }
//
//        //For binaries, see if they are available online.l
//        extractedBinaries.each {
//            String binary ->
//                File file = context.getConfiguration().getConfigurationValues().get(binary).toFile(context);
//                if (!Roddy.getInstance().isExecutable(file))
//                    context.addErrorEntry(ExecutionContextError.EXECUTION_BINARY_INVALID.expand("The binary ${binary} (file ${file.getAbsolutePath()}) could not be read."))
//        }
////        Roddy.getInstance().

    //Also check all variables in scripts.
    //Ideally, all input variables are in the form ${...}

//        calledToolPathsOriginalList.each { File f ->
//            List<String> allLinesInFile = f.readLines();
//            // Create a list of special characters which can occur in the complex variable syntax. The name is always in front of one of those.
//            List<String> specialChars = Arrays.asList("%", StringConstants.SPLIT_COLON, "/", "[-]", "[+]", '#');
//            // Keep a list of blacklistet variables, those are not checked.
//            // TODO This list has to be configurable, some can also be coming from the command factory.
//            List<String> blacklist = Arrays.asList("CONFIG_FILE", "PBS_*", "RODDY_*");
//            List<String> alreadyChecked = [];
////            Collection<String> allLinesWithVariables = allLinesInFile.findAll { String line -> !line.startsWith("#") && line.contains("\${") };
////            Collection<String> allLinesWithVariableDeclarations = allLinesInFile.findAll { String line -> !line.startsWith("#") && line.contains("\${") };
//
////            List<String> declaredVariableNames = [];
//            List<String> foundVariableNames = [];
//
//            for (String line in allLinesInFile) {
//                //Skip comments
//                line = line.trim();
//                if (line.startsWith("#")) continue;
//                if (line.length() == 0) continue;
//                //First step, find variable assignments
//                //These can either be declared using declare or by just assigning them
//                //Variables can be created in a [[ ]] && clause!
//                //Split by " " and parse every value.
//                String[] splitLine = line.split(" ");
//                for (String splitVar : splitLine) {
//                    if (!splitVar.contains("="))
//                        continue;
//                    String variableName = splitVar.split("=")[0];
////                    foundVariableNames << variableName;
//                    alreadyChecked << variableName;
//                }
//
//                //Second step, find variable usages
//                //Maybe also keep line number so you see if a variable was used before assignment if it was created in the script
//                String[] splitted = line.split("\\\$");
//                for (String splitVar in splitted) {
//                    if (splitVar.contains(StringConstants.BRACE_LEFT) && splitVar.contains(StringConstants.BRACE_RIGHT)) {
//                        String variableName = splitVar.split("[{]")[1].split(StringConstants.BRACE_RIGHT)[0].toString();
//                        foundVariableNames << variableName;
//                    }
//                }
//            }
//            //Third step, check all found variables
//            for (String variableName in foundVariableNames) {
//
//                if (variableName.contains("_BINARY") || variableName.startsWith("TOOL_"))
//                    continue;
//
//                specialChars.each { String chr -> variableName = variableName.split(chr)[0] }
//
//                if (alreadyChecked.contains(variableName))
//                    continue;
//                alreadyChecked << variableName;
//                boolean skip = false;
//                for (String entry in blacklist) {
//                    if (entry.endsWith("*")) {
//                        if (variableName.startsWith(entry[0..-2]))
//                            skip = true;
//                    } else {
//                        if (entry == variableName) {
//                            skip = true;
//                        }
//                    }
//                    if (skip)
//                        break;
//                }
//                if (skip)
//                    continue;
//
//                //Check if the variable is in the configuration
//                if (cfg.getConfigurationValues().hasValue(variableName))
//                    continue;
//
//                //Check if variable was defined somewhere in the script
//
//                //Check if variable was passed as a parameter
//
////                            println(varName);
//                context.addErrorEntry(ExecutionContextError.EXECUTION_SCRIPT_INVALID.expand("The variable ${variableName} is possibly not defined correctly: ${f.getAbsolutePath()}"));
//            }
//
//        }

    //See if there are new errors in the list.
//        return context.getErrors().size() - contextErrorCountStart == 0;
}
