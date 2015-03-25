package de.dkfz.roddy.execution.io

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.*
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.execution.jobs.JobDependencyID
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic

import java.util.logging.Level

import static de.dkfz.roddy.StringConstants.TILDE
import static de.dkfz.roddy.config.ConfigurationConstants.*

/**
 * Execution services context commands locally or remotely. Some specific commands (copy file or directory) are also available as those can (hopefully) be created by all service implementations.
 * i.e. local file handling is done by Java itself wheres remote handling with ssh is done via the ssh library
 *
 */
@CompileStatic
public abstract class ExecutionService extends CacheProvider {
    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionService.class.name);
    private static ExecutionService executionService;

    public static final String RODDY_CVALUE_DIRECTORY_LOCKFILES = "DIR_LOCKFILES"
    public static final String RODDY_CVALUE_DIRECTORY_TEMP = "DIR_TEMP"
    public static final String RODDY_CVALUE_DIRECTORY_EXECUTION = "DIR_EXECUTION"
    public static final String RODDY_CVALUE_DIRECTORY_EXECUTION_COMMON = "DIR_EXECUTION_COMMON"
    public static final String RODDY_CVALUE_DIRECTORY_RODDY_APPLICATION = "DIR_RODDY"
    public static final String RODDY_CVALUE_DIRECTORY_BUNDLED_FILES = "DIR_BUNDLED_FILES"
    public static final String RODDY_CVALUE_DIRECTORY_ANALYSIS_TOOLS = "DIR_ANALYSIS_TOOLS"
    public static final String RODDY_CVALUE_JOBSTATE_LOGFILE = "jobStateLogFile"

    /**
     * Specifies a list of dataSet's for which the creation of jobs is not allowed.
     */
    protected final LinkedList<String> blockedPIDsForJobExecution = new LinkedList<String>();

    /**
     * Specifies, that the execution service must not execute any further jobs.
     */
    protected boolean allJobsBlocked = false;

    public static void initializeService(boolean fullSetup) {

        if (!fullSetup) {
            executionService = new NoNoExecutionService();
            return;
        }

        ClassLoader classLoader = Roddy.class.getClassLoader();

        String executionServiceClassID = null;
        Roddy.RunMode runMode = Roddy.getRunMode();
        executionServiceClassID = Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, SSHExecutionService.class.getName());
        Class executionServiceClass = classLoader.loadClass(executionServiceClassID);
        executionService = (ExecutionService) executionServiceClass.getConstructors()[0].newInstance();
        if (runMode == Roddy.RunMode.CLI) {
            boolean isConnected = executionService.tryInitialize(true);
            if (!isConnected) {
                int queryCount = 0;
                //If password is not stored, ask once for the password only, increase queryCount.
                if (!Boolean.parseBoolean(Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, Boolean.FALSE.toString()))) {
                    Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD);
                    RoddyCLIClient.askForPassword();
                    isConnected = executionService.tryInitialize(true);
                    queryCount++;
                }
                for (; queryCount < 3 && !isConnected; queryCount++) {
                    //Wait for correct password, count to three?
                    RoddyCLIClient.performCommandLineSetup();
                    isConnected = executionService.tryInitialize(true);
                }
            }
        } else {
            executionService.initialize();
        }
    }

    public ExecutionService() {
        super("ExecutionService", true);
    }

    public static ExecutionService getInstance() {
        return executionService;
    }

    public void addSpecificSettingsToConfiguration(Configuration configuration) {

    }

    public boolean isLocalService() {
        return true;
    }

    public boolean initialize() {
        initialize(false);
    }

    public boolean tryInitialize(boolean waitFor) {
        try {
            return initialize(waitFor);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean tryInitialize() {
        try {
            return initialize();
        } catch (Exception ex) {
            return false;
        }
    }

    public void destroy() {
        this.listOfListeners.clear();
    }

    protected
    abstract List<String> _execute(String string, boolean waitFor, boolean ignoreErrors, OutputStream outputStream);

    public ExecutionResult execute(String string, boolean waitFor = true, OutputStream outputStream = null) {
        ExecutionResult er = null;
        if (string) {
            List<String> result = _execute(string, waitFor, true, outputStream);
//            System.out.println("" + (outputStream != null));
//            for (String line : result) {
//                System.out.println(line);
//            }
            String returnCodeStr = result[0];
            String processID = result[1];
            if (returnCodeStr == null) {
                returnCodeStr = 65535;
                result << "65535";
            }
            int returnCode = Integer.parseInt(returnCodeStr);
            result.remove(0);
            result.remove(0);
            er = new ExecutionResult(returnCode == 0, returnCode, result, processID);
        } else {
            er = new ExecutionResult(false, -1, Arrays.asList("Command not valid. String is empty."), "");
        }
        fireStringExecutedEvent(string, er);
        return er;
    }

    public void execute(Command command, boolean waitFor = true) {
        ExecutionContext run = command.getExecutionContext();
        boolean configurationDisallowsJobSubmission = Roddy.getApplicationProperty(Constants.APP_PROPERTY_APPLICATION_DEBUG_TAGS, "").contains(Constants.APP_PROPERTY_APPLICATION_DEBUG_TAG_NOJOBSUBMISSION);
        boolean preventCalls = run.getConfiguration().getPreventJobExecution();
        boolean pidIsBlocked = blockedPIDsForJobExecution.contains(command.getExecutionContext().getDataSet());
        boolean isDummyCommand = Command instanceof Command.DummyCommand;

        String cmdString;
        if (!configurationDisallowsJobSubmission && !allJobsBlocked && !pidIsBlocked && !preventCalls && !isDummyCommand) {
            try {
                cmdString = command.toString();
                //Store away process output if this is a local service.
                File tmpFile = isLocalService() ? File.createTempFile("roddy_", "_temporaryLogfileStream") : null;
                OutputStream outputStream = isLocalService() && waitFor && command.isBlockingCommand() ? new FileOutputStream(tmpFile) : null;

                ExecutionResult res = null;
                if (run.getExecutionContextLevel() == ExecutionContextLevel.TESTRERUN) {
                    String pid = String.format("0x%08X", System.nanoTime());
                    res = new ExecutionResult(true, 0, [pid], pid);
                } else
                    res = execute(cmdString, waitFor, outputStream);

                command.getJob().setJobState(!res.successful ? JobState.FAILED : JobState.OK);

                if (isLocalService() && command.isBlockingCommand()) {
                    command.setExecutionID(CommandFactory.getInstance().createJobDependencyID(command.getJob(), res.processID));

                    File logFile = command.getExecutionContext().getRuntimeService().getLogFileForCommand(command)
                    FileSystemInfoProvider.getInstance().moveFile(tmpFile, logFile);
                } else if (res.successful) {
                    String exID = CommandFactory.getInstance().parseJobID(res.resultLines[0]);
                    command.setExecutionID(CommandFactory.getInstance().createJobDependencyID(command.getJob(), exID));
                    CommandFactory.getInstance().storeJobStateInfo(command.getJob());
                }
                command.getExecutionContext().addCalledCommand(command);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        } else {
            StringBuilder reason = new StringBuilder();
            reason << configurationDisallowsJobSubmission ? "Application writeConfigurationFile does not allow job submission. " : "";
            reason << preventCalls ? "Configuration does not allow the execution of commands. " : "";
            reason << pidIsBlocked ? "The execution of jobs for this DataSet is stopped. " : "";
            reason << allJobsBlocked ? "The execution service is no longer allowed to execute commands. " : "";
            logger.postSometimesInfo("Skipping command " + command + " for reason: " + reason);
        }
        fireCommandExecutedEvent(command);
    }

    public static long measureStart() { return System.nanoTime(); }

    public static double measureStop(long startValue, String measurementID) {
        double t = (System.nanoTime() - startValue) / 1000000.0;
        if (measurementID != null)
            logger.postRareInfo("Execution of ${measurementID} took ${t} ms.")
        return t;
    }

    public boolean checkContextPermissions(ExecutionContext context) {
        //Create a script which contains a list of tool scripts and binary files which are
        //used by the workflow.

        //TODO Move some of those checks to the configuration validation. Especially missing tools, scripts and variables.

        ExecutionContext clone = context.clone();
        Configuration cfg = clone.getConfiguration();
        clone.setExecutionContextLevel(ExecutionContextLevel.QUERY_STATUS);
        clone.execute();
        int contextErrorCountStart = context.getErrors().size();

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
        return context.getErrors().size() - contextErrorCountStart == 0;
    }

    private class CompressedArchiveInfo {
        CompressedArchiveInfo(File localArchive, String md5, File folder) {
            this.localArchive = localArchive
            this.md5 = md5
            this.folder = folder
        }

        boolean validOnRemoteSite;
        String md5;
        File folder;
        File localArchive;
        File removeArchive;
    }

    private long lastExecutionContextCheckpoint = -1100;
    Map<File, CompressedArchiveInfo> mapOfPreviouslyCompressedArchivesByFolder = [:];

    /**
     * Writes files like the scripts in the analysisTools directory, the current configuration (shellscript and xml) and files with debug information to the runs logging directory.
     *
     * @param context
     */
    public void writeFilesForExecution(ExecutionContext context) {
        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_INIT);

        FileSystemInfoProvider provider = FileSystemInfoProvider.getInstance();
        ConfigurationFactory cfgService = ConfigurationFactory.getInstance();

        String analysisID = context.getAnalysis().getName();

        File execCacheFile = context.getRuntimeService().getNameOfExecCacheFile(context.getAnalysis());
        String execCacheFileLock = new File(execCacheFile.getAbsolutePath() + TILDE).getAbsolutePath();
        File executionBaseDirectory = context.getRuntimeService().getBaseExecutionDirectory(context);
        File executionDirectory = context.getExecutionDirectory();
        File analysisToolsDirectory = context.getAnalysisToolsDirectory();
        File temporaryDirectory = context.getTemporaryDirectory();
        File lockFilesDirectory = context.getLockFilesDirectory();
        File commonExecutionDirectory = context.getCommonExecutionDirectory();
        File roddyApplicationDirectory = Roddy.getApplicationDirectory();

        //Base path for the application. This path might not be available on the target system (i.e. because of ssh calls).
        File roddyBundledFilesDirectory = Roddy.getBundledFilesDirectory();

        //Those are files, that are available within the roddy directory/bundledFiles but those are not copied anywhere!This path might not be available on the target system (i.e. because of ssh calls).

        if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs) {
            provider.checkDirectory(executionBaseDirectory, context, true);
            provider.checkDirectory(executionDirectory, context, true);
            provider.checkDirectory(executionDirectory, context, true);
            provider.checkDirectory(temporaryDirectory, context, true);
            provider.checkDirectory(lockFilesDirectory, context, true);
            logger.postAlwaysInfo("Creating the following execution directory to store information about this process:")
            logger.postAlwaysInfo("\t${executionDirectory.getAbsolutePath()}");
        }

        Configuration cfg = context.getConfiguration();
        def configurationValues = cfg.getConfigurationValues()

        CommandFactory.getInstance().addSpecificSettingsToConfiguration(cfg)
        getInstance().addSpecificSettingsToConfiguration(cfg)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_LOCKFILES, lockFilesDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_TEMP, temporaryDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION, executionDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION_COMMON, commonExecutionDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_RODDY_APPLICATION, roddyApplicationDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_BUNDLED_FILES, roddyBundledFilesDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_ANALYSIS_TOOLS, analysisToolsDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_JOBSTATE_LOGFILE, executionDirectory.getAbsolutePath() + FileSystemInfoProvider.getInstance().getPathSeparator() + RODDY_JOBSTATE_LOGFILE, CVALUE_TYPE_STRING);

        if (!context.getExecutionContextLevel().canSubmitJobs) return;

        final boolean ATOMIC = true;
        final boolean BLOCKING = true;
        final boolean NON_BLOCKING = false;

        provider.createFileWithDefaultAccessRights(ATOMIC, context.getRuntimeService().getNameOfJobStateLogFile(context), context, BLOCKING);
        provider.checkFile(execCacheFile, true, context);
        provider.appendLineToFile(ATOMIC, execCacheFile, String.format("%s,%s,%s", executionDirectory.getAbsolutePath(), analysisID, provider.callWhoAmI()), NON_BLOCKING);

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_COPY_TOOLS);
        copyAnalysisToolsForContext(context);

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_COPY_CONFIG);

        //Current config
        String configText = cfgService.convertConfigurationToShellscript(context, cfg);
        provider.writeTextFile(context.getProject().getRuntimeService().getNameOfConfigurationFile(context), configText, context);

        //Current configs xml files (default, user, pipeline config file)
        String configXML = cfgService.convertConfigurationToXML(cfg);
        provider.writeTextFile(context.getProject().getRuntimeService().getNameOfXMLConfigurationFile(context), configXML, context);
        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_RUN);
    }

    /**
     * Copy and link analysis tools folders to the target analysis tools directory. Analysis tools folder names must be unique over all plugins.
     * However this is not enforced.
     * @param context
     */
    private void copyAnalysisToolsForContext(ExecutionContext context) {
        FileSystemInfoProvider provider = FileSystemInfoProvider.getInstance();
        Configuration cfg = context.getConfiguration();
        File dstExecutionDirectory = context.getExecutionDirectory();
        File dstAnalysisToolsDirectory = context.getAnalysisToolsDirectory();
        File dstCommonExecutionDirectory = context.getCommonExecutionDirectory();
        boolean useCentralAnalysisArchive = cfg.getUseCentralAnalysisArchive();

        //Current analysisTools directory (they are also used for execution)
//        File sourcePath = new File(".", analysisToolsDirectory.name);
        List<File> sourcePaths = new LinkedList<>(Arrays.asList(RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "resources", "analysisTools").listFiles()));
//        Map<String, File> allToolDirectories = new LinkedHashMap<>();
        for (PluginInfo pluginInfo : LibrariesFactory.getInstance().getLoadedPlugins()) {
//            allToolDirectories.putAll(pluginInfo.getToolsDirectories());
            sourcePaths.addAll(pluginInfo.getToolsDirectories().values());
        }

        provider.checkDirectory(dstExecutionDirectory, context, true);
        List<String> usedToolFolders = ((AnalysisConfiguration) context.getAnalysis().getConfiguration()).getUsedToolFolders();
        if (useCentralAnalysisArchive) {
            String[] existingArchives = provider.loadTextFile(context.getFileForAnalysisToolsArchiveOverview());
            Roddy.getCompressedAnalysisToolsDirectory().mkdir();

            Collection<File> listOfFolders = sourcePaths.findAll { File it -> !it.getName().contains(".svn"); }

            //We always copy the roddy folder as this folder is used by all scripts.
            if (usedToolFolders.size() > 0)
                listOfFolders = listOfFolders.findAll { File it -> it.getName().equals("roddyTools") || usedToolFolders.contains(it.getName()); }

            //Add used base paths to configuration.
            for (File folder : listOfFolders) {
                def bPathID = folder.getName()
                String basepathConfigurationID = ConfigurationFactory.createVariableName(ConfigurationConstants.CVALUE_PREFIX_BASEPATH, bPathID);
                cfg.getConfigurationValues().add(new ConfigurationValue(basepathConfigurationID, RoddyIOHelperMethods.assembleLocalPath(dstExecutionDirectory, "analysisTools", bPathID).getAbsolutePath(), "string"));
            }

            listOfFolders.parallelStream().forEach {
                File subFolder ->
                    String md5sum = RoddyIOHelperMethods.getSingleMD5OfFilesInDirectory(subFolder);
                    String zipFilename = "roddy_compressedArchive_analysisTools_pluginID_" + subFolder.getName() + ".zip";
                    String zipMD5Filename = zipFilename + "_contentmd5";
                    File tempFile = new File(Roddy.getCompressedAnalysisToolsDirectory(), zipFilename);
                    File zipMD5File = new File(Roddy.getCompressedAnalysisToolsDirectory(), zipMD5Filename);

                    boolean createNew = false;
                    if (!tempFile.exists())
                        createNew = true;

                    if (!zipMD5File.exists() || zipMD5File.text.trim() != md5sum)
                        createNew = true;

                    if (createNew) {
                        RoddyIOHelperMethods.compressDirectory(subFolder, tempFile)
                        zipMD5File.withWriter { BufferedWriter bw -> bw.writeLine(md5sum); }
                    }

                    String newArchiveMD5 = md5sum;
                    if (tempFile.size() == 0)
                        logger.severe("The size of archive ${tempFile.getName()} is 0!")
                    synchronized (mapOfPreviouslyCompressedArchivesByFolder) {
                        mapOfPreviouslyCompressedArchivesByFolder[subFolder] = new CompressedArchiveInfo(tempFile, newArchiveMD5, subFolder);
                    }
            };

//            listOfFolders.parallelStream().forEach() {
//                File subFolder ->
            for (File subFolder : listOfFolders) {
//                File remoteFile = File.createTempFile("roddy_", "_compressedArchive_analysisTools_pluginID_" + subFolder.getName() + ".zip");
                File localFile = mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive;
                File remoteFile = new File(mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive.getName()[0..-5] + "_" + context.getTimeStampString() + ".zip");
                String archiveMD5 = mapOfPreviouslyCompressedArchivesByFolder[subFolder].md5

                String foundExisting = null;
                String subFolderOnRemote = subFolder.getName();
                for (String line : existingArchives) {
                    String[] split = line.split(StringConstants.SPLIT_COLON);
                    if (split.length != 2) continue;
                    String existingFilePath = split[0];
                    String existingFileMD5 = split[1];

                    if (existingFileMD5.equals(archiveMD5)) {
                        foundExisting = existingFilePath;
                        remoteFile = new File(remoteFile.getParentFile(), existingFilePath);
                        subFolderOnRemote = remoteFile.getName().split(StringConstants.SPLIT_UNDERSCORE)[-3];
                        //TODO This is seriously a hack.
                        break;
                    }
                }

                File analysisToolsServerDir = null;
                if (foundExisting) {
                    //remoteFile.delete(); //Don't need that anymore
                    analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + foundExisting);
                    logger.postSometimesInfo("Skipping copy of file ${remoteFile.getName()}, a file with the same md5 was found.")
                } else {
                    //TODO This one is a really huge mess, make it good.

                    analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + remoteFile.getName())
                    provider.checkDirectory(dstCommonExecutionDirectory, context, true);
                    provider.checkDirectory(analysisToolsServerDir, context, true);
                    provider.copyFile(localFile, new File(dstCommonExecutionDirectory, remoteFile.getName()), context);
                    provider.checkFile(context.getFileForAnalysisToolsArchiveOverview(), true, context);
                    provider.appendLineToFile(true, context.getFileForAnalysisToolsArchiveOverview(), "${remoteFile.getName()}:${archiveMD5}", true);

                    GString str = RoddyIOHelperMethods.getCompressor().getDecompressionString(new File(dstCommonExecutionDirectory, remoteFile.getName()), analysisToolsServerDir, analysisToolsServerDir);
                    getInstance().execute(str, true);
                    provider.setDefaultAccessRightsRecursively(new File(analysisToolsServerDir.getAbsolutePath()), context);

                }
                provider.checkDirectory(dstAnalysisToolsDirectory, context, true);

                def linkCommand = "ln -s ${analysisToolsServerDir.getAbsolutePath()}/${subFolderOnRemote} ${dstAnalysisToolsDirectory.absolutePath}/${subFolder.getName()}"
                getInstance().execute(linkCommand, true);
            }


        } else {
            for (File sourcePath : sourcePaths) {
                provider.checkDirectory(dstAnalysisToolsDirectory, context, true);
                provider.copyDirectory(sourcePath, dstAnalysisToolsDirectory);
                provider.setDefaultAccessRightsRecursively(dstAnalysisToolsDirectory, context);
            }
        }
    }

    /**
     * Writes things like a command call file
     *
     * @param context
     */
    public void writeAdditionalFilesAfterExecution(ExecutionContext context) {
        if (!context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs) return;

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_FINALIZE_CREATE_JOBFILES);

        final FileSystemInfoProvider provider = FileSystemInfoProvider.getInstance();
        final String separator = Constants.ENV_LINESEPARATOR;

        List<Command> commandCalls = context.getCommandCalls();
        StringBuilder realCalls = new StringBuilder();
        List<JobDependencyID> jobIDs = new LinkedList<JobDependencyID>();
        int cnt = 0;
        Map<String, String> jobIDReplacement = new HashMap<String, String>();
        StringBuilder repeatCalls = new StringBuilder();

        for (Command c : commandCalls) {
            JobDependencyID eID = c.getExecutionID();
            if (eID != null) {
                jobIDs.add(eID);
                if (eID.getShortID() != null) {
                    jobIDReplacement.put(eID.getShortID(), String.format('$j_' + "%08d", cnt++));
                }
            }
        }
        for (Command c : commandCalls) {
            JobDependencyID eID = c.getExecutionID();
            String cmdStr = c.toString();
            realCalls.append(eID).append(", ").append(cmdStr).append(separator);

            String repeatCallLine = String.format("%s=`%s`" + separator, eID, cmdStr);
            for (String k : jobIDReplacement.keySet()) {
                String v = jobIDReplacement.get(k);

                repeatCallLine = repeatCallLine.replace(k, v);
            }
            repeatCalls.append(repeatCallLine.substring(1));
        }

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_FINALIZE_CREATE_BINARYFILES);

        RuntimeService rService = context.getRuntimeService();
        final File realCallFile = rService.getNameOfRealCallsFile(context);
        final File repeatCallFile = rService.getNameOfRepeatableJobCallsFile(context);
        provider.writeTextFile(realCallFile, realCalls.toString(), context);
        provider.writeTextFile(repeatCallFile, repeatCalls.toString(), context);
        rService.writeJobInfoFile(context);
    }

    public boolean doesKnowTheUsername() { return false; }

    public String getUsername() {}

    public boolean canCopyFiles() { return false; }

    public boolean canReadFiles() { return false; }

    public boolean canWriteFiles() { return false; }

    public boolean canDeleteFiles() { false; }

    public boolean canListFiles() { return false; }

    public boolean canModifyAccessRights() { return false; }

    public boolean canQueryFileAttributes() { return false; }

    public void writeTextFile(File file, String text) {}

    public void writeBinaryFile(File file, Serializable serializable) {}

    public void copyFile(File _in, File _out) {}

    public void copyDirectory(File _in, File _out) {}

    public void moveFile(File _from, File _to) {}

    public boolean modifyAccessRights(File file, String rightsStr, String groupID) {}

    public void createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {}

    public void removeDirectory(File directory) {}

    public void removeFile(File file) {}

    public void appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {}

    public void appendLineToFile(boolean atomic, File file, String line, boolean blocking) {}

    public Object loadBinaryFile(File file) {}

    public String[] loadTextFile(File file) {}

    public List<File> listFiles(File file, List<String> filters) {}

    public List<File> listFiles(List<File> file, List<String> filters) {}

    public boolean isFileReadable(File f) {}

    public boolean isFileExecutable(File f) {}

    public FileAttributes queryFileAttributes(File file) {}

    public Map<File, Boolean> getReadabilityOfAllFiles(List<File> listOfFiles) {
        List<File> allPaths = [];
        for (File file in listOfFiles) {
            if(file == null) {
                //TODO Print out an error message? Or just ignore this because the error is already noted somewhere else.
                continue;
            }
            if (!allPaths.contains(file.parentFile))
                allPaths << file.parentFile;
        }

        Map<File, Boolean> readability = [:];
        List<File> allFiles = listFiles(allPaths, null);
        for (File file in listOfFiles)
            readability[file] = allFiles.contains(file);

        return readability;
    }

    private LinkedList<ExecutionServiceListener> listOfListeners = new LinkedList<ExecutionServiceListener>();

    public void registerExecutionListener(ExecutionServiceListener listener) {
        listOfListeners.add(listener);
    }

    public void removeExecutionListener(ExecutionServiceListener listener) {
        listOfListeners.remove(listener);
    }

    public void fireStringExecutedEvent(String commandString, ExecutionResult result) {
        listOfListeners.each { ExecutionServiceListener esl -> esl.stringExecuted(commandString, result); }
    }

    public void fireCommandExecutedEvent(Command result) {
        listOfListeners.each { ExecutionServiceListener esl -> esl.commandExecuted(result); }
    }


    public void fireExecutionServiceStateChange(TriState state) {
        listOfListeners.each { ExecutionServiceListener esl -> esl.changeExecutionServiceState(state); }
    }

    public long fireExecutionStartedEvent(String message) {
        long id = System.nanoTime();
        listOfListeners.each { ExecutionServiceListener esl -> esl.executionStarted(id, message) }
        return id;
    }

    public void fireExecutionStoppedEvent(long id, String message) {
        listOfListeners.each { ExecutionServiceListener esl -> esl.executionFinished(id, message) }
    }

    /**
     * Tries to stop the execution of jobs for a specific dataSet
     *
     * @param dataSet
     */
    public synchronized void stopExecution(DataSet dataSet) {
//        MARK:
//        if (!blockedPIDsForJobExecution.contains(dataSet))
//            blockedPIDsForJobExecution.add(dataSet);
    }

    /**
     * Tries to stop the execution of all pending jobs.
     */
    public synchronized void stopExecution() {
        allJobsBlocked = true;
    }

    public boolean needsPassword() { return false; }

    public boolean testConnection() { return true; }

    /**
     * For remote files
     * @param file
     * @return
     */
    public File getTemporaryFileForFile(File file) { return null; }

    public abstract boolean isAvailable();
}
