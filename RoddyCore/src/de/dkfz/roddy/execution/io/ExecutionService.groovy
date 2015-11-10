package de.dkfz.roddy.execution.io

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.RunMode
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.config.converters.XMLConverter
import de.dkfz.roddy.core.*
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
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
 * Execution services commands locally or remotely. Some specific commands (copy file or directory) are also available as those can (hopefully) be created by all service implementations.
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

    public static void initializeService(Class executionServiceClass, RunMode runMode) {
        executionService = (ExecutionService) executionServiceClass.getConstructors()[0].newInstance();
        if (runMode == RunMode.CLI) {
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

    public static void initializeService(boolean fullSetup) {

        if (!fullSetup) {
            executionService = new NoNoExecutionService();
            return;
        }

        ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader();

        String executionServiceClassID = null;
        RunMode runMode = Roddy.getRunMode();
        executionServiceClassID = Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, SSHExecutionService.class.getName());
        Class executionServiceClass = classLoader.loadClass(executionServiceClassID);
        initializeService(executionServiceClass, runMode);
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

    protected abstract List<String> _execute(String string, boolean waitFor, boolean ignoreErrors, OutputStream outputStream);

    public ExecutionResult execute(String string, boolean waitFor = true, OutputStream outputStream = null) {
        ExecutionResult er = null;
        if (string) {
            List<String> result = _execute(string, waitFor, true, outputStream);
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
                } else {
                    res = execute(cmdString, waitFor, outputStream);
                }
                command.getJob().setJobState(!res.successful ? JobState.FAILED : JobState.OK);

                if (isLocalService() && command.isBlockingCommand()) {
                    command.setExecutionID(CommandFactory.getInstance().createJobDependencyID(command.getJob(), res.processID));

                    File logFile = command.getExecutionContext().getRuntimeService().getLogFileForCommand(command)
                    FileSystemAccessProvider.getInstance().moveFile(tmpFile, logFile);
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

    public static double measureStop(long startValue, String measurementID, int verbosityLevel = LoggerWrapper.VERBOSITY_RARE) {
        double t = (System.nanoTime() - startValue) / 1000000.0;
        if (measurementID != null)
            logger.log(verbosityLevel, "Execution of ${measurementID} took ${t} ms.")
        return t;
    }

    /**
     * Check different directories and files to see, if files are accessible.
     * Will not check everything but at least some of the well-known sources of errors.
     * The method also check for some files, if they exist. If so, the read / write access will be checked.
     * If a file does not exist yet, it will not be checked!
     * @param context The context object which needs to be checked.
     * @return
     */
    public boolean checkContextPermissions(ExecutionContext context) {
        FileSystemAccessProvider fis = FileSystemAccessProvider.getInstance();
        Analysis analysis = context.getAnalysis()

        //First check in and output directories for accessibility

        File inputBaseDirectory = analysis.getInputBaseDirectory()      //Project input directory with i.e. ../view-by-pid
        File outputBaseDirectory = analysis.getOutputBaseDirectory()    //Project output directory with i.e. ../results_per_pid
        File outputDirectory = context.getOutputDirectory()             //Output with dataset id

        File projectExecCacheFile = context.getRuntimeService().getNameOfExecCacheFile(context.getAnalysis())   // .roddyExecCache.txt containing the list of executed runs in the project output folder
        File projectExecutionDirectory = context.getCommonExecutionDirectory()  // .roddyExecutionDirectory in outputBaseDirectory
        File projectToolsMD5SumFile = context.getFileForAnalysisToolsArchiveOverview()  // The md5 sum file in .roddyExecutionDirectory
        File baseContextExecutionDirectory = context.getRuntimeService().getBaseExecutionDirectory(context) //roddyExecutionStore in the dataset folder
        File contextExecutionDirectory = context.getExecutionDirectory()        // the exec_... folder int he base context exec dir. (NOT CHECKED, created later!)

        Boolean inputIsReadable = fis.isReadable(inputBaseDirectory);
        Boolean outputIsWriteable = fis.directoryExists(outputBaseDirectory) ? fis.isReadable(outputBaseDirectory) && fis.isWritable(outputBaseDirectory) : null;
        Boolean datasetDirIsWritable = fis.directoryExists(outputDirectory) ? fis.isReadable(outputDirectory) && fis.isWritable(outputDirectory) : null;

        Boolean projectExecCacheFileIsWritable = outputIsWriteable && fis.fileExists(projectExecCacheFile) ? fis.isReadable(projectExecCacheFile) && fis.isWritable(projectExecCacheFile) : null
        Boolean projectExecutionContextDirIsWritable = fis.directoryExists(projectExecutionDirectory) ? fis.isReadable(projectExecutionDirectory) && fis.directoryExists(projectExecutionDirectory) : null;
        Boolean projectToolsMD5SumFileIsWritable = fis.fileExists(projectToolsMD5SumFile) ? fis.isReadable(projectToolsMD5SumFile) && fis.isWritable(projectToolsMD5SumFile) : projectExecutionContextDirIsWritable
        Boolean baseContextDirIsWritable = fis.directoryExists(baseContextExecutionDirectory) ? fis.isReadable(baseContextExecutionDirectory) && fis.isWritable(baseContextExecutionDirectory) : null;

        int countErrors = context.getErrors().sum(0) { ExecutionContextError ece -> ece.getErrorLevel() == Level.SEVERE ? 1 : 0 } as Integer


        if (outputIsWriteable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output dir is missing: ${outputBaseDirectory}", Level.WARNING));
        else if (outputIsWriteable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("Output dir is not writable: ${outputBaseDirectory}"));

        if (datasetDirIsWritable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output dir is missing: ${outputDirectory}", Level.WARNING));
        else if (datasetDirIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("Output dir is not writable: ${outputDirectory}"));

        if (projectExecCacheFileIsWritable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output file is missing: ${projectExecCacheFile}", Level.WARNING));
        else if (projectExecCacheFileIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("The projects exec cache file is not writable: ${projectExecCacheFile}"));

        if (projectExecutionContextDirIsWritable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output dir is missing: ${projectExecutionDirectory}", Level.WARNING));
        else if (projectExecutionContextDirIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("The project execution store is not writable: ${projectExecutionDirectory}"));

        if (projectToolsMD5SumFileIsWritable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output file is missing: ${projectToolsMD5SumFile}", Level.WARNING));
        else if (projectToolsMD5SumFileIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("The project md5sum file is not writable: ${projectToolsMD5SumFile}"));

        if (baseContextDirIsWritable == null)
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.expand("Output dir is missing: ${baseContextExecutionDirectory}", Level.WARNING));
        else if (baseContextDirIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.expand("The datasets execution storeage folder is not writable: ${baseContextExecutionDirectory}"));

        //Just check, if there were new errors.
        return (context.getErrors().sum(0) { ExecutionContextError ece -> ece.getErrorLevel() == Level.SEVERE ? 1 : 0 } as Integer) - countErrors == 0;
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

        FileSystemAccessProvider provider = FileSystemAccessProvider.getInstance();
        ConfigurationFactory configurationFactory = ConfigurationFactory.getInstance();

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

        if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs) {
            provider.checkDirectories([executionBaseDirectory, executionDirectory, temporaryDirectory, lockFilesDirectory], context, true);
            logger.postAlwaysInfo("Creating the following execution directory to store information about this process:")
            logger.postAlwaysInfo("\t${executionDirectory.getAbsolutePath()}");
        }

        Configuration cfg = context.getConfiguration();
        def configurationValues = cfg.getConfigurationValues()

        CommandFactory.getInstance().addSpecificSettingsToConfiguration(cfg)
        getInstance().addSpecificSettingsToConfiguration(cfg)

        //Add feature toggles to configuration
        AvailableFeatureToggles.values().each {
            AvailableFeatureToggles toggle ->
                configurationValues.put(toggle.name(), ((Boolean) Roddy.getFeatureToggleValue(toggle)).toString(), CVALUE_TYPE_BOOLEAN);
        }

        configurationValues.put(RODDY_CVALUE_DIRECTORY_LOCKFILES, lockFilesDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_TEMP, temporaryDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION, executionDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION_COMMON, commonExecutionDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_RODDY_APPLICATION, roddyApplicationDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_BUNDLED_FILES, roddyBundledFilesDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_DIRECTORY_ANALYSIS_TOOLS, analysisToolsDirectory.getAbsolutePath(), CVALUE_TYPE_PATH);
        configurationValues.put(RODDY_CVALUE_JOBSTATE_LOGFILE, executionDirectory.getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + RODDY_JOBSTATE_LOGFILE, CVALUE_TYPE_STRING);

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

        //Current version info strings.
        String versionInfo = "Roddy version: " + Roddy.getUsedRoddyVersion() + "\nLibrary info:\n" + LibrariesFactory.getInstance().getLoadedLibrariesInfoList().join("\n");
        provider.writeTextFile(context.getProject().getRuntimeService().getNameOfRuntimeFile(context), versionInfo, context);

        //Current config
        String configText = ConfigurationConverter.convertAutomatically(context, cfg);
        provider.writeTextFile(context.getProject().getRuntimeService().getNameOfConfigurationFile(context), configText, context);

        //The application ini
        provider.copyFile(Roddy.getPropertiesFilePath(), new File(executionDirectory, "applicationProperties.ini"), context);
        provider.writeTextFile(new File(executionDirectory, "roddyCall.sh"), Roddy.getApplicationDirectory().getAbsolutePath() + "/roddy.sh" + Roddy.getCommandLineCall().getArguments().join(StringConstants.WHITESPACE), context);

        //Current configs xml files (default, user, pipeline config file)
        String configXML = new XMLConverter().convert(context, cfg);
        provider.writeTextFile(context.getProject().getRuntimeService().getNameOfXMLConfigurationFile(context), configXML, context);
        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_RUN);
    }

    /**
     * Copy and link analysis tools folders to the target analysis tools directory. Analysis tools folder names must be unique over all plugins.
     * However this is not enforced.
     * @param context
     */
    private void copyAnalysisToolsForContext(ExecutionContext context) {
        FileSystemAccessProvider provider = FileSystemAccessProvider.getInstance();
        Configuration cfg = context.getConfiguration();
        File dstExecutionDirectory = context.getExecutionDirectory();
        File dstAnalysisToolsDirectory = context.getAnalysisToolsDirectory();
        File dstCommonExecutionDirectory = context.getCommonExecutionDirectory();
        boolean useCentralAnalysisArchive = cfg.getUseCentralAnalysisArchive();

        //Current analysisTools directory (they are also used for execution)
        Map<File, PluginInfo> sourcePaths = [:];
        for (PluginInfo pluginInfo : LibrariesFactory.getInstance().getLoadedPlugins()) {
            pluginInfo.getToolsDirectories().values().each { sourcePaths[it] = pluginInfo }
        }

        provider.checkDirectory(dstExecutionDirectory, context, true);
        List<String> usedToolFolders = ((AnalysisConfiguration) context.getAnalysis().getConfiguration()).getUsedToolFolders();
        if (useCentralAnalysisArchive) {
            String[] existingArchives = provider.loadTextFile(context.getFileForAnalysisToolsArchiveOverview());
            Roddy.getCompressedAnalysisToolsDirectory().mkdir();

            Map<File, PluginInfo> listOfFolders = sourcePaths.findAll { File it, PluginInfo pInfo -> !it.getName().contains(".svn"); }

            //Add used base paths to configuration.
            listOfFolders.each {
                File folder, PluginInfo pInfo ->
                    def bPathID = folder.getName()
                    String basepathConfigurationID = ConfigurationConverter.createVariableName(ConfigurationConstants.CVALUE_PREFIX_BASEPATH, bPathID);
                    cfg.getConfigurationValues().add(new ConfigurationValue(basepathConfigurationID, RoddyIOHelperMethods.assembleLocalPath(dstExecutionDirectory, "analysisTools", bPathID).getAbsolutePath(), "string"));
            }

            // Compress existing tool folders to a central location and generate some md5 sums for them.
            long startParallelCompression = System.nanoTime()
            listOfFolders.keySet().parallelStream().each {
                File subFolder ->
                    long startSingleCompression = System.nanoTime()
                    PluginInfo pInfo = listOfFolders[subFolder]
                    String md5sum = RoddyIOHelperMethods.getSingleMD5OfFilesInDirectory(subFolder);
                    String zipFilename = "cTools_${pInfo.getName()}:${pInfo.getProdVersion()}_${subFolder.getName()}.zip";
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
                    logger.postSometimesInfo("Compression of ${zipFilename} took ${(System.nanoTime() - startSingleCompression) / 1000000} ms.")
            };
            logger.postSometimesInfo("Overall tool compression took ${(System.nanoTime() - startParallelCompression) / 1000000} ms.");

            // Now check if the local file with its md5 sum exists on the remote site.
            listOfFolders.each {
                File subFolder, PluginInfo pInfo ->
                    File localFile = mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive;
                    File remoteFile = new File(mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive.getName()[0..-5] + "_" + context.getTimeStampString() + ".zip");
                    String archiveMD5 = mapOfPreviouslyCompressedArchivesByFolder[subFolder].md5

                    String foundExisting = null;
                    String subFolderOnRemote = subFolder.getName();
                    for (String line : existingArchives) {
                        String[] split = line.split(StringConstants.SPLIT_COLON);
                        String existingFilePath = split[0];
                        String existingFileMD5 = split[1];
                        if (split.length == 2) {
                            existingFilePath = split[0];
                            existingFileMD5 = split[1];
                        } else if (split.length == 3) {                   // Newer Roddy version create directories containing version strings (separated by ":")
                            existingFilePath = split[0] + ":" + split[1];
                            existingFileMD5 = split[2];
                        } else {
                            continue;
                        }

                        if (existingFileMD5.equals(archiveMD5)) {
                            foundExisting = existingFilePath;
                            remoteFile = new File(remoteFile.getParentFile(), existingFilePath);
                            subFolderOnRemote = remoteFile.getName().split(StringConstants.SPLIT_UNDERSCORE)[-3];
                            //TODO This is seriously a hack.
                            break;
                        }
                    }

                    File analysisToolsServerDir;

                    // Check, if there is a zip file available and if the zip file is uncompressed.
                    if (foundExisting) {
                        analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + foundExisting);
                        File remoteZipFile = new File(dstCommonExecutionDirectory, remoteFile.getName());
                        if (!provider.directoryExists(analysisToolsServerDir)) {
                            //Now we may assume, that the file was not uncompressed!
                            //Check if the zip file exists. If so, uncompress it.
                            if (provider.fileExists(remoteZipFile)) {
                                // Unzip the file again. foundExisting stays true
                                GString str = RoddyIOHelperMethods.getCompressor().getDecompressionString(remoteZipFile, analysisToolsServerDir, analysisToolsServerDir);
                                getInstance().execute(str, true);
                                provider.setDefaultAccessRightsRecursively(new File(analysisToolsServerDir.getAbsolutePath()), context);
                            } else {
                                // Uh Oh, the file is not existing, the directory is not existing! Copy again and unzip
                                foundExisting = false;
                            }
                        }
                    }

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
                        if (!provider.directoryExists(analysisToolsServerDir))
                            context.addErrorEntry(ExecutionContextError.EXECUTION_PATH_NOTFOUND.expand("The central archive ${analysisToolsServerDir.absolutePath} was not created!"))

                    }
                    provider.checkDirectory(dstAnalysisToolsDirectory, context, true);
                    def linkCommand = "ln -s ${analysisToolsServerDir.getAbsolutePath()}/${subFolderOnRemote} ${dstAnalysisToolsDirectory.absolutePath}/${subFolder.getName()}"
                    getInstance().execute(linkCommand, true);
            }


        } else {
            sourcePaths.each {
                File sourcePath, PluginInfo pInfo ->
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

        final FileSystemAccessProvider provider = FileSystemAccessProvider.getInstance();
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

    public boolean writeTextFile(File file, String text) {}

    public boolean writeBinaryFile(File file, Serializable serializable) {}

    public boolean copyFile(File _in, File _out) {}

    public boolean copyDirectory(File _in, File _out) {}

    public boolean moveFile(File _from, File _to) {}

    public boolean modifyAccessRights(File file, String rightsStr, String groupID) {}

    public boolean createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {}

    public boolean removeDirectory(File directory) {}

    public boolean removeFile(File file) {}

    public boolean appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {}

    public boolean appendLineToFile(boolean atomic, File file, String line, boolean blocking) {}

    public Object loadBinaryFile(File file) {}

    public String[] loadTextFile(File file) {}

    public List<File> listFiles(File file, List<String> filters) {}

    public List<File> listFiles(List<File> file, List<String> filters) {}

    public boolean fileExists(File f) { return false }

    public boolean directoryExists(File f) { return false }

    public boolean isFileReadable(File f) {}

    public boolean isFileWriteable(File f) {}

    public boolean isFileExecutable(File f) {}

    public FileAttributes queryFileAttributes(File file) {}

    public Map<File, Boolean> getReadabilityOfAllFiles(List<File> listOfFiles) {
        List<File> allPaths = [];
        for (File file in listOfFiles) {
            if (file == null) {
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
