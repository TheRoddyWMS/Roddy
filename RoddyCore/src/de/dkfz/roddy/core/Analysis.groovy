/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.FeatureToggles
import de.dkfz.roddy.BEException
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic
import org.apache.commons.io.filefilter.WildcardFileFilter

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate
import static de.dkfz.roddy.tools.RoddyIOHelperMethods.getStackTraceAsString

/**
 * An analysis represents the combination of a project, a workflow implementation and a configuration.
 * An analysis object is valid for only one project. The analysis configuration can be used by different projects.
 * Each project can have several analysis. So i.e. prostate can have whole_genome_analysis or exome_analysis.
 * Each found input dataset in the project can then be processed by all the projects analysis.
 * <p>
 * TODO Enable a initialize (cache erase?) to make it possible to reset data sets and data set states for an analysis.
 */
@CompileStatic
class Analysis {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Analysis.class.getSimpleName())

    /**
     * An analysis should have a unique name like i.e. whole_genome_processing or exome_analysis
     */
    private final String name

    /**
     * An analysis has a single link to a project, but the project can hav multiple analysis objects.
     */
    private final Project project

    /**
     * An analysis is directly linked to a configuration
     */
    private final Configuration configuration

    /**
     * The basic input directory for an analysis. This directory should then contain a list of datasets.
     * The list of datasets is created by the projects runtime service.
     */
    private File inputBaseDirectory

    /**
     * Runtime service is set in the analysis config. But the user can also set it for a project. The project then goes first, afterwards the analysis.
     */
    private RuntimeService runtimeService

    Analysis(String name, Project project, RuntimeService runtimeService, AnalysisConfiguration configuration) {
        this.name = name
        this.project = project
        this.configuration = configuration
        this.runtimeService = runtimeService
    }

    String getName() {
        return name
    }

    Project getProject() {
        return project
    }

    String getUsername() {
        return FileSystemAccessProvider.instance.callWhoAmI()
    }

    String getUsergroup() {
        //Get the default value.
        String groupID = FileSystemAccessProvider.instance.myGroup

        //If it is configured, get the group id from the config.
        boolean processSetUserGroup = configuration.configurationValues.
                getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, true)
        if (processSetUserGroup) {
            groupID = configuration.configurationValues.
                    getString(ConfigurationConstants.CFG_OUTPUT_FILE_GROUP, groupID)
        }
        return groupID
    }


    private ContextConfiguration _analysisConfiguration = null

    /**
     * Tests using analysis objects will need to implement the following:
     *
     * static {
     *   ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
     *   FileSystemAccessProvider.initializeProvider(true)
     * }
     *
     * If they don't, tests will fail with a NullPointerException!
     *
     * Alternatively, you could use the RoddyTestSpec class as a base for Spock tests.
     * @return
     */
    AnalysisConfiguration getConfiguration() {
        if (_analysisConfiguration == null) {
            _analysisConfiguration = new ContextConfiguration((AnalysisConfiguration) this.configuration,
                                                              (ProjectConfiguration) this.project.configuration)
            [
                    (Constants.USERNAME)                  : username,
                    (Constants.USERGROUP)                 : usergroup,
                    (Constants.USERHOME)                  : FileSystemAccessProvider.instance.userDirectory.absolutePath,
                    (Constants.PROJECT_CONFIGURATION_NAME): project.configurationName,
            ].each { String k, String v ->
                _analysisConfiguration.configurationValues << new ConfigurationValue(_analysisConfiguration, k, v)
            }
        }
        return _analysisConfiguration
    }

    @Override
    String toString() {
        return name
    }

    /**
     * Returns the base input directory for this analysis object.
     *
     * @return
     */
    File getInputBaseDirectory() {
        if (inputBaseDirectory == null)
            inputBaseDirectory = runtimeService.getInputBaseDirectory(this)
        assert (inputBaseDirectory != null)
        return inputBaseDirectory
    }

    /**
     * Returns the base output directory for this analysis object.
     *
     * @return
     */
    File getOutputBaseDirectory() {
        File outputBaseDirectory = runtimeService.getOutputBaseDirectory(this)
        assert (outputBaseDirectory != null)
        return outputBaseDirectory
    }

    /**
     * Returns the base output directory for this analysis object.
     *
     * @return
     */
    File getOutputAnalysisBaseDirectory() {
        return runtimeService.getOutputBaseDirectory(this)
    }

    DataSet getDataSet(String dataSetID) {
        // TODO: The avoidRecursion variable is more or less a hack. It work
        for (DataSet d : runtimeService.getListOfPossibleDataSets(this, true))
            if (d.id == dataSetID)
                return d
        return null
    }

    RuntimeService getRuntimeService() {
        RuntimeService rs = project.runtimeService
        if (rs == null) rs = runtimeService
        return rs
    }

    List<ExecutionContext> run(List<String> pidFilters, ExecutionContextLevel level) {
        return run(pidFilters, level, false)
    }

    /**
     * Executes this analysis for the specified list of identifiers.
     * An identifier can contain wildcards. The Apache WildCardFileFilter is used for wildcard resolution.
     *
     * @param pidFilters A list of identifiers or wildcards
     * @param level The level for the context execution
     * @return
     */
    List<ExecutionContext> run(List<String> pidFilters, ExecutionContextLevel level, boolean preventLoggingOnQueryStatus) {
        List<DataSet> selectedDatasets = getRuntimeService().loadDatasetsWithFilter(this, pidFilters)
        List<ExecutionContext> runs = new LinkedList<>()

        long creationCheckPoint = System.nanoTime()

        for (DataSet ds : selectedDatasets) {
            if (level.allowedToSubmitJobs && !canStartJobs(ds)) {
                logger.postAlwaysInfo("The ${Constants.DATASET_HR} ${ds.id} is still running and will be skipped for the process.")
                continue
            }

            ExecutionContext context =
                    new ExecutionContext(FileSystemAccessProvider.instance.callWhoAmI(), this, ds, level,
                            ds.getOutputFolderForAnalysis(this), ds.getInputFolderForAnalysis(this),
                            null, creationCheckPoint)

            executeRun(context, preventLoggingOnQueryStatus)
            runs.add(context)
        }
        return runs
    }

    /**
     * Reruns a list of execution context objects.
     * A new context is created using the old objects files.
     *
     * @param contexts
     * @return
     */
    List<ExecutionContext> rerun(List<ExecutionContext> contexts, boolean test) {
        long creationCheckPoint = System.nanoTime()
        LinkedList<ExecutionContext> newRunContexts = new LinkedList<>()
        for (ExecutionContext oldContext : contexts) {
            DataSet ds = oldContext.dataSet

            if (Roddy.getFeatureToggleValue(FeatureToggles.FailOnErroneousDryRuns) && oldContext.hasErrors()) {
                // Why print out here? Because the oldContext was started with suppressed messages (Default for QUERY_STATUS).
                // As there are errors, we'll print them here, otherwise we won't see them.
                printErrorsAndWarnings(oldContext)
                newRunContexts.add(oldContext)
                logger.postAlwaysInfo("\nYour tried to start an analysis using rerun or testrerun.\n" +
                        " This is a two step process, where the first step is used to gather information about previous runs." +
                        " However, this first step failed and Roddy will not continue.\n" +
                        " You can use the feature toggle 'FailOnErroneousDryRuns=false' to disable this behaviour.\n" +
                        " You can add it to the feature toggle file in ~/.roddy/featureToggles.ini")
                continue
            }

            if (!test && !canStartJobs(ds)) {
                logger.postAlwaysInfo("The ${Constants.DATASET_HR} ${ds.id} is still running and will be skipped for the process.")
                continue
            }

            ExecutionContext context =
                    new ExecutionContext(FileSystemAccessProvider.instance.callWhoAmI(),
                                         this,
                                         oldContext.dataSet,
                                         test ? ExecutionContextLevel.TESTRERUN : ExecutionContextLevel.RERUN,
                                         oldContext.outputDirectory,
                                         oldContext.inputDirectory,
                                         null,
                                         creationCheckPoint)

            context.allFilesInRun.addAll(oldContext.allFilesInRun)
            executeRun(context)
            newRunContexts.add(context)
        }
        return newRunContexts
    }

    private boolean canStartJobs(DataSet ds) {
        return !Roddy.getFeatureToggleValue(FeatureToggles.ForbidSubmissionOnRunning) || !hasKnownRunningJobs(ds)
    }

    boolean hasKnownRunningJobs(DataSet ds) {
        AnalysisProcessingInformation api = ds.getLatestValidProcessingInformation(this)
        ExecutionContext detailedProcessingInfo = api != null ? api.getDetailedProcessingInfo() : null
        return detailedProcessingInfo != null && detailedProcessingInfo.hasRunningJobs()
    }

    Map<DataSet, Boolean> checkStatus(List<String> pids) {
        return checkStatus(pids, false)
    }

    Map<DataSet, Boolean> checkStatus(List<String> pids, boolean suppressInfo) {
        List<DataSet> dataSets = getRuntimeService().loadDatasetsWithFilter(this, pids, suppressInfo)
        Map<DataSet, Boolean> results = new LinkedHashMap<>()
        dataSets.parallelStream().each({ DataSet ds ->
            boolean result = hasKnownRunningJobs(ds)
            synchronized (results) {
                results.put(ds, result)
            }
        })
        List<DataSet> sortedKeys = new LinkedList<>(results.keySet())
        sortedKeys.sort { DataSet ds1, DataSet ds2 ->
            ds1.id <=> ds2.id
        }
        Map<DataSet, Boolean> sortedMap = new LinkedHashMap<>()
        for (DataSet ds : sortedKeys) {
            sortedMap.put(ds, results.get(ds))
        }
        return sortedMap
    }

    @Deprecated
    List<DataSet> loadDatasetsWithFilter(List<String> pidFilters) {
        return loadDatasetsWithFilter(pidFilters, false)
    }

    @Deprecated()
    List<DataSet> loadDatasetsWithFilter(List<String> pidFilters, boolean suppressInfo) {
        return runtimeService.loadDatasetsWithFilter(this, pidFilters, suppressInfo)
    }

    @Deprecated
    List<DataSet> selectDatasetsFromPattern(List<String> pidFilters, List<DataSet> listOfDataSets, boolean suppressInfo) {
        return runtimeService.selectDatasetsFromPattern(this, pidFilters, listOfDataSets, suppressInfo)
    }

    /**
     * Creates a single execution context (similar to context(pidfilters...)) but does not execute it.
     * This method is mainly for ui based / asynchnronous execution context generation.
     * A separate thread is created which executes the context.
     *
     * @param pidFilter
     * @param executionContextLevel
     * @return
     */
    ExecutionContext runDeferredContext(String pidFilter, final ExecutionContextLevel executionContextLevel) {
        long creationCheckPoint = System.nanoTime()
        for (DataSet ds : getRuntimeService().getListOfPossibleDataSets(this)) {
            if (!new WildcardFileFilter(pidFilter).accept(ds.getInputFolderForAnalysis(this)))
                continue
            final ExecutionContext context = new ExecutionContext(
                    FileSystemAccessProvider.instance.callWhoAmI(),
                    this,
                    ds,
                    executionContextLevel,
                    ds.getOutputFolderForAnalysis(this),
                    ds.getInputFolderForAnalysis(this),
                    null,
                    creationCheckPoint)
            runDeferredContext(context)
            return context
        }
        return null
    }

    /**
     */
    ExecutionContext rerunDeferredContext(ExecutionContext oldContext, final ExecutionContextLevel executionContextLevel, long creationCheckPoint, boolean test) {
        ExecutionContext context = new ExecutionContext(oldContext.executingUser,
                                                        oldContext.analysis,
                                                        oldContext.dataSet,
                                                        test ? ExecutionContextLevel.TESTRERUN : ExecutionContextLevel.RERUN,
                                                        oldContext.outputDirectory,
                                                        oldContext.inputDirectory,
                                                        null,
                                                        creationCheckPoint)
        context.allFilesInRun.addAll(oldContext.allFilesInRun)
        runDeferredContext(context)
        return context
    }

    /**
     * Runs the passed execution context in a separate thread. The context level is taken from the passed object.
     *
     * @param ec The execution context which will be context in a separate thread.
     */
    void runDeferredContext(final ExecutionContext ec) {
        Thread t = Thread.start(String.format('Deferred execution context execution for ' + Constants.DATASET_HR + ' %s', ec.dataSet.id)) {
            executeRun(ec)
        }
    }

    protected void executeRun(ExecutionContext context) {
        executeRun(context, false)
    }


    protected boolean prepareExecution(ExecutionContext context) {
        logger.rare(context.executionContextLevel.toString())
        String datasetID = context.dataSet.id
        boolean contextRightsSettings = ExecutionService.instance.checkAccessRightsSettings(context)
        boolean contextPermissions = ExecutionService.instance.checkContextDirectoriesAndFiles(context)
        boolean configurationValidity =
                Roddy.strictModeEnabled && !Roddy.isOptionSet(RoddyStartupOptions.ignoreconfigurationerrors) ? !configuration.hasLoadErrors() : true

        // The setup of the workflow and the executability check may require the execution store, e.g. for
        // synchronously called jobs to gather data from the remote side.
        ExecutionService.instance.writeFilesForExecution(context)

        boolean setupExecutionStatus = context.workflow.setupExecution()
        boolean contextExecutability = context.workflow.checkExecutability()

        boolean isExecutable = setupExecutionStatus && contextRightsSettings && contextPermissions && contextExecutability && configurationValidity

        if (!isExecutable) {
            StringBuilder message =
                    new StringBuilder('The workflow does not seem to be executable for dataset ' + datasetID)
            if (!contextRightsSettings)
                message.append('\n\tContext access rights settings could not be validated.')
            if (!contextPermissions)
                message.append('\n\tContext permissions could not be validated.')
            if (!contextExecutability)
                message.append('\n\tContext and workflow are not considered executable.')
            if (!configurationValidity)
                message.append('\n\tContext configuration has errors.')
            logger.severe(message.toString())
        }

        return isExecutable
    }

    protected void withErrorReporting(ExecutionContext context, boolean preventLoggingOnQueryStatus, Closure code) {
        try {
            code()
        } catch (ConfigurationError e) {
            logger.sometimes(e.message + Constants.ENV_LINESEPARATOR + getStackTraceAsString(e))
            context.addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand(e.message))
        } catch (IOException e) {
            logger.always(e.message)
            context.addError(ExecutionContextError.EXECUTION_UNCAUGHTERROR.expand(e.message))
        } catch (Exception e) {
            logger.always("An unhandled exception of type '" + e.class.canonicalName + "' occurred: '" + e.localizedMessage + "'")
            logger.always(e.message + Constants.ENV_LINESEPARATOR + getStackTraceAsString(e))
            context.addError(ExecutionContextError.EXECUTION_UNCAUGHTERROR.expand(e.message))
        } finally {
            // Look up errors when jobs are executed directly and when there were any started jobs.
            if (context.getStartedJobs().size() > 0) {
                String failedJobs = ""
                for (Job job : context.getExecutedJobs()) {
                    if (job.getJobState() == JobState.FAILED)
                        failedJobs += "\n\t" + job.jobID + ",\t" + job.jobName
                }
                if (failedJobs.length() > 0)
                    context.addError(ExecutionContextError.EXECUTION_JOBFAILED.expand('Job(s) failed to execute:' + failedJobs))
            }

            // Print out informational messages like infos, warnings, errors
            // Only print them out if !QUERY_STATUS and the runmode is testrun or testrerun.
            if ((!preventLoggingOnQueryStatus || (context.executionContextLevel != ExecutionContextLevel.QUERY_STATUS))) {
                printErrorsAndWarnings(context)
            }
        }
    }

    void printErrorsAndWarnings(ExecutionContext context) {
        boolean printed = printConfigurationErrorsAndWarnings(context)
        printed |= printMessagesForContext(context)
        if (printed)
            logger.always("\nPlease check extended logs in " + logger.getCentralLogFile() + " for more details.")
    }

    /**
     * Executes the context object.
     * If the contexts level is QUERY_STATUS:
     * Writes context specific files before execution (like analysis tools, configuration files).
     * Writes some log files after execution.
     * <p/>
     * If its level is QUERY_STATUS:
     * Check the file validity after execution.<br />
     * Also there are no files written for this level.
     *
     * @param context
     */
    protected void executeRun(ExecutionContext context, boolean preventLoggingOnQueryStatus) {
        withErrorReporting(context, preventLoggingOnQueryStatus) {
            logger.rare(context.executionContextLevel.toString())
            String datasetID = context.dataSet.id

            boolean isExecutable = prepareExecution(context)

            if (isExecutable) {
                boolean successfullyExecuted = false
                try {
                    boolean execute = true
                    if (context.executionContextLevel.allowedToSubmitJobs) { // Only do these checks, if we are not in query mode!
                        List<String> invalidPreparedFiles = ExecutionService.instance.checkForInaccessiblePreparedFiles(context)
                        boolean copiedAnalysisToolsAreExecutable = ExecutionService.instance.checkCopiedAnalysisTools(context)
                        boolean ignoreFileChecks = Roddy.isOptionSet(RoddyStartupOptions.disablestrictfilechecks)
                        execute &= ignoreFileChecks || (invalidPreparedFiles.size() == 0 && copiedAnalysisToolsAreExecutable)
                        if (!execute) {
                            StringBuilder message =
                                    new StringBuilder('There were errors after preparing the workflow run for dataset ' + datasetID)
                            if (invalidPreparedFiles.size() > 0)
                                message.append('\n\tSome files could not be written. Workflow will not execute.\n\t' +
                                        String.join('\t\n', invalidPreparedFiles))
                            if (!copiedAnalysisToolsAreExecutable)
                                message.append('\n\tSome declared tools are not executable. Workflow will not execute.')
                            if (ignoreFileChecks) {
                                message.append('\n  The errors were ignored because disablestrictfilechecks is set.')
                            }
                            logger.severe(message.toString())
                        }
                    }

                    // Finally, if execution is allowed, run it and start the submitted jobs (if hold jobs is enabled)
                    if (execute) {
                        successfullyExecuted = context.execute()
                        if (successfullyExecuted)
                            finallyStartJobsOfContext(context)
                    }
                } catch (Exception ex) {
                    successfullyExecuted = false
                    maybeAbortStartedJobsOfContext(context)
                    throw ex
                } finally {

                    if (context.executionContextLevel == ExecutionContextLevel.QUERY_STATUS) { // Clean up
                        // Query file validity of all files
                        FileSystemAccessProvider.instance.validateAllFilesInContext(context)
                        if (context.getExecutionDirectory().getName().contains(ConfigurationConstants.RODDY_EXEC_DIR_PREFIX))
                            FileSystemAccessProvider.instance.removeDirectory(context.getExecutionDirectory())
                    } else {
                        if (!successfullyExecuted)
                            maybeAbortStartedJobsOfContext(context)

                        cleanUpAndFinishWorkflowRun(context)
                    }
                }
            }
        }
    }

    private boolean printConfigurationErrorsAndWarnings(ExecutionContext context) {
        String datasetID = context.dataSet.id
        Configuration configuration = context.configuration

        if (configuration.hasErrors()) {
            printMessages(datasetID, 'configuration errors', condense(configuration.errors))
            printMessages(datasetID, 'configuration errors', configuration.errors, false)
        }
        if (configuration.hasWarnings()) {
            printMessages(datasetID, 'configuration warnings', condense(configuration.warnings))
            printMessages(datasetID, 'configuration warnings', configuration.warnings, false)
        }
        if (configuration.hasLoadErrors())
            printMessages(datasetID, 'configuration load errors', configuration.listOfLoadErrors)

        return configuration.hasLoadErrors() | configuration.hasWarnings() | configuration.hasErrors()
    }

    /**
     * Takes ConfigurationIssues, sorts them by their template identifier and replaces multiple variants of the same
     * template with their collective message. If only one value of a template exists, its message is used.
     *
     * @param issues
     * @return Return a list of messages and collective messages.
     */
    static List<String> condense(List<ConfigurationIssue> issues) {
        List<String> result = []
        for (ConfigurationIssueTemplate template : ConfigurationIssueTemplate.values()) {
            int count = (int) issues.count { it.id == template }
            if (count > 1) {
                result << template.collectiveMessage
            } else if (count == 1) {
                result << issues.find { it.id == template }.message
            }
        }
        result
    }

    private boolean printMessagesForContext(ExecutionContext context) {
        String datasetID = context.dataSet.id
        if (context.hasInfos())
            printMessages(datasetID, 'execution infos', context.infos)
        if (context.hasWarnings())
            printMessages(datasetID, 'execution warnings', context.warnings)
        if (context.hasErrors())
            printMessages(datasetID, 'execution errors',
                          context.errors)
        boolean printed = context.hasWarnings() || context.hasErrors()
        return printed
    }

    private void printMessages(String datasetID, String title, List entries, boolean logAlways = true) {
        StringBuilder messages = new StringBuilder()
        messages.append('\nThere were ' + title + ' for dataset ' + datasetID)
        for (Object entry : entries) {
            messages.append('\n\t* ').append(entry.toString())
        }
        if (logAlways)
            logger.postAlwaysInfo(messages.toString())
        else
            logger.sometimes(messages.toString())
    }

    /**
     * Will start all the jobs in the context.
     *
     * @param context
     */
    private void finallyStartJobsOfContext(ExecutionContext context) throws BEException {
        Roddy.getJobManager().startHeldJobs(context.getExecutedJobs() as List<BEJob>)
    }

    /**
     * Finally write last logfiles or remove (or keep) the execution directory.
     *
     * @param context
     */
    private void cleanUpAndFinishWorkflowRun(ExecutionContext context) {
        //First, check if there were any executed jobs. If not, we can safely delete the the context directory.
        if (context.startedJobs.size() == 0) {
            if (Roddy.getCommandLineCall().isOptionSet(RoddyStartupOptions.forcekeepexecutiondirectory)) {
                logger.always('There were no started jobs, but Roddy will keep the execution directory as forcekeepexecutiondirectory was set.')
            } else {
                logger.always('There were no started jobs, the execution directory will be removed.')
                if (context.executionDirectory.name.contains(ConfigurationConstants.RODDY_EXEC_DIR_PREFIX))
                    FileSystemAccessProvider.instance.removeDirectory(context.executionDirectory)
                else {
                    throw new RuntimeException('A wrong path would be deleted: ' + context.executionDirectory.absolutePath)
                }
            }
        } else {
            ExecutionService.instance.writeAdditionalFilesAfterExecution(context)
        }
    }

    /**
     * Checks, whether strict mode AND rollback toggles are enabled and
     * try to abort jobs by calling the job manager.
     * Occurring exceptions are caught such that following code can be executed properly.
     *
     * @param context
     */
    private void maybeAbortStartedJobsOfContext(ExecutionContext context) {
        if (Roddy.isStrictModeEnabled() && context.getFeatureToggleStatus(FeatureToggles.RollbackOnWorkflowError)) {
            try {
                logger.severe('A workflow error occurred, try to rollback / abort submitted jobs.')
                Roddy.jobManager.killJobs(context.jobsForProcess as List<BEJob>)
                context.jobsForProcess.each { job ->
                    logger.severe("Revoked command for job ID" + job.jobID +
                            ": " + job.lastCommand.toBashCommandString())
                }
            } catch (Exception ex) {
                logger.severe('Could not abort jobs.', ex)
            }
        } else {
            logger.severe('A workflow error occurred, but strict mode is disabled and/or ' +
                    'RollbackOnWorkflowError is disabled. Submitted jobs will be left running.' +
                    '\n\tConsider enabling Roddy strict mode by setting the feature toggles ' +
                    "'StrictMode' and 'RollbackOnWorkflowError'.")
        }
    }

    /**
     * Calls a cleanup script and / or a workflows cleanup method to cleanup the directories of a workflow.
     * If you call the cleanup script, a new execution context log directory will be created for this purpose. This directory will not be created if
     * the workflows cleanup method is called!.
     *
     * @param pidList
     */
    void cleanup(List<String> pidList) {
        AnalysisConfiguration analysisConfiguration = (AnalysisConfiguration) getConfiguration()
        if (!analysisConfiguration.hasCleanupScript() && !ExecutionContext.createAnalysisWorkflowObject(this).hasCleanupMethod())
            logger.postAlwaysInfo('There is neither a configured cleanup script or a native workflow cleanup method available for this analysis.')

        List<DataSet> dataSets = runtimeService.loadDatasetsWithFilter(this, pidList, true)
        for (DataSet ds : dataSets) {

            ExecutionContext context = new ExecutionContext(FileSystemAccessProvider.instance.callWhoAmI(), this,
                    ds, ExecutionContextLevel.CLEANUP, ds.getOutputFolderForAnalysis(this), ds.getInputFolderForAnalysis(this),
                    null)

            if (analysisConfiguration.hasCleanupScript()) {
                withErrorReporting(context, false, {
                    String cleanupScript = analysisConfiguration.cleanupScript
                    if (cleanupScript != null && cleanupScript != "") {
                        Job cleanupJob = new Job(context, "cleanupScript", cleanupScript, null)
                        try {
                            ExecutionService.instance.writeFilesForExecution(context)
                            cleanupJob.run()
                        } finally {
                            ExecutionService.instance.writeAdditionalFilesAfterExecution(context)
                        }
                    }
                })
            }

            // Call the workflows cleanup java method.
            if (context.getWorkflow().hasCleanupMethod()) {
                withErrorReporting(context, false) {
                    Workflow wf = context.workflow
                    boolean isExecutable = prepareExecution(context)
                    if (!isExecutable) {
                        logger.severe(
                                new StringBuilder('The workflow does not seem to be executable for dataset ' +
                                                          context.dataSet.id).
                                        append('\n\tSetup for cleanup failed.').
                                        toString())
                    } else {
                        try {
                            boolean successfullyExecuted = wf.cleanup()
                            if (successfullyExecuted)
                                finallyStartJobsOfContext(context)
                        } finally {
                            ExecutionService.getInstance().writeAdditionalFilesAfterExecution(context)
                        }
                    }
                }
            }
        }
    }

    File getReadmeFile() {
        configuration.preloadedConfiguration.readmeFile
    }
}
