/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.FeatureToggles
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.BindSpec
import de.dkfz.roddy.execution.Command
import de.dkfz.roddy.execution.Executable
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.Utils
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull
import de.dkfz.roddy.execution.jobs.Command as BECommand
import org.jetbrains.annotations.Nullable

import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Level

import static de.dkfz.roddy.Constants.UNKNOWN

/**
 * An ExecutionContext is the runtime context for an analysis and a DataSet.<br />
 * It keeps track of context relevant information like:<br />
 * <ul>
 * <li>Created files</li>
 * <li>Executed jobs and commands</li>
 * <li>BEJob states</li>
 * <li>Log files</li>
 * <li>Context-dependent access to configuration, and plain configuration access</li>
 * </ul>
 * It also contains information about context specific settings like:<br />
 * <ul>
 * <li>I/O directories</li>
 * <li>Execution and temporary directories</li>
 * </ul>
 * The context object allows you to create lockfile jobs and a
 * generic streambuffer job for interprocess communication.
 * <p>
 * The context also allows you to access logfiles, command, jobs etc via getter methods
 * <p>
 * The context is finally used to context the stored analysis for the stored dataset.
 * <br>
 *
 * @author michael
 */
@CompileStatic
class ExecutionContext {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionContext.class.name)

    /**
     * The project to which this context belongs.
     */
    protected final Project project
    /**
     * The analysis for which this context was created
     */
    protected final Analysis analysis
    /**
     * The workflow for this context
     */
    protected final Workflow workflow
    /**
     * The source data set on which we work.
     */
    protected final DataSet dataSet
    /**
     * Keeps a list of all files which were created with this process.
     */
    protected final List<BaseFile> allFilesInRun = new LinkedList<BaseFile>().asSynchronized()
    /**
     * Keeps a list of all (previously) started jobs which belong to this process.
     */
    protected final List<Job> jobsForProcess = new LinkedList<Job>().asSynchronized()
    /**
     * Stores a list of all calls which were passed to the job system within this context.
     */
    private final List<BECommand> commandCalls = new LinkedList<BECommand>().asSynchronized()
    /**
     * This is some sort of synchronization checkpoint marker.
     * Contexts which were started with the same
     */
    private final long creationCheckPoint
    /**
     * Keeps a list of errors which happen either on read back or on execution.
     * The list is not stored and rebuilt if necessary, so not all errors might be available.
     */
    private final List<ExecutionContextError> errors = ([] as List<ExecutionContextError>).asSynchronized()
    /**
     * Keeps a list of warnings which happen either on read back or on execution.
     * The list is not stored and rebuilt if necessary, so not all errors might be available.
     */
    private final List<ExecutionContextError> warnings = ([] as List<ExecutionContextError>).asSynchronized()
    /**
     * Keeps a list of info entries which happen either on read back or on execution.
     * The list is not stored and rebuilt if necessary, so not all errors might be available.
     */
    private final List<ExecutionContextError> infos = ([] as List<ExecutionContextError>).asSynchronized()

    /**
     * The timestamp of this context object
     */
    protected Date timestamp = new Date()
    /**
     * The input directory for the context's dataset
     */
    private File inputDirectory
    /**
     * The output directory for the context's dataset
     */
    private File outputDirectory
    /**
     * The path to the execution directory for this context.
     */
    private File executionDirectory = null
    /**
     * The path to the directory for process lock files.
     */
    private File lockFilesDirectory = null
    /**
     * The directory for storing temporary files.
     */
    private File temporaryDirectory = null
    /**
     * The path to process log files.
     */
    private File loggingDirectory = null
    /**
     * This directory contains common information for the current project.
     */
    private File commonExecutionDirectory
    /**
     * The file containing a list of all known copied analysis tools archives and their md5 sum.
     */
    private File md5OverviewFile
    /**
     * The path in which a copy of the analysis tools are put to.
     */
    private File analysisToolsDirectory
    private ExecutionContextLevel executionContextLevel
    private ExecutionContextSubLevel executionContextSubLevel = ExecutionContextSubLevel.RUN_UNINITIALIZED
    private ToolEntry currentExecutedTool
    private ProcessingFlag processingFlag = ProcessingFlag.STORE_EVERYTHING
    /**
     * The user who created the context (if known)
     */
    private String executingUser = Constants.UNKNOWN_USER

    ExecutionContext(String userID, Analysis analysis, DataSet dataSet, ExecutionContextLevel executionContextLevel,
                     File outputDirectory, File inputDirectory, File executionDirectory) {
        this(userID, analysis, dataSet, executionContextLevel, outputDirectory, inputDirectory, executionDirectory, -1)
    }

    ExecutionContext(String userID, Analysis analysis, DataSet dataSet, ExecutionContextLevel executionContextLevel,
                     File outputDirectory, File inputDirectory, File executionDirectory, long creationCheckPoint) {
        this.executionDirectory = executionDirectory
        this.outputDirectory = outputDirectory
        this.inputDirectory = inputDirectory
        this.creationCheckPoint = creationCheckPoint
        this.project = analysis?.getProject()
        this.analysis = analysis
        this.workflow = createContextWorkflowObject(analysis, this)
        this.executionContextLevel = executionContextLevel
        this.dataSet = dataSet

        setExecutingUser(userID)
    }

    /**
     * The constructor will be called if one attempts to read out a a stored execution context.
     */
    ExecutionContext(AnalysisProcessingInformation api, Date readOutTimestamp) {
        this.executionDirectory = api.getExecPath()
        this.dataSet = api.getDataSet()
        this.project = analysis?.getProject()
        this.analysis = api.getAnalysis()
        this.workflow = createContextWorkflowObject(analysis, this)
        this.executionContextLevel = ExecutionContextLevel.READOUT
        this.inputDirectory = dataSet.getInputFolderForAnalysis(analysis)
        this.outputDirectory = dataSet.getOutputFolderForAnalysis(analysis)
        this.setTimestamp(readOutTimestamp)
        creationCheckPoint = -1
    }

    /**
     * Cloning constructor to create a shallow object copy.
     */
    private ExecutionContext(ExecutionContext p) {
        this.project = p.project
        this.analysis = p.analysis
        this.workflow = p.workflow
        this.dataSet = p.dataSet
        this.timestamp = p.timestamp
        this.inputDirectory = p.inputDirectory
        this.outputDirectory = p.outputDirectory
        this.executionDirectory = p.executionDirectory
        this.lockFilesDirectory = p.lockFilesDirectory
        this.temporaryDirectory = p.temporaryDirectory
        this.loggingDirectory = p.loggingDirectory
        this.executionContextLevel = p.executionContextLevel
        this.executionContextSubLevel = p.executionContextSubLevel
        this.processingFlag = p.processingFlag
        this.executingUser = p.executingUser
        this.allFilesInRun.addAll(p.getAllFilesInRun())
        this.jobsForProcess.addAll(p.jobsForProcess)
        this.commandCalls.addAll(p.commandCalls)
        this.errors.addAll(p.errors)
        creationCheckPoint = -1
    }

    /**
     * Creates a shallow copy if this context.
     *
     * @return
     */
    ExecutionContext clone() {
        return new ExecutionContext(this)
    }

    /**
     * Creates a workflow without a context object attached.
     * @param analysis
     * @return
     */
    static Workflow createAnalysisWorkflowObject(Analysis analysis) {
        return createContextWorkflowObject(analysis, null)
    }

    /**
     * Create a workflow object for an analysis / context. In most cases context will not be null.
     * However, in analysis.cleanup and some rare other cases, a workflow object needs to be usable without a context.
     * @param analysis
     * @param context
     * @return
     */
    static Workflow createContextWorkflowObject(Analysis analysis, ExecutionContext context) {
        Class workflowClass = LibrariesFactory.instance.searchForClass(analysis.configuration.workflowClass)
        Workflow workflow
        if (workflowClass.name.endsWith('$py')) {
            // Jython creates a class called Workflow$py with a constructor with a single (unused) String parameter.
            workflow = (Workflow) workflowClass.getConstructor(String).newInstance("dummy")
        } else {
            workflow = (Workflow) workflowClass.getConstructor().newInstance()
        }
        if (context)
            workflow.setContext(context)
        return workflow
    }

    Workflow getWorkflow() {
        return workflow
    }

    Map<String, Object> getDefaultJobParameters(@NotNull String toolId) {
        return runtimeService.getDefaultJobParameters(this, toolId)
    }

    @NotNull String getToolMd5(@NotNull String toolId) {
        return configuration.getProcessingToolMD5(toolId)
    }

    @NotNull ResourceSet getResourceSetFromConfiguration(@Nullable String toolId) {
        if (!toolId || toolId.trim() == "") {
            // TODO However the semantics of values null, "", and UNKNOWN are. Use ToolId class and forbid null ToolId.
            logger.warning("ToolId was null or empty. Returning EmptyResourceSet. Please report this! " +
                           "Stacktrace:\n" + Utils.stackTrace())
            return new EmptyResourceSet()
        } else if (toolId == UNKNOWN) {
            return new EmptyResourceSet()
        } else {
            ToolEntry te = configuration.tools.getValue(toolId)
            return te.getResourceSet(configuration) ?: new EmptyResourceSet()
        }
    }

    String createJobName(BaseFile p, String toolId) {
        return createJobName(p, toolId, false)
    }

    @NotNull String createJobName(BaseFile p, String toolId, boolean reduceLevel) {
        return runtimeService.createJobName(this, p, toolId, reduceLevel)
    }

    File getInputDirectory() {
        return inputDirectory
    }

    File getOutputDirectory() {
        return outputDirectory
    }

    File getRoddyScratchBaseDir() {
        return new File(configurationValues.get(Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY).toString())
    }

    File getRoddyScratchDir() {
        return new File(configurationValues.get(ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_SCRATCH_RAW).toString())
    }

    RuntimeService getRuntimeService() {
        return analysis.runtimeService
    }

    ExecutionContextLevel getExecutionContextLevel() {
        return executionContextLevel
    }

    void setExecutionContextLevel(ExecutionContextLevel newLevel) {
        this.executionContextLevel = newLevel
    }

    ExecutionContextSubLevel getDetailedExecutionContextLevel() {
        return executionContextSubLevel
    }

    synchronized void setDetailedExecutionContextLevel(ExecutionContextSubLevel subLevel) {
        this.executionContextSubLevel = subLevel
    }

    void setCurrentExecutedTool(ToolEntry toolEntry) {
        this.currentExecutedTool = toolEntry
    }

    ToolEntry getCurrentExecutedTool() {
        return currentExecutedTool
    }

    ProcessingFlag getProcessingFlag() {
        return processingFlag
    }

    ProcessingFlag setProcessingFlag(ProcessingFlag processingFlag) {
        ProcessingFlag temp = this.processingFlag
        this.processingFlag = processingFlag
        return temp
    }

    /**
     * Sets the level to ABORTED if it was allowed to submit jobs.
     */
    void abortJobSubmission() {
        if (executionContextLevel.canSubmitJobs)
            executionContextLevel = ExecutionContextLevel.ABORTED
    }

    Project getProject() {
        return project
    }

    Analysis getAnalysis() {
        return analysis
    }

    DataSet getDataSet() {
        return dataSet
    }

    Configuration getConfiguration() {
        return analysis.configuration
    }

    Configuration createJobConfiguration() {
        return new Configuration(null, configuration)
    }

    boolean getFeatureToggleStatus(FeatureToggles toggle) {
        if (toggle.applicationLevelOnly) {
            logger.warning("The feature toggle ${toggle} is marked as application level only. The application level value will be used.")
        } else {
            def cvalues = configuration.configurationValues
            if (cvalues.hasValue(toggle.name()))
                return cvalues.get(toggle.name()).toBoolean()
        }
        return Roddy.getFeatureToggleValue(toggle)
    }

    RecursiveOverridableMapContainerForConfigurationValues getConfigurationValues() {
        return configuration.configurationValues
    }

    void setTimestamp(Date timestamp) {
        this.timestamp = timestamp
    }

    Date getTimestamp() {
        return timestamp
    }

    String getTimestampString() {
        return InfoObject.formatTimestamp(timestamp)
    }

    long getCreationCheckPoint() {
        return creationCheckPoint
    }

    String getExecutingUser() {
        return this.executingUser
    }

    void setExecutingUser(String p) {
        this.executingUser = p
    }

    Optional<String> getAccountingName() {
        Optional.ofNullable(configurationValues.
                getString(ConfigurationConstants.CVALUE_ACCOUNTING_NAME,
                        null))
    }

    JobExecutionEnvironment getJobExecutionEnvironmentOrBash() {
        String envName = configurationValues.
                get(ConfigurationConstants.CVALUE_JOB_EXECUTION_ENVIRONMENT, "bash").
                toString().
                trim()
        try {
            JobExecutionEnvironment.from(envName)
        } catch (IllegalArgumentException ex) {
            addError(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("Unknown jobExecutionEnvironment name: '$envName'. WARNING Continuing with 'bash'"))
            JobExecutionEnvironment.bash
        }
    }


    /** You can set the path to the container engine. If it is not set then we use the name (apptainer, singularity)
     *  as relative path and hope it is the name of the executable and in the PATH variable on the execution host.
     *
     *  This should be the path on the remote system, if the jobs are executed remotely.
     */
    Path getContainerEnginePath() {
        String pathStr = configurationValues.
                get(ConfigurationConstants.CVALUE_CONTAINER_ENGINE_PATH, "").toString()
        if (pathStr.size() > 0) {
            Paths.get(pathStr)
        } else {
            jobExecutionEnvironmentOrBash.toPath()
        }
    }

    /** Container engine arguments as needed to run the container, even when running in a "contained" mode.
     *  Some variables are provided to the remote process via the submission command line (e.g. PARAMETER_FILE),
     *  others are defined by the execution system (e.g. LSB_JOBID). Here we collect this information.
     *
     *  @return    List of environment variable names
     *  */
    List<String> getRoddyContainerCopyVariables() {
    [       // General variables
            "USER",                    // Not sure, whether that is necessary.

            // The variables created in the cluster job by the JobManager.
            Roddy.jobManager.jobIdVariable,
            Roddy.jobManager.jobNameVariable,
            Roddy.jobManager.queueVariable,

            // The variables that BatchEuphoria adds to the submission command.
            ConfigurationConstants.DEBUG_WRAP_IN_SCRIPT,
            Constants.APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT,
            JobConstants.PRM_TOOL_ID,
            Constants.PARAMETER_FILE]
    }



    /** Container engine arguments selected by the user as cvalues */
    List<String> getUserContainerEngineArguments() {
        configurationValues.
                get(ConfigurationConstants.CVALUE_APPTAINER_ARGUMENTS, "").toStringList(",")
    }

    /** All directories to which the container needs access need to be mounted into the container
     *  This includes, for example:
     *
     *    * Additional software environments (virtualenv, conda, etc.)
     *    * Reference data directories (reference genomes, annotations, etc.)
     *
     * In the moment, Roddy has no way to know these directories. The variables could be declared as such in
     * the plugins, but they are not (yet). Therefore, for now we need to ask the user to declare them in
     * the configuration files.
     *
     * Bind specifications are in the commonly known format (Docker, Apptainer),
     *
     *   /host/path((|:/container/path)|:/container/path:(ro|rw))?
     *
     * * If no target path is given, the same path as the host-path is used in the container.
     * * If no mode is given, "ro" will be assumed.
     */
    private static BindSpec fromBindSpecString(String bindSpecString) {
        String[] parts = bindSpecString.split(":")
        if (parts.size() == 1) {
            return new BindSpec(Paths.get(parts[0]))
        } else if (parts.size() == 2) {
            return new BindSpec(Paths.get(parts[0]),
                                Paths.get(parts[1]))
        } else if (parts.size() == 3) {
            return new BindSpec(Paths.get(parts[0]),
                                Paths.get(parts[1]),
                                BindSpec.Mode.from(parts[2]))
        } else {
            throw new IllegalArgumentException("Invalid bind specification: " + bindSpecString)
        }
    }
    List<BindSpec> getUserContainerMounts() {
        configurationValues.
                get(ConfigurationConstants.CVALUE_CONTAINER_MOUNTS, "").
                toStringList(' ').
                collect { fromBindSpecString(it) }
    }

    String getContainerImage() {
        configurationValues.
            get(ConfigurationConstants.CVALUE_CONTAINER_IMAGE).
            toString()
    }

    /** Additionally, to the directories explicitly requested by the caller, Roddy will also mount all necessary
     *  directories into the container that it knows of. These are
     *
     *    * outputBaseDirectory (RW)
     *    * inputBaseDirectory (RO)
     *    * scratchBaseDirectory (RW)
     *    * analysisToolsDirectory (RO), which contains the plugin code
     */
    List<BindSpec> getRoddyMounts() {
        [new BindSpec(
                runtimeService.getOutputBaseDirectory(this).toPath(),
                runtimeService.getOutputBaseDirectory(this).toPath(),
                BindSpec.Mode.RW),
        new BindSpec(
                runtimeService.getInputBaseDirectory(this).toPath(),
                runtimeService.getInputBaseDirectory(this).toPath(),
                BindSpec.Mode.RO),
        new BindSpec(
                roddyScratchBaseDir.toPath(),
                roddyScratchBaseDir.toPath(),
                BindSpec.Mode.RW),
        new BindSpec(
                getAnalysisToolsDirectory().toPath(),
                getAnalysisToolsDirectory().toPath(),
                BindSpec.Mode.RO)]
    }



    /**
     * Returns the execution directory for this context. If it was not set this is done here.
     *
     * @return
     */
    synchronized File getExecutionDirectory() {
        if (executionDirectory == null)
            executionDirectory = runtimeService.getExecutionDirectory(this)
        return executionDirectory
    }

    File getFileForAnalysisToolsArchiveOverview() {
        if (!md5OverviewFile)
            md5OverviewFile = runtimeService.getAnalysedMD5OverviewFile(this)
        return md5OverviewFile
    }

    synchronized File getCommonExecutionDirectory() {
        if (!commonExecutionDirectory)
            commonExecutionDirectory = runtimeService.getCommonExecutionDirectory(this)
        return commonExecutionDirectory
    }

    boolean hasExecutionDirectory() {
        return executionDirectory != null
    }
    Boolean checkedIfAccessRightsCanBeSet = null

    boolean isAccessRightsModificationAllowed() {
        // Include an additional check, if the target filesystem allows the modification and disable this, if necessary.
        if (checkedIfAccessRightsCanBeSet != null)
            return checkedIfAccessRightsCanBeSet
        boolean modAllowed = configurationValues.getBoolean(
                ConfigurationConstants.CFG_ALLOW_ACCESS_RIGHTS_MODIFICATION, true)
        if (modAllowed && checkedIfAccessRightsCanBeSet == null) {
            checkedIfAccessRightsCanBeSet = FileSystemAccessProvider.instance.checkIfAccessRightsCanBeSet(this)
            if (!checkedIfAccessRightsCanBeSet) {
                modAllowed = false
                addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.
                        expand("Access rights modification was disabled. The test on the file " +
                                "system raised an error.", Level.WARNING))
            }
        }
        return modAllowed
    }

    FileSystemAccessProvider getFileSystemAccessProvider() {
        return FileSystemAccessProvider.instance
    }

    Integer getMaxFileAccessAttempts() {
        return configurationValues.getInteger(ConfigurationConstants.CFG_MAX_FILE_ACCESS_ATTEMPTS, 3)
    }

    Integer getFileAccessRetryWaitTimeMS() {
        return configurationValues.getInteger(ConfigurationConstants.CFG_FILE_ACCESS_RETRY_WAIT_TIME_MS, 100)
    }

    String getOutputDirectoryAccessRights() {
        if (!isAccessRightsModificationAllowed()) return null
        configurationValues.get(ConfigurationConstants.CFG_OUTPUT_ACCESS_RIGHTS_FOR_DIRECTORIES,
                fileSystemAccessProvider.commandSet.defaultAccessRightsString).toString()
    }

    String getOutputFileAccessRights() {
        if (!isAccessRightsModificationAllowed()) return null
        return configurationValues.get(ConfigurationConstants.CFG_OUTPUT_ACCESS_RIGHTS,
                fileSystemAccessProvider.commandSet.defaultAccessRightsString).toString()
    }

    String getOutputGroupString() {
        if (!isAccessRightsModificationAllowed()) return null
        return configurationValues.get(ConfigurationConstants.CFG_OUTPUT_FILE_GROUP,
                fileSystemAccessProvider.myGroup).toString()
    }

    String getUMask() {
        return configurationValues.getString(ConfigurationConstants.CFG_OUTPUT_UMASK,
                fileSystemAccessProvider.commandSet.defaultUMask)
    }

    synchronized File getLockFilesDirectory() {
        if (lockFilesDirectory == null)
            lockFilesDirectory = runtimeService.getLockFilesDirectory(this)
        return lockFilesDirectory
    }

    synchronized File getTemporaryDirectory() {
        if (temporaryDirectory == null)
            temporaryDirectory = runtimeService.getTemporaryDirectory(this)
        return temporaryDirectory
    }

    synchronized File getLoggingDirectory() {
        if (loggingDirectory == null) {
            loggingDirectory = runtimeService.getLoggingDirectory(this)
            assert (null != loggingDirectory)
        }
        return loggingDirectory
    }

    synchronized File getAnalysisToolsDirectory() {
        if (analysisToolsDirectory == null)
            analysisToolsDirectory = runtimeService.getAnalysisToolsDirectory(this)
        return analysisToolsDirectory
    }

    File getParameterFilename(Job job) {
        new File(executionDirectory, "${job.jobName}_${job.jobCreationCounter}${Constants.PARAMETER_FILE_SUFFIX}")
    }

    void addFile(BaseFile file) {
        if (!processingFlag.contains(ProcessingFlag.STORE_FILES))
            return
        if (executionContextLevel == ExecutionContextLevel.QUERY_STATUS
                || executionContextLevel == ExecutionContextLevel.RERUN
                || executionContextLevel == ExecutionContextLevel.TESTRERUN
                || executionContextLevel == ExecutionContextLevel.RUN) {
            synchronized (allFilesInRun) {
                this.allFilesInRun.add(file)
            }
        }
    }

    List<BaseFile> getAllFilesInRun() {
        List<BaseFile> newList = new LinkedList<BaseFile>()
        synchronized (allFilesInRun) {
            newList.addAll(allFilesInRun)
        }
        return newList
    }

    List<Job> getExecutedJobs() {
        return new LinkedList<Job>(jobsForProcess)
    }

    List<Job> getStartedJobs() {
        return jobsForProcess.findAll { Job job ->
            job != null && !job.fakeJob && !job.getJobState().dummy
        }
    }

    void setExecutedJobs(List<Job> previousJobs) {
        //TODO Add processing flag query
        jobsForProcess.clear()
        jobsForProcess.addAll(previousJobs)
    }

    void addExecutedJob(Job job) {
        if ((job.jobState == JobState.DUMMY || job.jobState == JobState.UNKNOWN) &&
                !processingFlag.contains(ProcessingFlag.STORE_DUMMY_JOBS))
            return
        jobsForProcess.add(job)
    }

    /**
     * Determines if an execution context still has some jobs which are running.
     * Only works for the latest readout execution context and a context which was just created.
     *
     * @return
     */
    boolean hasRunningJobs() {
        if (jobsForProcess.size() == 0) {
            return false
        }

        //Query readout jobs.
        if (jobsForProcess.get(0) instanceof ReadOutJob) {
            for (BEJob job : jobsForProcess) {
                ReadOutJob rj = (ReadOutJob) job

                def state = rj.jobState
                if (state.plannedOrRunning) {
                    if (state.runningOrStarted)
                        return true
                    else {
                        //Check previous jobs... Or just wait for the next step???

                    }
                }
            }
        }

        // Query current jobs, i.e. on recheck
        List<String> jobIDsForQuery = new LinkedList<>()
        for (BEJob job : jobsForProcess) {
            BEJobResult runResult = job.runResult
            if (runResult != null && runResult.jobID.id != null) {
                jobIDsForQuery.add(runResult.jobID.id)
            }
        }
        Map<BEJob, JobState> map = Roddy.jobManager.queryJobStatus(jobsForProcess as List<BEJob>)
        for (JobState js : map.values()) {
            if (js.plannedOrRunning)
                return true
        }

        return false
    }

    void addCalledCommand(BECommand command) {
        commandCalls.add(command)
    }

    List<BECommand> getCommandCalls() {
        return commandCalls
    }

    List<File> getLogFilesForExecutedJobs() {
        return Roddy.jobManager.queryExtendedJobStateById(executedJobs*.jobID).collect { it.value.logFile }
    }

    List<File> getAdditionalLogFiles() {
        return runtimeService.getAdditionalLogFilesForContext(this)
    }

    /**
     * Appends an entry to the error, warnings or info list. Will internally call addError, addWarning or addInfo
     * Will treat other levels than warning, info or severe as severe!
     */
    void addErrorEntry(ExecutionContextError error) {
        switch (error.errorLevel) {
            case Level.WARNING:
                addWarning(error)
                break
            case Level.INFO:
                addInfo(error)
                break
            case Level.SEVERE:
                addError(error)
                break
            default:
                logger.warning(
                        "The message '${error.description}' had an unknown level ${error.errorLevel} and" +
                        "will be treated as an error.")
                addError(error)
        }
    }

    /**
     * API Level 3.2+
     */
    void addError(ExecutionContextError error) {
        this.errors << error
    }

    /**
     * API Level 3.2+
     */
    void addWarning(ExecutionContextError warning) {
        this.warnings << warning
    }

    /**
     * API Level 3.2+
     */
    void addInfo(ExecutionContextError info) {
        this.infos << info
    }

    /**
     * API Level 3.2+
     */
    boolean hasErrors() {
        return errors
    }

    /**
     * API Level 3.2+
     */
    boolean hasWarnings() {
        return warnings
    }

    /**
     * API Level 3.2+
     */
    boolean hasInfos() {
        return infos
    }

    /**
     * Returns a shallow copy of the errors list.
     *
     * @return
     */
    List<ExecutionContextError> getErrors() {
        return new LinkedList<>(errors)
    }

    /**
     * API Level 3.2+
     * Returns a shallow copy of the warnings list
     */
    List<ExecutionContextError> getWarnings() {
        return new LinkedList<>(warnings)
    }

    /**
     * API Level 3.2+
     * Returns a shallow copy of the infos list
     */
    List<ExecutionContextError> getInfos() {
        return new LinkedList<>(infos)
    }

    /**
     * Execute the stored dataset with the stored analysis.
     *
     * @return
     */
    boolean checkExecutability() {
        return workflow.checkExecutability(this)
    }

    boolean valueIsEmpty(Object value, String variableName = null) {
        if (value == null || value.toString() == "") {
            if (variableName)
                addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Expected value to be set: ${variableName}"))
            else
                addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("A value is null or empty."))
            return true
        }
        return false
    }

    // TODO The following methods use the EC only for logging! Move this stuff to the FileSystemAccessProvider!
    //      But the ExecutionContext is not only context but also log accumulator. Better use plain logging framework.

    boolean fileIsAccessible(File file, String variableName = null) {
        if (valueIsEmpty(file, variableName) ||
                !FileSystemAccessProvider.instance.checkFile(file, false, this)) {
            addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand(
                    "File '${file}' not accessible${variableName ? ": " + variableName : "."}"))
            return false
        }
        return true
    }

    boolean fileIsExecutable(File file, String variableName = null) {
        if (!(fileIsAccessible(file, variableName))) return false
        if (!FileSystemAccessProvider.instance.isExecutable(file)) {
            addError(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("File '${file}' is not executable${variableName ? ": " + variableName : "."}"))
            return false
        }
        return true
    }

    boolean directoryIsAccessible(File directory, String variableName = null) {
        if (valueIsEmpty(directory, variableName)
                || !FileSystemAccessProvider.instance.checkDirectory(directory, this, false)) {
            addError(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("Directory '${directory}' not accessible${variableName ? ": " + variableName : "."}"))
            return false
        }
        return true
    }

    /**
     * Execute the stored dataset with the stored analysis.
     *
     * @return
     */
    boolean execute() throws ConfigurationError {
        return workflow.execute(this)
    }

    @Override
    String toString() {
        return String.format("Context [%s-%s:%s, %s]",
                project.configurationName, analysis,
                dataSet, InfoObject.formatTimestamp(timestamp))
    }

    @NotNull ToolCommand getToolCommand(@NotNull String toolId)
            throws ConfigurationError {
        new ToolCommand(toolId,
                        new Executable(
                                configuration.getProcessingToolPath(this, toolId).toPath(),
                                getToolMd5(toolId)),
                        configuration.getSourceToolPath(toolId).toPath())
    }

    Command getWrapInCommand() {
        new Command(new Executable(
                configuration.getProcessingToolPath(this, Constants.TOOLID_WRAPIN_SCRIPT).toPath(),
                getToolMd5(Constants.TOOLID_WRAPIN_SCRIPT)))
    }

}
