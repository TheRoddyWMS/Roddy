package de.dkfz.roddy.config.converters

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstant
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.ConfigurationValueBundle
import de.dkfz.roddy.config.InformationalConfigurationContent
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic

import java.util.logging.Level

/**
 * Converts a configuration object to bash script.
 * Created by heinold on 18.06.15.
 */
@groovy.transform.CompileStatic
class BashConverter extends ConfigurationConverter {

    //TODO Use a pipeline converter interface with methods like "convertCValues, convertCValueBundles, convertTools"
    @Override
    String convert(ExecutionContext context, Configuration cfg) {
        final FileSystemAccessProvider provider = FileSystemAccessProvider.getInstance();
        final String targetSystemNewLineString = provider.getNewLineString();
        final String separator = Constants.ENV_LINESEPARATOR;

        def values = cfg.getConfigurationValues().getAllValuesAsList();
        Map<String, ConfigurationValue> listOfUnsortedValues = [:]
        def listOfSortedValues = new LinkedHashMap<String, ConfigurationValue>();

        for (ConfigurationValue cv in values) {
            listOfUnsortedValues[cv.id] = cv;
        }

        boolean somethingChanged = true;
        int i = -1;
        while (somethingChanged) { //Passes
            somethingChanged = false;
            i++;
            if (LoggerWrapper.isVerbosityHigh())
                println "Pass ${i}, left ${listOfUnsortedValues.values().size()}";
            Map<String, ConfigurationValue> foundValues = [:];

            //TODO There must be a central blacklist for those things.
            List<String> valueBlacklist = ["PBS_JOBID", "PBS_ARRAYID", 'PWD', "PID", "pid", "sample", "run", "projectName", "testDataOptionID", "analysisMethodNameOnInput", "analysisMethodNameOnOutput"
                                           , "outputAnalysisBaseDirectory", "inputAnalysisBaseDirectory", "executionTimeString"]
            for (ConfigurationValue cv in listOfUnsortedValues.values()) {
                boolean isValidationRule = cv.id.contains("cfgValidationRule");
                if (isValidationRule)
                    continue;
                if (cv.toString().startsWith("#"))
                    continue;
                def dependencies = cv.getIDsForParrentValues();
                int noOfDependencies = dependencies.size();
                int noOfOriginalDependencies = dependencies.size();
                List<String> notFound = [];
                for (String dep : dependencies) {
                    if (listOfSortedValues.containsKey(dep) || valueBlacklist.contains(dep))
                        noOfDependencies--;
                    else
                        notFound << dep;
                }

                if (LoggerWrapper.isVerbosityHigh() && noOfDependencies > 0) {
                    println "NOT ACCEPTED: ${cv.id} = ${cv.value}"
                    for (String dep in notFound) {
                        println("Not found: ${dep}");
                    }
                    continue;
                }
                foundValues[cv.id] = cv;
                listOfSortedValues[cv.id] = cv;
            }
            if (foundValues.values().size() > 0)
                somethingChanged = true;
            listOfUnsortedValues -= foundValues;
        }

        if (LoggerWrapper.isVerbosityHigh())
            for (ConfigurationValue cv in listOfUnsortedValues.values()) {
                println "UP: ${cv.id} = ${cv.value}:";
            }
        listOfSortedValues += listOfUnsortedValues;

        StringBuilder text = new StringBuilder();
        text << "#!/bin/bash" << separator; //Add a shebang line

        //TODO The output umask and the group should be taken from a central location.
        String umask = context.getUMask();
        String outputFileGroup = context.getOutputGroupString();
        boolean processSetUserGroup = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, true);
        boolean processSetUserMask = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERMASK, true);
        text << separator << 'if [ -z "${PS1-}" ]; then' << separator << "\t echo non interactive process!" << separator << "else" << separator << "\t echo interactive process" << separator

        if (processSetUserMask) text << separator << "\t umask " << umask;
        text << separator << "fi" << separator << separator;

        for (ConfigurationValue cv : listOfSortedValues.values()) {
            boolean isValidationRule = cv.id.contains("cfgValidationRule");

            if (isValidationRule) {
                text << "# Validation rule!: " + cv.toString() << separator;
                continue;
            }

            convertConfigurationValueToShellScriptLine(cv, text, context)
            text << separator;
        }

        Map<String, ConfigurationValueBundle> cvBundles = cfg.getConfigurationValueBundles().getAllValues();
        for (String bKey : cvBundles.keySet()) {
            ConfigurationValueBundle bundle = cvBundles.get(bKey);
            text.append("#<").append(bKey).append(separator);
            for (String key : bundle.getKeys()) {
                ConfigurationValue cv = bundle.get(key);
                convertConfigurationValueToShellScriptLine(cv, text, context)
                text << separator;
            }
            text << "#>" << bKey << separator;
        }

        //Store tools
        for (ToolEntry te : cfg.getTools().getAllValuesAsList()) {
            String id = te.getID();
            String valueName = createVariableName("TOOL_", id);
            text << valueName << '="' << cfg.getProcessingToolPath(context, id) << '"' << separator;
        }

        boolean debugPipefail = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_USE_PIPEFAIL, true);
        boolean debugVerbose = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_USE_VERBOSE_OUTPUT, true);
        boolean debugXecute = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_USE_EXECUTE_OUTPUT, true);
        boolean debugUndefVar = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_USE_UNDEFINED_VARIABLE_BREAK, false);
        boolean debugExitOnError = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_USE_EXIT_ON_ERROR, false);
        boolean debugParseScripts = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.DEBUG_OPTIONS_PARSE_SCRIPTS, false);
        boolean processQueryEnv = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_QUERY_ENV, false);
        boolean processQueryID = cfg.getConfigurationValues().getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_QUERY_ID, false);

        text << separator << separator

        //TODO Convert those following calls to calls to the CommandSet. Otherwise it won't possibly work on different setups.
        //This is very system specific.
        if (debugPipefail) text << separator << "set -o pipefail";
        if (debugVerbose) text << separator << "set -v";
        if (debugXecute) text << separator << "set -x";
        if (debugUndefVar) text << separator << "set -u";
        if (debugExitOnError) text << separator << "set -e";
        if (debugParseScripts) text << separator << "set -n";

        if (processQueryID) text << separator << "id";
        if (processQueryEnv) text << separator << "env";

        //text << separator << '[[ ! ${TOOL_RESOLVE_WORKFLOW_DEPENDENCIES-null} -eq null ]] && source ${TOOL_RESOLVE_WORKFLOW_DEPENDENCIES}';

        //Set a path if necessary.
        text << separator << '[[ ! ${SET_PATH-} == "" ]] && export PATH=${SET_PATH}'

        text << separator << "";
        return text.toString();
    }

    @CompileStatic
    private void convertConfigurationValueToShellScriptLine(ConfigurationValue cv, StringBuilder text, ExecutionContext context) {
        if (cv.toString().startsWith("#COMMENT")) {
            text << cv.toString();
        } else {
            String tmp;
            if (cv.type && cv.type.toLowerCase() == "path")
                tmp = "${cv.toFile(context)}".toString();
            else {
                if (cv.value.startsWith("-") || cv.value.startsWith("*"))
                    tmp = "\"${cv.toString()}\"".toString();
                else
                    tmp = "${cv.toString()}".toString();
            }
            text << "${cv.id}=";
            //TODO Important, this is a serious hack! It must be removed soon
            if (tmp.startsWith("bundledFiles/"))
                text << Roddy.getApplicationDirectory().getAbsolutePath() << FileSystemAccessProvider.getInstance().getPathSeparator();
            if(cv.isQuoteOnConversionSet()) text << "'";
            text << tmp;
            if(cv.isQuoteOnConversionSet()) text << "'";
        }
    }



    public void importConfigurationFile(String configurationFile, String usedConfiguration) {
//        if (!usedConfiguration) {
//            throw new RuntimeException("Configuration has to be specified correctly!")
//        }
//        String[] cfgPath = usedConfiguration.split("\\.")
//        int depth = cfgPath.length
//
//        if (depth > 3 || depth == 0) {
//            throw new RuntimeException("Configuration path is not valid. Only three levels are allowed seperated by [.]")
//        }
//
//        String workflow = cfgPath[0]
//
//        Configuration configuration = loadConfiguration(cfgPath[0])
//        if (!configuration) {
//            throw new RuntimeException("Base configuration ${cfgPath[0]} is not available!")
//        }
//
//        Configuration temp = loadShellScript(configurationFile)
//        Map<String, ConfigurationValue> basePaths = [:]
//        Map<String, ToolEntry> toolEntries = [:]
//        Map<String, ConfigurationValue> cValues = temp.getConfigurationValues()
//
//        switch (depth) {
//            case 1: L: {
//                configuration.setConfigurationValues(cValues)
//                break;
//            }
//            case 2: L: {
//                String project = cfgPath[1]
//                if (!configuration.hasSubConfiguration(project)) {
//                    configuration.addSubConfiguration(new Configuration(configuration, project, "", cValues, temp.getConfigurationValueBundles(), basePaths, toolEntries, null))
//                }
//                configuration.getSubConfiguration(project).setConfigurationValues(cValues)
//                break;
//            }
//            case 3: L: {
//                String project = cfgPath[1]
//                if (!configuration.hasSubConfiguration(project)) {
//                    configuration.addSubConfiguration(new Configuration(configuration, project, "", null, null, basePaths, toolEntries, null))
//                }
//                Configuration cfgPrj = configuration.getSubConfiguration(project)
//                String variant = cfgPath[2]
//                if (!cfgPrj.hasSubConfiguration(variant)) {
//                    cfgPrj.addSubConfiguration(new Configuration(cfgPrj, variant, "", cValues, temp.getConfigurationValueBundles(), basePaths, toolEntries, null))
//                }
//                break;
//            }
//        }
//        writeConfiguration(configuration)
        //        pipelineConfigurationFiles/$
    }



    public Configuration loadShellScript(String configurationFile) {
        if (!configurationFile) {
            throw new IOException("Configuration file must be specified.")
        }
        File cf = new File(configurationFile)
        if (!cf.canRead()) {
            throw new IOException("Configuration file is not readable.")
        }

        //Filter input from configuration file
        int commentCnt = 0;
        List<String> cfFiltered = []
        HashMap<String, List<String>> valueBundles = new HashMap<String, List<String>>();
        List<String> currentBundle = null;
        String currentBundleName = "";

        cf.readLines().each { String line ->
            String l = line.trim()
            if (!l) return;
            if (l.startsWith("#<")) {
                currentBundle = new ArrayList<String>();
                currentBundleName = l[2..-1];
            } else if (l.startsWith("#>")) {
                //cfFiltered << ";VALUE_BUNDLE;" + currentBundle[0];
                valueBundles[currentBundleName] = currentBundle;
                currentBundle = null;
            } else if (l[0] == "#") {
                //TODO Also store comments
                if (currentBundle != null) {
                    currentBundle << String.format("#COMMENT_%04d=%s", commentCnt, l);
                } else {
                    cfFiltered << String.format("#COMMENT_%04d=%s", commentCnt, l);
                }
                commentCnt++;
            } else if (l.indexOf("=") > 0) {
                if (currentBundle != null) {
                    currentBundle << l
                } else {
                    cfFiltered << l
                }
            }
        }

        Map<String, Integer> doubletteCounter = new HashMap<String, Integer>();
        InformationalConfigurationContent userConfig = new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "userconfig_INVALIDNAME", "An imported configuration, please change the name and this description. Also set the classname and type as necessary.", null, null, "", null, null, "");
        Configuration newCfg = new Configuration(userConfig, (Map<String, Configuration>) null);
        Map<String, ConfigurationValue> cValues = newCfg.configurationValues.getMap();
        Map<String, ConfigurationValueBundle> cValueBundles = newCfg.configurationValueBundles.getMap();

        for (String cval in cfFiltered) {
            String[] cvarr = cval.split("=")
            String key = cvarr[0];
            int keyLen = key.length() + 1;
            String value = cval[keyLen..-1];

            try {
                String k2 = key;
                if (!cValues.containsKey(key)) {
                    doubletteCounter[key] = 0;
                } else {
                    doubletteCounter[key] = doubletteCounter[key] + 1;
                    k2 = String.format("%s_%04d", key, doubletteCounter[key]);
                }

                cValues[k2] = new ConfigurationValue(newCfg, k2, value);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        }

        valueBundles.each() {
            String bundleName, List<String> bundle ->
                Map<String, ConfigurationValue> bundleValues = new LinkedHashMap<String, ConfigurationValue>();
                try {
                    for (String cval in bundle) {
                        String[] cvarr = cval.split("=")
                        String key = cvarr[0];
                        bundleValues[key] = new ConfigurationValue(newCfg, key, cval[key.length() + 1..-1]);
                    }
                    cValueBundles[bundleName] = new ConfigurationValueBundle(bundleValues);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.toString());
                }
        }


        return newCfg
    }
}
