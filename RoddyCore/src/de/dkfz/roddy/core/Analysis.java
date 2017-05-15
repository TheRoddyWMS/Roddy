/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.eilslabs.batcheuphoria.jobs.JobState;
import de.dkfz.roddy.AvailableFeatureToggles;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.config.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.*;
import java.util.logging.Level;


/**
 * An analysis represents the combination of a project, a workflow implementation and a configuration.
 * An analysis object is valid for only one project. The analysis configuration can be used by different projects.
 * Each project can have several analysis. So i.e. prostate can have whole_genome_analysis or exome_analysis.
 * Each found input dataset in the project can then be processed by all the projects analysis.
 * <p>
 * TODO Enable a initialize (cache erase?) to make it possible to reset data sets and data set states for an analysis.
 */
public class Analysis {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(Analysis.class.getSimpleName());

    /**
     * An analysis should have a unique name like i.e. whole_genome_processing or exome_analysis
     */
    private final String name;

    /**
     * An analysis has a single link to a project, but the project can hav multiple analysis objects.
     */
    private final Project project;

    /**
     * An analysis has a single link to a workflow but a workflow can be used by multiple workflows.
     */
    private final Workflow workflow;

    /**
     * An analysis is directly linked to a configuration
     */
    private final Configuration configuration;

    /**
     * The basic input directory for an analysis. This directory should then contain a list of datasets.
     * The list of datasets is created by the projects runtime service.
     */
    private File inputBaseDirectory;

    /**
     * Runtime service is set in the analysis config. But the user can also set it for a project. The project then goes first, afterwards the analysis.
     */
    private RuntimeService runtimeService;

    public Analysis(String name, Project project, Workflow workflow, RuntimeService runtimeService, AnalysisConfiguration configuration) {
        this.name = name;
        this.project = project;
        this.workflow = workflow;
        this.configuration = configuration;
        this.runtimeService = runtimeService;
    }

    public String getName() {
        return name;
    }

    public Project getProject() {
        return project;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public String getUsername() {
        try {
            return FileSystemAccessProvider.getInstance().callWhoAmI();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public String getUsergroup() {
        try {
            //Get the default value.
            String groupID = FileSystemAccessProvider.getInstance().getMyGroup();

            //If it is configured, get the group id from the config.
            boolean processSetUserGroup = getConfiguration().getConfigurationValues().getBoolean(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, true);
            if (processSetUserGroup) {
                groupID = getConfiguration().getConfigurationValues().getString(ConfigurationConstants.CFG_OUTPUT_FILE_GROUP, groupID);
            }
            return groupID;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * The object should actually move to context itself. However, currently, this would break a lot.
     * Mark it as Deprecated for now.
     */
    @Deprecated
    private ContextConfiguration _contextConfiguration = null;

    public Configuration getConfiguration() {
        if (_contextConfiguration == null)
            _contextConfiguration = new ContextConfiguration((AnalysisConfiguration) this.configuration, (ProjectConfiguration) this.project.getConfiguration());
        return _contextConfiguration;
//        return configuration;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns the base input directory for this analysis object.
     *
     * @return
     */
    public File getInputBaseDirectory() {
        if (inputBaseDirectory == null)
            inputBaseDirectory = getRuntimeService().getInputFolderForAnalysis(this);
        return inputBaseDirectory;
    }

    /**
     * Returns the base output directory for this analysis object.
     *
     * @return
     */
    public File getOutputBaseDirectory() {
        return getRuntimeService().getOutputFolderForAnalysis(this);
    }

    /**
     * Returns the base output directory for this analysis object.
     *
     * @return
     */
    public File getOutputAnalysisBaseDirectory() {
        return getRuntimeService().getOutputFolderForAnalysis(this);
    }

    public List<DataSet> getListOfDataSets() {
        return getRuntimeService().getListOfPossibleDataSets(this);
    }

    public DataSet getDataSet(String dataSetID) {
        // TODO: The avoidRecursion variable is more or less a hack. It work
        for (DataSet d : getRuntimeService().getListOfPossibleDataSets(this, true))
            if (d.getId().equals(dataSetID))
                return d;
        return null;
    }

    public RuntimeService getRuntimeService() {
        RuntimeService rs = project.getRuntimeService();
        if (rs == null) rs = runtimeService;
        return rs;
    }

    public List<ExecutionContext> run(List<String> pidFilters, ExecutionContextLevel level) {
        return run(pidFilters, level, false);
    }

    /**
     * Executes this analysis for the specified list of identifiers.
     * An identifier can contain wildcards. The Apache WildCardFileFilter is used for wildcard resolution.
     *
     * @param pidFilters A list of identifiers or wildcards
     * @param level      The level for the context execution
     * @return
     */
    public List<ExecutionContext> run(List<String> pidFilters, ExecutionContextLevel level, boolean preventLoggingOnQueryStatus) {
        List<DataSet> selectedDatasets = getRuntimeService().loadDatasetsWithFilter(this, pidFilters);
        List<ExecutionContext> runs = new LinkedList<>();

        long creationCheckPoint = System.nanoTime();

        for (DataSet ds : selectedDatasets) {
            if (level.isOrWasAllowedToSubmitJobs && !checkJobStartability(ds)) continue;

            ExecutionContext ec = new ExecutionContext(FileSystemAccessProvider.getInstance().callWhoAmI(), this, ds, level, ds.getOutputFolderForAnalysis(this), ds.getInputFolderForAnalysis(this), null, creationCheckPoint);

            executeRun(ec, preventLoggingOnQueryStatus);
            runs.add(ec);
        }
        return runs;
    }

    /**
     * Reruns a list of execution context objects.
     * A new context is created using the old objects files.
     *
     * @param contexts
     * @return
     */
    public List<ExecutionContext> rerun(List<ExecutionContext> contexts, boolean test) {
        long creationCheckPoint = System.nanoTime();
        LinkedList<ExecutionContext> newRuns = new LinkedList<>();
        for (ExecutionContext oldContext : contexts) {
            DataSet ds = oldContext.getDataSet();
            if (!test && !checkJobStartability(ds)) continue;

            ExecutionContext context = new ExecutionContext(FileSystemAccessProvider.getInstance().callWhoAmI(), this, oldContext.getDataSet(), test ? ExecutionContextLevel.TESTRERUN : ExecutionContextLevel.RERUN, oldContext.getOutputDirectory(), oldContext.getInputDirectory(), null, creationCheckPoint);

            context.getAllFilesInRun().addAll(oldContext.getAllFilesInRun());
            executeRun(context);
            newRuns.add(context);
        }
        return newRuns;
    }

    private boolean checkJobStartability(DataSet ds) {
        String datasetID = ds.getId();
        boolean running = checkStatusForDataset(ds);

        if (running && Roddy.getFeatureToggleValue(AvailableFeatureToggles.ForbidSubmissionOnRunning)) {
            logger.postAlwaysInfo("The pid " + datasetID + " is still running and will be skipped for the process.");
            return false;
        }
        return true;
    }

    public boolean checkStatusForDataset(DataSet ds) {
        AnalysisProcessingInformation api = ds.getLatestValidProcessingInformation(this);
        ExecutionContext detailedProcessingInfo = api != null ? api.getDetailedProcessingInfo() : null;
        return detailedProcessingInfo != null && detailedProcessingInfo.hasRunningJobs();
    }

    public Map<DataSet, Boolean> checkStatus(List<String> pids) {
        return checkStatus(pids, false);
    }

    public Map<DataSet, Boolean> checkStatus(List<String> pids, boolean suppressInfo) {
        List<DataSet> dataSets = getRuntimeService().loadDatasetsWithFilter(this, pids, suppressInfo);
        Map<DataSet, Boolean> results = new LinkedHashMap<>();
        dataSets.parallelStream().forEach(ds -> {
            boolean result = checkStatusForDataset(ds);
            synchronized (results) {
                results.put(ds, result);
            }
        });
        List<DataSet> sortedKeys = new LinkedList<>(results.keySet());
        sortedKeys.sort((ds1, ds2) -> ds1.getId().compareTo(ds2.getId()));
        Map<DataSet, Boolean> sortedMap = new LinkedHashMap<>();
        for (DataSet ds : sortedKeys) {
            sortedMap.put(ds, results.get(ds));
        }
        return sortedMap;
    }

    /**
     * Convenience accessor to runtimeService
     *
     * @return
     */
    public List<DataSet> getListOfPossibleDataSets() {
        return runtimeService.getListOfPossibleDataSets(this);
    }

    @Deprecated
    public List<DataSet> loadDatasetsWithFilter(List<String> pidFilters) {
        return loadDatasetsWithFilter(pidFilters, false);
    }

    @Deprecated()
    public List<DataSet> loadDatasetsWithFilter(List<String> pidFilters, boolean suppressInfo) {
        return runtimeService.loadDatasetsWithFilter(this, pidFilters, suppressInfo);
    }

    @Deprecated
    public List<DataSet> selectDatasetsFromPattern(List<String> pidFilters, List<DataSet> listOfDataSets, boolean suppressInfo) {
        return runtimeService.selectDatasetsFromPattern(this, pidFilters, listOfDataSets, suppressInfo);
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
    public ExecutionContext runDeferredContext(String pidFilter, final ExecutionContextLevel executionContextLevel) {
        long creationCheckPoint = System.nanoTime();
        for (DataSet ds : getRuntimeService().getListOfPossibleDataSets(this)) {
            if (!new WildcardFileFilter(pidFilter).accept(ds.getInputFolderForAnalysis(this)))
                continue;
            final ExecutionContext context = new ExecutionContext(FileSystemAccessProvider.getInstance().callWhoAmI(), this, ds, executionContextLevel, ds.getOutputFolderForAnalysis(this), ds.getInputFolderForAnalysis(this), null, creationCheckPoint);
            runDeferredContext(context);
            return context;
        }
        return null;
    }

    /**
     */
    public ExecutionContext rerunDeferredContext(ExecutionContext oldContext, final ExecutionContextLevel executionContextLevel, long creationCheckPoint, boolean test) {
        ExecutionContext context = new ExecutionContext(oldContext.getExecutingUser(), oldContext.getAnalysis(), oldContext.getDataSet(), test ? ExecutionContextLevel.TESTRERUN : ExecutionContextLevel.RERUN, oldContext.getOutputDirectory(), oldContext.getInputDirectory(), null, creationCheckPoint);
        context.getAllFilesInRun().addAll(oldContext.getAllFilesInRun());
        runDeferredContext(context);
        return context;
    }

    /**
     * Runs the passed execution context in a separate thread. The context level is taken from the passed object.
     *
     * @param ec The execution context which will be context in a separate thread.
     */
    public void runDeferredContext(final ExecutionContext ec) {
//        ThreadGroup
        Thread t = new Thread(() -> {
            executeRun(ec);
        });
        t.setName(String.format("Deferred execution context execution for pid %s", ec.getDataSet().getId()));
        t.start();
    }

    protected void executeRun(ExecutionContext context) {
        executeRun(context, false);
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
        logger.rare("" + context.getExecutionContextLevel());
        boolean isExecutable;
        String datasetID = context.getDataSet().getId();
        Exception eCopy = null;
        try {
            boolean contextRightsSettings = ExecutionService.getInstance().checkAccessRightsSettings(context);
            boolean contextPermissions = ExecutionService.getInstance().checkContextDirectoriesAndFiles(context);
            boolean contextExecutability = context.checkExecutability();
            boolean configurarionValidity = Roddy.isStrictModeEnabled() ? !getConfiguration().hasErrors() : true;
            isExecutable = contextRightsSettings && contextPermissions && contextExecutability && configurarionValidity;

            if (!isExecutable) {
                StringBuilder message = new StringBuilder("The workflow does not seem to be executable for dataset " + datasetID);
                if (!contextRightsSettings) message.append("\n\tContext access rights settings could not be validated.");
                if (!contextPermissions) message.append("\n\tContext permissions could not be validated.");
                if (!contextExecutability) message.append("\n\tContext and workflow is not considered executable.");
                if (!configurarionValidity) message.append("\n\tContext configuration has errors.");
                logger.severe(message.toString());
            } else {
                try {
                    ExecutionService.getInstance().writeFilesForExecution(context);
                    boolean execute = true;
                    if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs) { // Only do these checks, if we are not in query mode!
                        boolean preparedFilesAreValid = ExecutionService.getInstance().checkFilesPreparedForExecution(context);
                        boolean copiedAnalysisToolsAreExecutable = ExecutionService.getInstance().checkCopiedAnalysisTools(context);
                        execute &= preparedFilesAreValid && copiedAnalysisToolsAreExecutable;
                        if (!execute) {
                            StringBuilder message = new StringBuilder("There were errors after preparing the workflow run for dataset " + datasetID);
                            if (!preparedFilesAreValid) message.append("\n\tSome files could not be written. Workflow will not execute.");
                            if (!copiedAnalysisToolsAreExecutable) message.append("\n\tSome declared tools are not executable. Workflow will not execute.");
                            logger.severe(message.toString());
                        }
                    }

                    // Finally, if execution is allowed, run it and start the submitted jobs (if hold jobs is enabled)
                    if (execute) {
                        context.execute();
                        finallyStartJobsOfContext(context);
                    }
                } catch (Exception ex) {
                    // (Maybe) abort jobs in strict mode
                    maybeAbortStartedJobsOfContext(context);
                } finally {

                    if (context.getExecutionContextLevel() == ExecutionContextLevel.QUERY_STATUS) { //Clean up
                        //Query file validity of all files
                        FileSystemAccessProvider.getInstance().validateAllFilesInContext(context);
                    } else {

                        //First, check if there were any executed jobs. If not, we can safely delete the the context directory.
                        if (context.getStartedJobs().size() == 0) {
                            logger.postAlwaysInfo("There were no started jobs, the execution directory will be removed.");
                            if (context.getExecutionDirectory().getName().contains(ConfigurationConstants.RODDY_EXEC_DIR_PREFIX))
                                FileSystemAccessProvider.getInstance().removeDirectory(context.getExecutionDirectory());
                            else {
                                throw new RuntimeException("A wrong path would be deleted: " + context.getExecutionDirectory().getAbsolutePath());
                            }
                        } else {
                            ExecutionService.getInstance().writeAdditionalFilesAfterExecution(context);
                        }
                    }
                }
            }
        } catch (Exception e) {
            eCopy = e;
            context.addErrorEntry(ExecutionContextError.EXECUTION_UNCATCHEDERROR.expand(e));

        } finally {
            if (eCopy != null) {
                logger.always("An exception occurred: '" + eCopy.getLocalizedMessage() + "'");
                // This error should always be visible fully. We have a lot of known errors, which are properly catched. So unknown things should be visible.
                logger.always(RoddyIOHelperMethods.getStackTraceAsString(eCopy));
//                if (logger.isVerbosityMedium()) {
//                    logger.log(Level.SEVERE, eCopy.toString());
//                } else {
//                    logger.always("Set verbosity option -v or --vv to see the stack trace. Or look in to the log files in ~/.roddy/logs.");
//                }
            }

            // Look up errors when jobs are executed directly and when there were any started jobs.
            if (context.getStartedJobs().size() > 0) {
                for (Job job : context.getExecutedJobs()) {
                    if (job.getJobState() == JobState.FAILED)
                        context.addErrorEntry(ExecutionContextError.EXECUTION_JOBFAILED.expand("A job execution failed "));
                }
            }

            // It is very nice now, that a lot of error messages will be printen. But how about colours?
            // Normally the CLI client takes care of it.

            // Print out configuration errors (for context configuration! Not only for analysis)
            // Don't know, if this is the right place.
            if (context.getConfiguration().hasErrors()) {
                StringBuilder messages = new StringBuilder();
                messages.append("There were configuration errors for dataset " + datasetID);
                for (ConfigurationLoadError configurationLoadError : context.getConfiguration().getListOfLoadErrors()) {
                    messages.append(configurationLoadError.toString());
                }
                logger.always(messages.toString());
            }

            // Print out context errors.
            // Only print them out if !QUERY_STATUS and the runmode is testrun or testrerun.
            if (context.getErrors().size() > 0 && (!preventLoggingOnQueryStatus || (context.getExecutionContextLevel() != ExecutionContextLevel.QUERY_STATUS))) {
                StringBuilder messages = new StringBuilder();
                boolean warningsOnly = true;

                for (ExecutionContextError executionContextError : context.getErrors()) {
                    if (executionContextError.getErrorLevel().intValue() > Level.WARNING.intValue())
                        warningsOnly = false;
                }
                if (warningsOnly)
                    messages.append("\nThere were warnings for the execution context for dataset " + datasetID);
                else
                    messages.append("\nThere were errors for the execution context for dataset " + datasetID);
                for (ExecutionContextError executionContextError : context.getErrors()) {
                    messages.append("\n\t* ").append(executionContextError.toString());
                }
                logger.postAlwaysInfo(messages.toString());
            }

        }
    }

    /**
     * Will start all the jobs in the context.
     *
     * @param context
     */
    private void finallyStartJobsOfContext(ExecutionContext context) {
        Roddy.getJobManager().startHeldJobs(context.getExecutedJobs());
    }

    /**
     * Checks, whether strict mode AND rollback toggles are enabled and
     * try to abort jobs by calling the job manager.
     * Occurring exceptions are catched so following code can be executed properly.
     *
     * @param context
     */
    private void maybeAbortStartedJobsOfContext(ExecutionContext context) {
        if (Roddy.isStrictModeEnabled() && context.getFeatureToggleStatus(AvailableFeatureToggles.RollbackOnWorkflowError)) {
            try {
                logger.severe("A workflow error occurred, try to rollback / abort submitted jobs.");
                Roddy.getJobManager().queryJobAbortion(context.jobsForProcess);
            } catch (Exception ex) {
                logger.severe("Could not successfully abort jobs.", ex);
            }
        } else {
            logger.severe("A workflow error occurred. However, strict mode is disabled and/or RollbackOnWorkflowError is disabled and therefore, all submitted jobs will be left running." +
                    "\n\tYou might consider to enable Roddy strict mode by setting the feature toggles 'StrictMode' and 'RollbackOnWorkflowError'.");
        }
    }


    /**
     * Calls a cleanup script and / or a workflows cleanup method to cleanup the directories of a workflow.
     * If you call the cleanup script, a new execution context log directory will be created for this purpose. This directory will not be created if
     * the workflows cleanup method is called!.
     *
     * @param pidList
     */
    public void cleanup(List<String> pidList) {
        if (!((AnalysisConfiguration) getConfiguration()).hasCleanupScript() && !getWorkflow().hasCleanupMethod())
            logger.postAlwaysInfo("There is neither a configured cleanup script or a native workflow cleanup method available for this analysis.");
        List<DataSet> dataSets = getRuntimeService().loadDatasetsWithFilter(this, pidList, true);
        for (DataSet ds : dataSets) {
            // Call a custom cleanup shell script.
            if (((AnalysisConfiguration) getConfiguration()).hasCleanupScript()) {
                //TODO Think hard if this could be generified and simplified! This is also used in other places in a similar way right?
                ExecutionContext context = new ExecutionContext(FileSystemAccessProvider.getInstance().callWhoAmI(), this, ds, ExecutionContextLevel.CLEANUP, ds.getOutputFolderForAnalysis(this), ds.getInputFolderForAnalysis(this), null);
                Job cleanupJob = new Job(context, "cleanup", ((AnalysisConfiguration) getConfiguration()).getCleanupScript(), null);
//                Command cleanupCmd = Roddy.getJobManager().createCommand(cleanupJob, cleanupJob.getToolPath(), new LinkedList<>());
                try {
                    ExecutionService.getInstance().writeFilesForExecution(context);
                    cleanupJob.run();
                } catch (Exception ex) {
                    // Philip: We don't want to see any cleanup errors?
                } finally {
                    ExecutionService.getInstance().writeAdditionalFilesAfterExecution(context);
                }
            }

            // Call the workflows cleanup java method.
            if (getWorkflow().hasCleanupMethod())
                getWorkflow().cleanup(ds);
        }
    }

    public File getReadmeFile() {
        return getConfiguration().getInformationalConfigurationContent().getReadmeFile();
    }
}
