/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.core.ExecutionContextError

import static de.dkfz.roddy.config.ConfigurationConstants.*
import static de.dkfz.roddy.StringConstants.*
/**
 * This class validates scripts.
 * Checked are:
 *  - The existence of each script
 *  - Calls to other scripts
 *  - TODO Access to scripts on runtime? Or is this for the ExecutionContext
 *  - Variable usage and declaration
 */
@groovy.transform.CompileStatic
public class ScriptValidator extends ConfigurationValidator {

    private List<File> listOfCalledScripts = [];

    private Map<File, String> toolIDsByScript = [:];

    // Create a list of special characters which can occur in the complex variable syntax. The name is always in front of one of those.
    // TODO This list has to be configurable, some can also be coming from the command factory.
    // Keep a list of blacklistet variables, those are not checked.
    public static final List<String> blacklist = Arrays.asList("WRAPPED_SCRIPT", "CONFIG_FILE", "PID", "PBS_*", "RODDY_*");

    public ScriptValidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate() {
        boolean good = validateScriptAvailability();

        good &= validateVariableUsage();

        return good;
    }

    public boolean validateVariableUsageInFile(File f, String toolID) {

        ScriptChecker checkerForFile = getScriptCheckerForFile(f, toolID);
        if (checkerForFile) checkerForFile.validateScript();

        return true;
    }

    public boolean validateVariableUsage() {
        //Also check all variables in scripts.
        //Ideally, all input variables are in the form ${...}
        boolean good = true;
        for (File f in listOfCalledScripts) {
            String toolID = toolIDsByScript[f];
            good &= validateVariableUsageInFile(f, toolID);
        }
        return false;
    }

    public boolean validateScriptAvailability() {
        List<String> scriptIDsToValidate = null;
        if (configuration instanceof AnalysisConfiguration) {
            scriptIDsToValidate = ((AnalysisConfiguration) configuration).getListOfUsedTools();
        }
        if (scriptIDsToValidate == null) //The value was not configured, so no checks will be made.
            return true;

        for (String scriptID : scriptIDsToValidate) {
            configuration.getSourceToolPath(scriptID);
        }

        //TODO Move script and tool extration to (Bash-)ScriptChecker
//        Collection<String> calledTools =  clone.getExecutedJobs().collect { BEJob j -> j.getTool(); }.unique();
        listOfCalledScripts = scriptIDsToValidate.collect { String toolID -> File path = configuration.getSourceToolPath(toolID); toolIDsByScript[path] = toolID; return path; };

//        Collection<File> calledToolPathsOriginalList = new LinkedList<>(calledToolPaths);

        // Parse each file and extract binary and tool entries this will surely be incomplete du to missing applied naming conventions!
        // Tools / Scripts need to be named like TOOL_[name], binaries need to contain BINARY
        List<String> allLines = [];
        for (File f in listOfCalledScripts) {
            allLines += f.readLines()
        }

        //TODO How to check base path usage in scripts?

        //TODO Skipping commentary lines should be based on the target file system
        Collection<String> allUsedTools = allLines.findAll { String line -> !line.startsWith(HASH) && line.contains(BRACE_LEFT + CVALUE_PREFIX_TOOL) }; //Skip commentary lines
        Collection<String> allUsedBinaries = allLines.findAll { String line -> !line.startsWith(HASH) && (line.contains(CVALUE_SUFFIX_BINARY + BRACE_RIGHT) || line.contains(CVALUE_SUFFIX_BINARY_SHORT + BRACE_RIGHT))};
        Collection<String> allUsedBasePaths = allLines.findAll { String line -> !line.startsWith(HASH) && line.contains(BRACE_LEFT + CVALUE_PREFIX_BASEPATH) };

        List<String> extractedToolIDs = [];
        List<String> extractedBinaries = [];
        List<String> extractedBasePaths = [];
        allUsedTools.each { String it -> it.split("[\\{,\\}]").each { String it2 -> if (it2.contains(CVALUE_PREFIX_TOOL)) extractedToolIDs << it2 } }
        allUsedBinaries.each { String it -> it.split("[\\{,\\}]").each { String it2 -> if (it2.contains(CVALUE_SUFFIX_BINARY) || it2.contains(CVALUE_SUFFIX_BINARY_SHORT)) extractedBinaries << it2 } }
        allUsedBasePaths.each { String it -> it.split("[\\{,\\}]").each { String it2 -> if (it2.contains(CVALUE_PREFIX_BASEPATH)) extractedBasePaths << it2 } }
        extractedToolIDs = extractedToolIDs.unique().sort();
        extractedBinaries = extractedBinaries.unique().sort();
        extractedBasePaths = extractedBasePaths.unique().sort();

        //Convert back a TOOL_ID_NAME to idName
        for (int i = 0; i < extractedToolIDs.size(); i++) {
            String toolID = extractedToolIDs[i];
            toolID = ConfigurationConverter.convertBackVariableName(toolID);
            extractedToolIDs[i] = toolID;
            try {
                File path = configuration.getSourceToolPath(toolID);
                listOfCalledScripts << path;
                toolIDsByScript[path] = toolID;
            } catch (Exception ex) {
                super.addErrorToList(new ValidationError(configuration, ExecutionContextError.EXECUTION_SCRIPT_NOTFOUND.description, "${toolID} - ${configuration.getID()}", ex));
            }
        }

        //Binaries are stored as configuration values, tools as tool entries.
        //For tools, see if they exist and if they are in the roddy directory? So they will be available on the cluster.
        for (File file in listOfCalledScripts) {
            boolean canRead = file.canRead();
            boolean sizeIsGood = file.size() > 0;
            if (!canRead && !sizeIsGood)
                super.addErrorToList(new ValidationError(configuration, ExecutionContextError.EXECUTION_SCRIPT_INVALID.description, file.getAbsolutePath(), null));
        }

        return true;
    }

    public ScriptChecker getScriptCheckerForFile(File f, String toolID) {
        //TODO Configure script checker types and endings. Don't hardcode it.
        if (f.getName().endsWith(".sh"))
            return new BashScriptChecker(f, toolID, this);
        return null;
    }

    @Override
    public boolean performRuntimeChecks() {
        return validateScriptAccessibility();
    }

    public boolean validateScriptAccessibility() {
        return true;
    }
}
