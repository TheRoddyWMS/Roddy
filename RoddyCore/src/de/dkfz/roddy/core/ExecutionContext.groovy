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
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic

import java.util.logging.Level

/**
 * An ExecutionContext is the runtime context for an analysis and a DataSet.<br />
 * It keeps track of context relevant information like:<br />
 * <ul>
 * <li>Created files</li>
 * <li>Executed jobs and commands</li>
 * <li>BEJob states</li>
 * <li>Log files</li>
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
    private final List<Command> commandCalls = new LinkedList<Command>().asSynchronized()
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
        Class workflowClass = LibrariesFactory.getInstance().searchForClass(analysis.configuration.getWorkflowClass())
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

    Map<String, Object> getDefaultJobParameters(String TOOLID) {
        return runtimeService.getDefaultJobParameters(this, TOOLID)
    }

    String createJobName(BaseFile p, String TOOLID) {
        return createJobName(p, TOOLID, false)
    }

    String createJobName(BaseFile p, String TOOLID, boolean reduceLevel) {
        return runtimeService.createJobName(this, p, TOOLID, reduceLevel)
    }

    File getInputDirectory() {
        return inputDirectory
    }

    File getOutputDirectory() {
        return outputDirectory
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
        boolean modAllowed = configurationValues.getBoolean(ConfigurationConstants.CFG_ALLOW_ACCESS_RIGHTS_MODIFICATION, true)
        if (modAllowed && checkedIfAccessRightsCanBeSet == null) {
            checkedIfAccessRightsCanBeSet = FileSystemAccessProvider.instance.checkIfAccessRightsCanBeSet(this)
            if (!checkedIfAccessRightsCanBeSet) {
                modAllowed = false
                addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.
                        expand("Access rights modification was disabled. The test on the file system raised an error.", Level.WARNING))
            }
        }
        return modAllowed
    }

    FileSystemAccessProvider getFileSystemAccessProvider() {
        return FileSystemAccessProvider.instance
    }

    String getOutputDirectoryAccess() {
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
        return jobsForProcess.findAll { Job job -> job != null && !job.fakeJob && !job.getJobState().dummy }
    }

    void setExecutedJobs(List<Job> previousJobs) {
        //TODO Add processing flag query
        jobsForProcess.clear()
        jobsForProcess.addAll(previousJobs)
    }

    void addExecutedJob(Job job) {
        if ((job.jobState == JobState.DUMMY || job.jobState == JobState.UNKNOWN) && !processingFlag.contains(ProcessingFlag.STORE_DUMMY_JOBS))
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

        //Query current jobs, i.e. on recheck
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

    void addCalledCommand(Command command) {
        commandCalls.add(command)
    }

    List<Command> getCommandCalls() {
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
                logger.warning("The message '${error.description}' had an unknown level ${error.errorLevel} and will be treated as an error.")
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

    boolean fileIsAccessible(File file, String variableName = null) {
        if (valueIsEmpty(file, variableName) || !FileSystemAccessProvider.getInstance().checkFile(file, false, this)) {
            addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("File '${file}' not accessible${variableName ? ": " + variableName : "."}"))
            return false
        }
        return true
    }

    boolean fileIsExecutable(File file, String variableName = null) {
        if (!(fileIsAccessible(file, variableName))) return;
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

}
