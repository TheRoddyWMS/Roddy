/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io

import de.dkfz.roddy.*
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.config.loader.ConfigurationFactory
import de.dkfz.roddy.config.loader.ConfigurationLoaderException
import de.dkfz.roddy.core.*
import de.dkfz.roddy.execution.BEExecutionService
import de.dkfz.roddy.execution.UnexpectedExecutionResultException
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.cluster.lsf.LSFSubmissionCommand
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSSubmissionCommand
import de.dkfz.roddy.execution.jobs.cluster.sge.SGESubmissionCommand
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectCommand
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutionJobManager
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.shell.bash.Service as BashService
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

import java.text.ParseException
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.logging.Level

import static de.dkfz.roddy.StringConstants.TILDE
import static de.dkfz.roddy.config.ConfigurationConstants.*

/**
 * Execution services commands locally or remotely. Some specific commands (copy file or directory) are also available as those can (hopefully) be created by all service implementations.
 * i.e. local file handling is done by Java itself wheres remote handling with ssh is done via the ssh library
 *
 */
@CompileStatic
abstract class ExecutionService implements BEExecutionService {
    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionService.class.name)
    private static ExecutionService executionService

    public static final String RODDY_CVALUE_DIRECTORY_LOCKFILES = "DIR_LOCKFILES"
    public static final String RODDY_CVALUE_DIRECTORY_TEMP = "DIR_TEMP"
    public static final String RODDY_CVALUE_DIRECTORY_EXECUTION = "DIR_EXECUTION"
    public static final String RODDY_CVALUE_DIRECTORY_EXECUTION_COMMON = "DIR_EXECUTION_COMMON"
    public static final String RODDY_CVALUE_DIRECTORY_RODDY_APPLICATION = "DIR_RODDY"
    public static final String RODDY_CVALUE_DIRECTORY_BUNDLED_FILES = "DIR_BUNDLED_FILES"
    public static final String RODDY_CVALUE_DIRECTORY_ANALYSIS_TOOLS = "DIR_ANALYSIS_TOOLS"
    public static final String RODDY_CVALUE_JOBSTATE_LOGFILE = "jobStateLogFile"

    static void initializeService(Class executionServiceClass, RunMode runMode) {
        executionService = (ExecutionService) executionServiceClass.getConstructors()[0].newInstance()
        if (runMode == RunMode.CLI) {
            boolean isConnected = executionService.tryInitialize()
            if (!isConnected) {
                int passwordQueryCounts = 0
                //If password is not stored, ask once for the password only, increase queryCount.
                boolean storePassword = Boolean.parseBoolean(Roddy.applicationConfiguration.
                        getOrSetApplicationProperty(runMode,
                                Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD,
                                Boolean.FALSE.toString()))
                if (!storePassword) {
                    Roddy.applicationConfiguration.setApplicationProperty(runMode,
                            Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD,
                            Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD)
                    RoddyCLIClient.askForPassword()
                    isConnected = executionService.tryInitialize()
                    passwordQueryCounts++
                }
                for (; passwordQueryCounts < 3 && !isConnected; passwordQueryCounts++) {
                    RoddyCLIClient.performCommandLineSetup()
                    isConnected = executionService.tryInitialize()
                }
            }
        } else {
            executionService.initialize()
        }
    }

    static void initializeService(boolean fullSetup) {
        logger.postSometimesInfo("public static void initializeService(boolean fullSetup)")

        if (!fullSetup) {
            executionService = new NoNoExecutionService()
            return
        }

        ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader()

        RunMode runMode = Roddy.runMode
        String executionServiceClassID =
                Roddy.applicationConfiguration.
                        getOrSetApplicationProperty(
                                runMode,
                                Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS,
                                SSHExecutionService.class.name)
        try {
            Class executionServiceClass = classLoader.loadClass(executionServiceClassID)
            initializeService(executionServiceClass, runMode)
        } catch (ClassNotFoundException) {
            throw new ConfigurationLoaderException("Could not load JobManager class ${executionServiceClassID}")
        }
    }

    /** The constructor is not used except in the initialize method above. This is a singleton and the proper way to
     *  initialize it is to call the initialize method, not the constructor directly.
     */
    protected ExecutionService() {
    }

    static ExecutionService getInstance() {
        return executionService
    }

    void addSpecificSettingsToConfiguration(Configuration configuration) {

    }

    boolean isLocalService() {
        return true
    }

    boolean initialize() {
    }

    boolean tryInitialize() {
        try {
            long t1 = System.nanoTime()
            initialize()
            logger.postSometimesInfo(RoddyIOHelperMethods.printTimingInfo("initialize exec service", t1, System.nanoTime()))
            return true
        } catch (Exception ex) {
            return false
        }
    }

    void destroy() {}

    protected abstract ExecutionResult _execute(String command,
                                                boolean waitFor = true,
                                                Duration timeout = Duration.ZERO,
                                                OutputStream outputStream = null)

    /**
     * Directly pass parameters to a tool script to be executed remotely. Use this method if you have a script in your plugin
     * whose execution is required during the workflow set-up. E.g. if you need the number of read-groups in the input BAM
     * during submission time.
     *
     * The tool is executed just like a normal tool but using the DirectSynchronousExecutionJobManager (so no PBS, GE, or LSF).
     *
     * Note that this tool may be executed both in the initial phase and the final phase during a testrerun. If you want to avoid
     * double execution, use some sort of caching in the calling code.
     *
     * @param context
     * @param toolID
     * @param parameters
     * @param jobNameExtension
     * @return
     */
    List<String> runDirect(@NotNull ExecutionContext context,
                           @NotNull String toolID,
                           Map<String, Object> parameters = null) {

        if (!parameters) parameters = [:]
        parameters[DISABLE_DEBUG_OPTIONS_FOR_TOOLSCRIPT] = "true"
        parameters[CVALUE_PLACEHOLDER_RODDY_SCRATCH_RAW] = context.temporaryDirectory

        Job wrapperJob = new Job(
                context,
                context.timestampString + "_directTool:" + toolID,
                context.getToolCommand(toolID),
                parameters)
        DirectSynchronousExecutionJobManager jobManager =
                new DirectSynchronousExecutionJobManager(instance, JobManagerOptions.create().build())
        wrapperJob.setJobManager(jobManager)
        BEJobResult result = wrapperJob.run(false)

        // Getting complicated from here on. We need to replicate some code.

        // If QUERY_STATUS is set as the context status, some parts of the above .run() will not be executed and need to
        // be mimicked (for now).
        if (!context.executionContextLevel.allowedToSubmitJobs) {
            wrapperJob.storeJobConfigurationFile(wrapperJob.createJobConfiguration())
            wrapperJob.keepOnlyEssentialParameters()
            // wrapperJob.wasSubmittedOnHold is already set in wrapperJob.run()
            result = jobManager.submitJob(wrapperJob)
        }

        File jobLog = new File(wrapperJob.jobLog.getOut(wrapperJob.jobID.toString()))
        if (!FileSystemAccessProvider.instance.fileExists(jobLog))
            throw new IOException("Job log file ${jobLog} does not exist")

        Long jobLogSize = FileSystemAccessProvider.instance.fileSize(jobLog)
        Long maximumJobLogSize = context.configurationValues.
                get("maximumJobLogSize", (1024 * 1024 * 10).toString()).toLong()
        if (jobLogSize > maximumJobLogSize)
            throw new IOException("Job log file of ${jobLogSize} byte is larger than permitted size ${maximumJobLogSize}")


        def lines = FileSystemAccessProvider.instance.loadTextFile(jobLog)

        if (lines != null) {

            if (!result.successful)
                throw new ExecutionException("Execution failed. ${result.executionResult.toStatusMessage()}", null)

            // Depending on which debug options are set, it is possible (set -v), that "Wrapped script ended"
            // is found before "Starting wrapped script". It is however safe to compare the whole "Starting wrapped script"
            // string to find the beginning of the output block of the executed wrapped script. Therefore we
            // use this to cut away misleading trailing output and then find the end block afterwards.
            // The end block can contain a "+ " at the beginning (again depending on the debug options), so we use
            // the find method to identify the first upcoming line.
            int index = lines.findIndexOf {
                it == "######################################################### Starting wrapped script ###########################################################"
            }
            if (index < 0)
                new ParseException("Could not find '#+ Starting wrapped script #+' in job log '${jobLog}' during synchronous script execution", -1)
            lines = lines[1 + index..-1]
            index = lines.findIndexOf { String it ->
                it.contains("######################################################### Wrapped script ended ##############################################################")
            }
            if (index < 0)
                new ParseException("Could not find '#+ Wrapped script ended #+' in job log '${jobLog}' during synchronous script execution", -1)
            lines = lines[0..index - 1]

            return lines as List<String>
        } else {
            return []
        }

    }

    List<String> executeTool(ExecutionContext context, String toolID, String jobNameExtension = "_executedScript:") {
        File path = context.configuration.getProcessingToolPath(context, toolID)
        String cmd = FileSystemAccessProvider.instance.commandSet.getExecuteScriptCommand(path)
        return execute(cmd, true).stdout
    }

    ExecutionResult execute(String command,
                            boolean waitFor = true,
                            Duration timeout = Duration.ZERO,
                            OutputStream outputStream = null) {
        if (command) {
            return _execute(command, waitFor, timeout, outputStream)
        } else {
            return new ExecutionResult([command], false, -1,
                    [], Arrays.asList("Command not valid. String is empty."),
                    "")
        }
    }

    /** Create somehow valid submission IDs.
     *
     * @param command
     * @return
     */
    String validSubmissionCommandOutputWithRandomJobId(Command command) {
        if (command instanceof LSFSubmissionCommand) {
            return String.format("<0%04d>", System.nanoTime() % 10000)
        } else if (command instanceof PBSSubmissionCommand) {
            return String.format("0x%04X", System.nanoTime() % 10000)
        } else if (command instanceof SGESubmissionCommand) {
            return String.format("dummy 0%04d", System.nanoTime() % 10000)
        } else if (command instanceof DirectCommand) {
            return String.format("dummy 0%04d", System.nanoTime() % 10000)
        } else {
            throw new RuntimeException("Don't know how to create a valid dummy output for a '${command.getClass()}'")
        }
    }

    @Override
    ExecutionResult execute(Command command, Duration timeout) {
        execute(command, true, timeout)
    }

    @Deprecated
    @Override
    ExecutionResult execute(String command, Duration timeout) {
        execute(command, true, timeout)
    }

    /** TODO: Remove the catch-all exception. Client code in Alignment workflow
     *        COProjectRuntimeService may depend on this, therefore this cannot be changed now.
     *        It's the interest of the client code to decide whether an exception should be ignored
     *        or not.
     */
    @Override
    ExecutionResult execute(Command command,
                            boolean waitFor = true,
                            Duration timeout = Duration.ZERO) {
        ExecutionContext context = ((Job) command.job).executionContext
        ExecutionResult res

        String cmdString
        try {
            cmdString = command.toBashCommandString()

            OutputStream outputStream = createServiceBasedOutputStream(command, waitFor)

            if (context.executionContextLevel == ExecutionContextLevel.TESTRERUN) {
                String pid = validSubmissionCommandOutputWithRandomJobId(command)
                res = new ExecutionResult([cmdString], true,
                        0, [pid], [], pid)
            } else {
                res = execute(cmdString, waitFor, timeout, outputStream)
            }
            command.getJob().setJobState(!res.successful ? JobState.FAILED : JobState.COMPLETED_SUCCESSFUL)

            if (outputStream)
                finalizeServiceBasedOutputStream(command, outputStream)

            context.addCalledCommand(command)
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.toString())
        }
        return res
    }

    protected FileOutputStream createServiceBasedOutputStream(Command command, boolean waitFor) { return null }

    protected void finalizeServiceBasedOutputStream(Command command, OutputStream outputStream) {}

    static long measureStart() { return System.nanoTime() }

    static double measureStop(long startValue, String measurementID, int verbosityLevel = LoggerWrapper.VERBOSITY_RARE) {
        double t = (System.nanoTime() - startValue) / 1000000.0
        if (measurementID != null)
            logger.log(verbosityLevel, "Execution of ${measurementID} took ${t} ms.")
        return t
    }

    /**
     * Check, if access rights setting is enabled and also, if the user and the group exist.
     *
     * @param context
     * @return
     */
    boolean checkAccessRightsSettings(ExecutionContext context) {
        boolean valid = true
        if (context.isAccessRightsModificationAllowed()) {
//            context.getExecutingUser()

            def userGroup = context.getOutputGroupString()
            boolean isGroupAvailable = FileSystemAccessProvider.instance.isGroupAvailable(userGroup)
            if (!isGroupAvailable) {
                context.addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("The requested user group ${userGroup} is not available on the target system.\n\t\tDisable Roddys access rights management by setting ${ConfigurationConstants.CFG_ALLOW_ACCESS_RIGHTS_MODIFICATION} to false)) or\n\t\tSelect a proper group by setting outputFileGroup."))
                valid = false
            }
        }
        return valid
    }

    /**
     * Check different directories and files to see, if files are accessible.
     * Will not check everything but at least some of the well-known sources of errors.
     * The method also check for some files, if they exist. If so, the read / write access will be checked.
     * If a file does not exist yet, it will not be checked!
     * @param context The context object which needs to be checked.
     * @return
     */
    boolean checkContextDirectoriesAndFiles(ExecutionContext context) {
        FileSystemAccessProvider fsap = FileSystemAccessProvider.instance
        Analysis analysis = context.analysis

        // First check in and output directories for accessibility

        // Project input directory with i.e. ../view-by-pid
        File inputBaseDirectory = analysis.getInputBaseDirectory()
        Boolean inputIsReadable = fsap.isReadable(inputBaseDirectory)
        if (!inputIsReadable)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.
                    expand("The input base directory is not readable: ${inputBaseDirectory}, please check access rights and ownership.", Level.SEVERE))

        // Project output base directory with i.e. ../results_per_pid
        File outputBaseDirectory = analysis.getOutputBaseDirectory()
        Boolean outputIsWriteable =
                fsap.directoryExists(outputBaseDirectory) ? fsap.isReadable(outputBaseDirectory) && fsap.isWritable(outputBaseDirectory) : null
        if (outputIsWriteable == null)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.
                    expand("Output base directory is missing: ${outputBaseDirectory}, please create with proper access rights and ownership.", Level.SEVERE))
        else if (outputIsWriteable == Boolean.FALSE) //Do an else if because groovy might evaluate null to false.
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("Output base directory is not writable: ${outputBaseDirectory}, please change access rights and ownership."))

        // Output with dataset id
        File outputDirectory = context.getOutputDirectory()
        Boolean datasetDirIsWritable =
                fsap.directoryExists(outputDirectory) ? fsap.isReadable(outputDirectory) && fsap.isWritable(outputDirectory) : null
        if (datasetDirIsWritable == null)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTFOUND_WARN.
                    expand("Creating output directory: ${outputDirectory}", Level.INFO))
        else if (datasetDirIsWritable == Boolean.FALSE) //Do an else if because groovy might evalute null to false.
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("Output directory is not writable: ${outputDirectory}"))

        // roddyExecutionStore in the dataset folder
        File baseContextExecutionDirectory = context.runtimeService.getBaseExecutionDirectory(context)
        Boolean baseContextDirIsWritable =
                fsap.directoryExists(baseContextExecutionDirectory) ? fsap.isReadable(baseContextExecutionDirectory) && fsap.isWritable(baseContextExecutionDirectory) : null
        if (baseContextDirIsWritable == Boolean.FALSE) //Do an else if because groovy might evaluate null to false.
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("The datasets execution storage folder is not writable: ${baseContextExecutionDirectory}"))

        // the exec_... folder in the base context exec dir. (NOT CHECKED, created later!)
        File contextExecutionDirectory = context.executionDirectory

        // .roddyExecutionStore in outputBaseDirectory
        File projectExecutionDirectory = context.commonExecutionDirectory
        Boolean projectExecutionContextDirIsWritable =
                fsap.directoryExists(projectExecutionDirectory) ? fsap.isReadable(projectExecutionDirectory) && fsap.directoryExists(projectExecutionDirectory) : null
        if (projectExecutionContextDirIsWritable == Boolean.FALSE)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("The project execution store is not writable: ${projectExecutionDirectory}"))

        // .roddyExecCache.txt containing the list of executed runs in the project output folder
        File projectExecCacheFile = context.runtimeService.getExecCacheFile(context.analysis)
        Boolean projectExecCacheFileIsWritable =
                outputIsWriteable && fsap.fileExists(projectExecCacheFile) ? fsap.isReadable(projectExecCacheFile) && fsap.isWritable(projectExecCacheFile) : null
        if (projectExecCacheFileIsWritable == Boolean.FALSE)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("The projects execution cache file is not writable: ${projectExecCacheFile}"))

        // The md5 sum file in .roddyExecutionStore
        File projectToolsMD5SumFile = context.getFileForAnalysisToolsArchiveOverview()
        Boolean projectToolsMD5SumFileIsWritable =
                fsap.fileExists(projectToolsMD5SumFile) ? fsap.isReadable(projectToolsMD5SumFile) && fsap.isWritable(projectToolsMD5SumFile) : projectExecutionContextDirIsWritable
        if (projectToolsMD5SumFileIsWritable == Boolean.FALSE)
            context.addError(ExecutionContextError.EXECUTION_PATH_NOTWRITABLE.
                    expand("The project md5sum file is not writable: ${projectToolsMD5SumFile}"))

        //Just check, if there were new errors.
        int countErrors = context.getErrors().sum(0) { ExecutionContextError ece -> ece.getErrorLevel() == Level.SEVERE ? 1 : 0 } as Integer
        return (context.getErrors().sum(0) { ExecutionContextError ece -> ece.getErrorLevel() == Level.SEVERE ? 1 : 0 } as Integer) - countErrors == 0
    }

    private class CompressedArchiveInfo {
        CompressedArchiveInfo(File localArchive, String md5, File folder) {
            this.localArchive = localArchive
            this.md5 = md5
            this.folder = folder
        }

        boolean validOnRemoteSite
        String md5
        File folder
        File localArchive
        File removeArchive
    }

    private long lastExecutionContextCheckpoint = -1100
    Map<File, CompressedArchiveInfo> mapOfPreviouslyCompressedArchivesByFolder = [:]

    /**
     * Writes files like the scripts in the analysisTools directory, the current configuration (shellscript and xml) and files with debug information to the runs logging directory.
     *
     * @param context
     */
    void writeFilesForExecution(ExecutionContext context) {
        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_INIT)

        FileSystemAccessProvider provider = FileSystemAccessProvider.instance
        ConfigurationFactory configurationFactory = ConfigurationFactory.instance

        String analysisID = context.analysis.name

        File execCacheFile = context.runtimeService.getExecCacheFile(context.analysis)
        String execCacheFileLock = new File(execCacheFile.absolutePath + TILDE).absolutePath
        File executionBaseDirectory = context.runtimeService.getBaseExecutionDirectory(context)
        File executionDirectory = context.executionDirectory
        File analysisToolsDirectory = context.analysisToolsDirectory
        File temporaryDirectory = context.temporaryDirectory
        File lockFilesDirectory = context.lockFilesDirectory
        File commonExecutionDirectory = context.commonExecutionDirectory
        File roddyApplicationDirectory = Roddy.applicationDirectory

        //Base path for the application. This path might not be available on the target system (i.e. because of ssh calls).
        File roddyBundledFilesDirectory = Roddy.getBundledFilesDirectory()

        provider.checkDirectories([executionBaseDirectory, executionDirectory, temporaryDirectory, lockFilesDirectory], context, true)
        if (context.executionContextLevel.allowedToSubmitJobs) {
            logger.always("Creating the following execution directory to store information about this process:")
            logger.always("\t${executionDirectory.absolutePath}")
        }

        Configuration cfg = context.configuration
        def configurationValues = cfg.configurationValues

        instance.addSpecificSettingsToConfiguration(cfg)

        //Add feature toggles to configuration
        FeatureToggles.values().each {
            FeatureToggles toggle ->
                configurationValues.put(toggle.name(), ((Boolean) Roddy.getFeatureToggleValue(toggle)).toString(), CVALUE_TYPE_BOOLEAN)
        }

        configurationValues.put(RODDY_CVALUE_DIRECTORY_LOCKFILES, lockFilesDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_TEMP, temporaryDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION, executionDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_EXECUTION_COMMON, commonExecutionDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_RODDY_APPLICATION, roddyApplicationDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_BUNDLED_FILES, roddyBundledFilesDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_DIRECTORY_ANALYSIS_TOOLS, analysisToolsDirectory.absolutePath, CVALUE_TYPE_PATH)
        configurationValues.put(RODDY_CVALUE_JOBSTATE_LOGFILE, 
                executionDirectory.absolutePath + FileSystemAccessProvider.instance.pathSeparator + RODDY_JOBSTATE_LOGFILE,
                CVALUE_TYPE_STRING)

        final boolean ATOMIC = true
        final boolean BLOCKING = true
        final boolean NON_BLOCKING = false

        provider.createFileWithDefaultAccessRights(ATOMIC, context.runtimeService.getJobStateLogFile(context), context, BLOCKING)
        provider.checkFile(execCacheFile, true, context)
        provider.appendLineToFile(ATOMIC, execCacheFile, String.format("%s,%s,%s", executionDirectory.absolutePath, analysisID, provider.callWhoAmI()), NON_BLOCKING)

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_COPY_TOOLS)
        copyAnalysisToolsForContext(context)

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_SETUP_COPY_CONFIG)

        //Current version info strings.
        String versionInfo = "Roddy version: " + Roddy.getUsedRoddyVersion() + "\nLibrary info:\n" + LibrariesFactory.instance.getLoadedLibrariesInfoList().join("\n") + "\n"
        provider.writeTextFile(context.runtimeService.getRuntimeFile(context), versionInfo, context)

        //Current config
        String configText = ConfigurationConverter.convertAutomatically(context, cfg)

        //The application ini
        provider.copyFile(Roddy.propertiesFilePath, new File(executionDirectory, Constants.APP_PROPERTIES_FILENAME), context)
        provider.writeTextFile(new File(executionDirectory, "roddyCall.sh"),
                Roddy.applicationDirectory.absolutePath + "/roddy.sh \\\n" +
                        Roddy.getCommandLineCall().arguments.
                                collect { "\t" + BashService.escape(it) }.
                                join(" \\\n") + "\n",
                context)

        context.detailedExecutionContextLevel = ExecutionContextSubLevel.RUN_RUN
    }

    /**
     * Checks all the files created in writeFilesForExecution
     * If strict mode is enabled, all missing files will lead to false.
     * If strict mode is disabled, only neccessary files are checked.
     * @return A descriptive list of missing files
     */
    List<String> checkForInaccessiblePreparedFiles(ExecutionContext context) {
        if (!context.executionContextLevel.allowedToSubmitJobs) return []
        boolean strict = Roddy.strictModeEnabled
        def runtimeService = context.runtimeService

        List<String> inaccessibleNecessaryFiles = []
        List<String> inaccessibleIgnorableFiles = []

        // Use the context check methods, so we automatically get an error message
        [
                runtimeService.getBaseExecutionDirectory(context),
                context.executionDirectory,
                context.analysisToolsDirectory,
                context.temporaryDirectory,
                context.lockFilesDirectory,
                context.commonExecutionDirectory,
        ].each {
            if (!context.directoryIsAccessible(it)) inaccessibleNecessaryFiles << it.absolutePath
        }

        if (!context.fileIsAccessible(runtimeService.getJobStateLogFile(context))) inaccessibleNecessaryFiles << "JobState logfile"

        // Check the ignorable files. It is still nice to see whether they are there
        if (!context.fileIsAccessible(runtimeService.getExecCacheFile(context.analysis))) inaccessibleIgnorableFiles << "Execution cache file"
        if (!context.fileIsAccessible(runtimeService.getRuntimeFile(context))) inaccessibleIgnorableFiles << "Runtime information file"
        if (!context.fileIsAccessible(new File(context.executionDirectory, Constants.APP_PROPERTIES_FILENAME))) inaccessibleIgnorableFiles << "Copy of application.ini file"

        // Return true, if the necessary files are there and if strict mode is enabled and in this case all ignorable files exist
        return inaccessibleNecessaryFiles + (strict ? [] as List<String> : inaccessibleIgnorableFiles)
    }

    /**
     * Which tools should be checked?
     * My best guess is to check all the defined tools.
     * @return
     */
    boolean checkCopiedAnalysisTools(ExecutionContext context) {
        boolean checked = true
        for (ToolEntry tool in context.configuration.tools.allValuesAsList) {
            File toolPath = context.configuration.getProcessingToolPath(context, tool.id)
            checked &= context.fileIsExecutable(toolPath)
        }
        return checked
    }

    void markConfiguredToolsAsExecutable(ExecutionContext context) {
        logger.postSometimesInfo("BEExecutionService.markConfiguredToolsAsExecutable is not implemented yet! Only checks for executability are available.")
//        context.configuration.tools.each {
//            ToolEntry tool ->
//                File toolPath = context.configuration.getProcessingToolPath(context, tool.id)
//
//                def instance = FileSystemAccessProvider.instance
//
//                instance.setDefaultAccessRights()
//
//        }
    }

    /**
     * Copy and link analysis tools folders to the target analysis tools directory. Analysis tools folder names must be unique over all plugins.
     * However this is not enforced.
     * @param context
     */
    void copyAnalysisToolsForContext(ExecutionContext context) {
        FileSystemAccessProvider provider = FileSystemAccessProvider.instance
        Configuration cfg = context.configuration
        File dstExecutionDirectory = context.executionDirectory
        File dstAnalysisToolsDirectory = context.analysisToolsDirectory

        //Current analysisTools directory (they are also used for execution)
        Map<File, PluginInfo> sourcePaths = [:]
        for (PluginInfo pluginInfo : LibrariesFactory.instance.getLoadedPlugins()) {
            pluginInfo.toolsDirectories.values().each { sourcePaths[it] = pluginInfo }
        }

        provider.checkDirectory(dstExecutionDirectory, context, true)

        String[] existingArchives = provider.loadTextFile(context.getFileForAnalysisToolsArchiveOverview())
        if (existingArchives == null)
            existingArchives = new String[0]
        Roddy.compressedAnalysisToolsDirectory.mkdir()

        Map<File, PluginInfo> listOfFolders = sourcePaths.findAll { File it, PluginInfo pInfo -> !it.name.contains(".svn") }

        //Add used base paths to configuration.
        listOfFolders.each {
            File folder, PluginInfo pInfo ->
                def bPathID = folder.name
                String basepathConfigurationID = ConfigurationConverter.
                        createVariableName(ConfigurationConstants.CVALUE_PREFIX_BASEPATH, bPathID)
                cfg.configurationValues.add(new ConfigurationValue(basepathConfigurationID, 
                        RoddyIOHelperMethods.assembleLocalPath(dstExecutionDirectory, 
                                RuntimeService.DIRNAME_ANALYSIS_TOOLS, bPathID).absolutePath,
                        ConfigurationConstants.CVALUE_TYPE_STRING))
        }

        Map<String, List<Map<String, String>>> mapOfInlineScripts = [:]

        for (ToolEntry tool in cfg.tools.allValuesAsList) {
            if (tool.hasInlineScript()) {
                mapOfInlineScripts.get(tool.basePathId, []) << ["inlineScript": tool.inlineScript, "inlineScriptName": tool.inlineScriptName]
            }
        }

        long startParallelCompression = System.nanoTime()

        // Check and override the listOfFolders, eventually create new temporary folders, if inline scripts are used
        listOfFolders = writeInlineScriptsAndCorrectListOfFolders(listOfFolders, mapOfInlineScripts)

        // Compress the new (or old) folder list.
        compressToolFolders(listOfFolders, mapOfInlineScripts)
        logger.postRareInfo("Overall tool compression took ${(System.nanoTime() - startParallelCompression) / 1000000} ms.")

        // Now check if the local file with its md5 sum exists on the remote site.
        moveCompressedToolFilesToRemoteLocation(listOfFolders, existingArchives, provider, context)

        markConfiguredToolsAsExecutable(context)
    }

    /**
     * Add inline scripts and compress existing tool folders to a central location and generate some md5 sums for them.
     *
     * 1. Create a new temp folder
     * 2.a If there are inline scripts in the folder:
     *  3. copy the script subfolder to the newly created temp folder AND
     *  4. Copy all inline scripts to files.
     *  5. Create a corrected entry in the new map
     * 2.b If not, put the original folder entry to the map
     *
     * @param listOfFolders
     * @param mapOfInlineScriptsBySubfolder - Map<SubfolderName,ScriptName>
     */
    Map<File, PluginInfo> writeInlineScriptsAndCorrectListOfFolders(Map<File, PluginInfo> listOfFolders, Map<String, List<Map<String, String>>> mapOfInlineScriptsBySubfolder) {

        Map<File, PluginInfo> correctedListOfFolders = [:]

        listOfFolders.keySet().parallelStream().each {
            File subFolder ->
                if (!subFolder.directory) return
                PluginInfo pInfo = listOfFolders[subFolder]

                if (!mapOfInlineScriptsBySubfolder.containsKey(subFolder.name)) {
                    correctedListOfFolders[subFolder] = pInfo
                    return
                }

                // Create the temp folder
                File tempFolder = File.createTempDir()
                tempFolder.deleteOnExit()
                tempFolder = RoddyIOHelperMethods.assembleLocalPath(tempFolder, subFolder.name)

                // Copy the original scripts to the new folder
                RoddyIOHelperMethods.copyDirectory(subFolder, tempFolder)
                logger.postSometimesInfo("Folder ${subFolder.name} copied to ${tempFolder.absolutePath}")

                // Create inline script files in new folder
                mapOfInlineScriptsBySubfolder[subFolder.name].each {
                    scriptEntry ->
                        new File(tempFolder, scriptEntry["inlineScriptName"]) << scriptEntry["inlineScript"]
                }
                correctedListOfFolders[tempFolder] = pInfo
        }
        return correctedListOfFolders
    }

    /**
     * Compress all folders in the given list of folders.
     * Also create some sort of (md5 based) checksum for this.
     * @param listOfFolders
     * @param mapOfInlineScriptsBySubfolder
     */
    void compressToolFolders(Map<File, PluginInfo> listOfFolders, Map<String, List<Map<String, String>>> mapOfInlineScriptsBySubfolder) {
        listOfFolders.keySet().parallelStream().each {
            File subFolder ->
                long startSingleCompression = System.nanoTime()

                PluginInfo pInfo = listOfFolders[subFolder]
                // Md5sum from tempFolder
                String md5sum = RoddyIOHelperMethods.getSingleMD5OfFilesInDirectoryIncludingDirectoryNamesAndPermissions(subFolder)
                String zipFilename = "cTools_${pInfo.name}:${pInfo.getProdVersion()}_${subFolder.name}.zip"
                String zipMD5Filename = zipFilename + "_contentmd5"
                File tempFile = new File(Roddy.compressedAnalysisToolsDirectory, zipFilename)
                File zipMD5File = new File(Roddy.compressedAnalysisToolsDirectory, zipMD5Filename)
                boolean createNew = false
                if (!tempFile.exists())
                    createNew = true

                if (!zipMD5File.exists() || zipMD5File.text.trim() != md5sum)
                    createNew = true

                if (createNew) {
                    RoddyIOHelperMethods.compressDirectory(subFolder, tempFile)
                    // See issue #286
                    zipMD5File.text = md5sum
                }

                String newArchiveMD5 = md5sum
                if (tempFile.size() == 0)
                    logger.severe("The size of archive ${tempFile.name} is 0!")
                synchronized (mapOfPreviouslyCompressedArchivesByFolder) {
                    mapOfPreviouslyCompressedArchivesByFolder[subFolder] = new CompressedArchiveInfo(tempFile, newArchiveMD5, subFolder)
                }
                logger.postSometimesInfo("Compression of ${zipFilename} took ${(System.nanoTime() - startSingleCompression) / 1000000} ms.")
        }
    }

    /**
     * Check if the local file with its md5 sum exists on the remote site otherwise move those files from local to remote site.
     * @param listOfFolders
     * @param existingArchives
     * @param provider
     * @param context
     */
    void moveCompressedToolFilesToRemoteLocation(Map<File, PluginInfo> listOfFolders, String[] existingArchives, FileSystemAccessProvider provider, ExecutionContext context) {
        File dstCommonExecutionDirectory = context.commonExecutionDirectory
        File dstAnalysisToolsDirectory = context.analysisToolsDirectory

        listOfFolders.each {
            File subFolder, PluginInfo pInfo ->
                if (!subFolder.directory)
                    return
                File localFile = mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive
                File remoteFile = new File(mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive.name[0..-5] + "_" + context.getTimestampString() + ".zip")
                String archiveMD5 = mapOfPreviouslyCompressedArchivesByFolder[subFolder].md5

                String foundExisting = null
                String subFolderOnRemote = subFolder.name
                for (String line : existingArchives) {
                    String[] split = line.split(StringConstants.SPLIT_COLON)
                    String existingFilePath = split[0]
                    String existingFileMD5 = split[1]
                    if (split.length == 2) {
                        existingFilePath = split[0]
                        existingFileMD5 = split[1]
                    } else if (split.length == 3) {                   // Newer Roddy version create directories containing version strings (separated by ":")
                        existingFilePath = split[0] + ":" + split[1]
                        existingFileMD5 = split[2]
                    } else {
                        continue
                    }

                    if (existingFileMD5.equals(archiveMD5)) {
                        foundExisting = existingFilePath
                        remoteFile = new File(remoteFile.getParentFile(), existingFilePath)
                        subFolderOnRemote = remoteFile.name.split(StringConstants.SPLIT_UNDERSCORE)[-3]
                        //TODO This is seriously a hack.
                        break
                    }
                }

                File analysisToolsServerDir

                // Check, if there is a zip file available and if the zip file is uncompressed.
                if (foundExisting) {
                    analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + foundExisting)
                    File remoteZipFile = new File(dstCommonExecutionDirectory, remoteFile.name)
                    if (!provider.directoryExists(analysisToolsServerDir)) {
                        // Now we may assume, that the file was not uncompressed!
                        // Check if the zip file exists. If so, uncompress it.
                        if (provider.fileExists(remoteZipFile)) {
                            // Unzip the file again. foundExisting stays true
                            GString command = RoddyIOHelperMethods.compressor.getDecompressionString(
                                    remoteZipFile, analysisToolsServerDir, analysisToolsServerDir)
                            ExecutionResult result =
                                    instance.execute(command, true)
                            if (!result.successful)
                                throw new UnexpectedExecutionResultException(
                                        "Could not execute command: ${result.toStatusMessage()}" as String,
                                        result.stdout)
                            boolean success = provider.
                                    setDefaultAccessRightsRecursively(new File(analysisToolsServerDir.absolutePath), context)
                            if (!success)
                                logger.warning("Ignoring error while setting default access rights on '${analysisToolsServerDir.absolutePath} (existing archive)'")
                        } else {
                            // Uh Oh, the file is not existing, the directory is not existing! Copy again and unzip
                            foundExisting = false
                        }
                    }
                }

                if (foundExisting) {
                    analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + foundExisting)
                    logger.postSometimesInfo("Skipping copy of file ${remoteFile.name}, a file with the same md5 was found.")
                } else {

                    analysisToolsServerDir = new File(dstCommonExecutionDirectory, "/dir_" + remoteFile.name)
                    provider.checkDirectory(dstCommonExecutionDirectory, context, true)
                    provider.checkDirectory(analysisToolsServerDir, context, true)
                    provider.copyFile(localFile, new File(dstCommonExecutionDirectory, remoteFile.name), context)
                    provider.checkFile(context.getFileForAnalysisToolsArchiveOverview(), true, context)
                    provider.appendLineToFile(true, context.getFileForAnalysisToolsArchiveOverview(), "${remoteFile.name}:${archiveMD5}", true)

                    GString command = RoddyIOHelperMethods.compressor.getDecompressionString(
                            new File(dstCommonExecutionDirectory, remoteFile.name),
                            analysisToolsServerDir, analysisToolsServerDir)
                    ExecutionResult result =
                            instance.execute(command, true)
                    if (!result.successful)
                        throw new UnexpectedExecutionResultException(
                                "Could not execute command: ${result.toStatusMessage()}" as String,
                                result.stdout)
                    boolean success = provider.
                            setDefaultAccessRightsRecursively(new File(analysisToolsServerDir.absolutePath), context)
                    if (!success)
                        logger.warning("Ignoring error while setting default access rights on '${analysisToolsServerDir.absolutePath} (no existing archive)'")
                    if (!provider.directoryExists(analysisToolsServerDir))
                        context.addError(ExecutionContextError.EXECUTION_PATH_NOTFOUND.
                                expand("The central archive ${analysisToolsServerDir.absolutePath} was not created!"))

                }
                provider.checkDirectory(dstAnalysisToolsDirectory, context, true)
                def linkCommand = "ln -s ${analysisToolsServerDir.absolutePath}/${subFolderOnRemote} ${dstAnalysisToolsDirectory.absolutePath}/${subFolder.name}"
                instance.execute(linkCommand, true)
        }
    }

    /**
     * Writes things like a command call file
     *
     * @param context
     */
    void writeAdditionalFilesAfterExecution(ExecutionContext context) {
        if (!context.executionContextLevel.allowedToSubmitJobs) return

        context.setDetailedExecutionContextLevel(ExecutionContextSubLevel.RUN_FINALIZE_CREATE_JOBFILES)

        final FileSystemAccessProvider provider = FileSystemAccessProvider.instance
        final String separator = Constants.ENV_LINESEPARATOR

        List<Command> commandCalls = context.getCommandCalls() ?: ([] as List<Command>)
        StringBuilder realCalls = new StringBuilder()
        List<BEJobID> jobIDs = new LinkedList<>()
        int cnt = 0
        Map<String, String> jobIDReplacement = new HashMap<String, String>()
        StringBuilder repeatCalls = new StringBuilder()

        for (Command c : commandCalls) {
            BEJobID eID = c.getJobID()
            if (eID != null) {
                jobIDs.add(eID)
                if (eID.getShortID() != null) {
                    jobIDReplacement.put(eID.getShortID(), String.format('$j_' + "%08d", cnt++))
                }
            }
        }
        for (Command c : commandCalls) {
            BEJobID eID = c.getJobID()
            String cmdStr = c.toBashCommandString()
            realCalls.append(eID).append(", ").append(cmdStr).append(separator)

            String repeatCallLine = String.format("%s=`%s`" + separator, eID, cmdStr)
            for (String k : jobIDReplacement.keySet()) {
                String v = jobIDReplacement.get(k)

                repeatCallLine = repeatCallLine.replace(k, v)
            }
            repeatCalls.append(repeatCallLine.substring(1))
        }

        context.detailedExecutionContextLevel =
                ExecutionContextSubLevel.RUN_FINALIZE_CREATE_BINARYFILES

        RuntimeService rService = context.runtimeService
        final File realCallFile = rService.getRealCallsFile(context)
        final File repeatCallFile = rService.getRepeatableJobCallsFile(context)
        provider.writeTextFile(realCallFile, realCalls.toString(), context)
        provider.writeTextFile(repeatCallFile, repeatCalls.toString(), context)
        rService.writeJobInfoFile(context)
    }

    boolean doesKnowTheUsername() { return false }

    String getUsername() {}

    boolean canCopyFiles() { return false }

    boolean canReadFiles() { return false }

    boolean canWriteFiles() { return false }

    boolean canDeleteFiles() { false }

    boolean canListFiles() { return false }

    boolean canModifyAccessRights() { return false }

    boolean canQueryFileAttributes() { return false }

    boolean writeTextFile(File file, String text) {}

    boolean writeBinaryFile(File file, Serializable serializable) {}

    boolean copyFile(File _in, File _out) {}

    boolean copyDirectory(File _in, File _out) {}

    boolean moveFile(File _from, File _to) {}

    boolean modifyAccessRights(File file, String rightsStr, String groupID) {}

    boolean createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {}

    boolean removeDirectory(File directory) {}

    boolean removeFile(File file) {}

    boolean appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {}

    boolean appendLineToFile(boolean atomic, File file, String line, boolean blocking) {}

    Object loadBinaryFile(File file) {}

    String[] loadTextFile(File file) {}

    List<File> listFiles(File file, List<String> filters) {}

    List<File> listFiles(List<File> file, List<String> filters) {}

    boolean fileExists(File f) { return false }

    boolean directoryExists(File f) { return false }

    boolean isFileReadable(File f) {}

    boolean isFileWriteable(File f) {}

    boolean isFileExecutable(File f) {}

    FileAttributes queryFileAttributes(File file) {}

    Map<File, Boolean> getReadabilityOfAllFiles(List<File> listOfFiles) {
        List<File> allPaths = []
        for (File file in listOfFiles) {
            if (file == null) {
                //TODO Print out an error message? Or just ignore this because the error is already noted somewhere else.
                continue
            }
            if (!allPaths.contains(file.parentFile))
                allPaths << file.parentFile
        }

        Map<File, Boolean> readability = [:]
        List<File> allFiles = listFiles(allPaths, null)
        for (File file in listOfFiles)
            readability[file] = allFiles.contains(file)

        return readability
    }

    boolean needsPassword() { return false }

    boolean testConnection() { return true }

    /**
     * For remote files
     * @param file
     * @return
     */
    File getTemporaryFileForFile(File file) { return null }

    abstract boolean isAvailable()
}
