/*
 * Copyright (c) 2023 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import com.google.common.base.Preconditions
import de.dkfz.roddy.FeatureToggles
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.converters.BashConverter
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.Command as BECommand
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectCommand
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutionJobManager
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.tools.EscapableString
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.Tuple2
import de.dkfz.roddy.tools.Utils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import static de.dkfz.roddy.Constants.*
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_JOBID
import static de.dkfz.roddy.config.ConfigurationConstants.DEBUG_WRAP_IN_SCRIPT
import static de.dkfz.roddy.config.FilenamePattern.PLACEHOLDER_JOBPARAMETER
import static de.dkfz.roddy.execution.jobs.JobConstants.PRM_TOOL_ID
import static de.dkfz.roddy.tools.EscapableString.Shortcuts.*

@CompileStatic
class Job extends BEJob<BEJob, BEJobResult> {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(BEJob.class.simpleName)

    public static final String PARM_WRAPPED_SCRIPT = "WRAPPED_SCRIPT"
    public static final String PARM_WRAPPED_SCRIPT_MD5 = "WRAPPED_SCRIPT_MD5"

    /**
     * The current context.
     */
    public final ExecutionContext context

    /**
     * The tool command you want to call. This is not necessarily the same as in the execution context, e.g.
     * if the job command should be wrapped into a container call.
     */
    @NotNull
    private final AnyToolCommand toolCommand

    /**
     * Keeps a list of all unchanged, initial parameters, including default job parameters.
     */
    protected final Map<String, Object> allRawInputParameters

    /**
     * Keeps a list of all parameters after conversion and before removing them from the final parameter list
     * (See keepOnlyEssentialParameters). The value is used for testrun and testrerun output.
     */
    final Map<String, EscapableString> reportedParameters = [:]

    /**
     * Provide a list of files if you want to generate job dependencies.
     */
    public transient final List<BaseFile> parentFiles

    private Map<String, Object> initialInputParameters = [:]

    /**
     * Command object of last execution.
     */
    protected transient BECommand lastCommand

    /**
     * The list contains files which should be validated for a job restart.
     */
    public final List<BaseFile> filesToVerify

    /**
     * This value should be set after submitting the job. The null serves as a protective
     * default value.
     */
    protected Boolean wasSubmittedOnHold = null

    /**
     * TODO No apparent usage of this constructor. Leave it in only for backwards-compatibility.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        List<String> arrayIndices,
        Map<String, Object> parameters,
        List<BaseFile> parentFiles) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters, parentFiles)
    }

    /**
     * TODO Used in Alignment and COWorkflows plugins. Replace by main constructor.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        Map<String, Object> parameters,
        List<BaseFile> parentFiles,
        List<BaseFile> filesToVerify) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters, parentFiles, filesToVerify)
    }

    /**
     * TODO Used in Alignment workflow (Common & Samtools). Replace by main constructor.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        Map<String, Object> parameters,
        List<BaseFile> parentFiles) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters, parentFiles)
    }

    /**
     * TODO No apparent usage of this constructor. Leave it in only for backwards-compatibility.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        List<String> arrayIndices,
        List<BaseFile> filesToVerify,
        Map<String, Object> parameters) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters, [], filesToVerify)
    }

    /**
     * TODO No apparent usage of this constructor. Leave it in only for backwards-compatibility.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        List<BaseFile> filesToVerify,
        String toolID,
        Map<String, Object> parameters) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters, [], filesToVerify)
    }

    /**
     * TODO No apparent usage of this constructor. Leave it in only for backwards-compatibility.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        List<String> arrayIndices,
        Map<String, Object> parameters) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters)
    }

    /**
     * TODO No apparent usage of this constructor. Leave it in only for backwards-compatibility.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        String toolID,
        Map<String, Object> parameters) {
        this(context, jobName, context.getToolCommand(toolID),
                parameters)
    }

    /**
     * TODO: Used only in ACEseq plugin. Replace single called from ACEseqMethods by main
     *       constructor call.
     */
    @Deprecated
    Job(ExecutionContext context,
        String jobName,
        @NotNull String toolID,
        @Nullable List<String> arrayIndices,
        Map<String, Object> inputParameters,
        List<BaseFile> parentFiles,
        List<BaseFile> filesToVerify) {
        this(context, jobName, context.getToolCommand(toolID),
                inputParameters, parentFiles, filesToVerify)
    }

    /**
     * Main constructor.
     *
     * @param context           the execution context that carries Roddy's global state
     * @param jobName           the name of the job on the cluster (if it will be run on the cluster)
     * @param toolCommand       a CommandI object. Usually this should be `Executable(toolId)` or `Code` object..
     *                          Job will executed this command with a defined environment (PARAMETER_FILE) and
     *                          possibly nested in a container runtime call (singularity exec).
     * @param inputParameters   ?
     * @param parentFiles       ?
     * @param filesToVerify     ?
     */
    Job(@NotNull ExecutionContext context,
        @NotNull String jobName,
        @NotNull AnyToolCommand toolCommand,
        Map<String, Object> inputParameters = [:],
        List<BaseFile> parentFiles = [],
        List<BaseFile> filesToVerify = []) {
        super(BEJobID.newUnknown
              , Roddy.jobManager
              , u(jobName)
              , EffectiveToolCommandBuilder.
                from(context).
                build(toolCommand).
                map { ToolCommand it -> it.command }.
                orElse(null)
              , context.getResourceSetFromConfiguration(toolCommand.toolId)
              , []
              , [:] as Map<String, EscapableString>
              , JobLog.toOneFile(new File(context.loggingDirectory, jobName + ".o{JOB_ID}"))
              , null as File
              , context.accountingName.map { e(it) }.orElse(null))
        Preconditions.checkArgument(context != null)
        Preconditions.checkArgument(jobName != null)
        Preconditions.checkArgument(toolCommand != null)
        this.addParentJobs(reconcileParentJobInformation(
                collectParentJobsFromFiles(parentFiles),
                collectJobIDsFromFiles(parentFiles),
                jobManager))
        this.context = context
        this.toolCommand = EffectiveToolCommandBuilder.
                from(context).
                build(toolCommand).
                orElse(null)

        // Set and validate the job parameters
        Map<String, Object> defaultParameters = context.getDefaultJobParameters(toolCommand.toolId)

        if (inputParameters != null)
            defaultParameters.putAll(inputParameters)
        this.allRawInputParameters = defaultParameters

        for (String k : defaultParameters.keySet()) {
            if (this.parameters.containsKey(k)) continue
            Object _v = defaultParameters[k]
            if (_v == null) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.
                        expand("The parameter " + k + " has no valid value and will be set to ${NO_VALUE}."))
                this.parameters[k] = e(NO_VALUE)
            } else {
                EscapableString newParameters = u(parameterObjectToString(k, _v))
                this.parameters.put(k, newParameters)
            }
        }

        parameters.putAll(convertResourceSetToParameters())
        reportedParameters.putAll(parameters)

        // Used by the DirectSynchronousExecutionJobManager and job.
        this.parameters[PARM_JOBCREATIONCOUNTER] = u(jobCreationCounter.toString())

        // Needed by the wrapInScript.sh defined in the DefaultPlugin.
        this.parameters[PARM_WRAPPED_SCRIPT] =
                e(context.configuration.getProcessingToolPath(context, toolCommand.toolId).absolutePath)
        this.parameters[PARM_WRAPPED_SCRIPT_MD5] =
                e(context.getToolMd5(toolCommand.toolId))


        if (inputParameters != null)
            initialInputParameters.putAll(inputParameters)

        this.parentFiles = parentFiles ?: new LinkedList<BaseFile>()

        // These are debugging methods to get at least some warnings about null values that should not occur.
        // The only place I could find, where a BaseFile in the list may be null is in the indexFile of a BamFile.
        if (filesToVerify == null) {
            logger.warning("filesToVerify parameter must not be null. Please report this! Stacktrace:\n" +
                           Utils.stackTrace())
        } else if (filesToVerify.any { it == null }) {
            logger.warning("filesToVerify parameter must not contain null entries. Please report this! Stacktrace\n" +
                           Utils.stackTrace())
        }

        this.filesToVerify = filesToVerify ?: new LinkedList<BaseFile>()
        this.context.addExecutedJob(this)
    }

    private static List<BEJobID> jobs2jobIDs(List<BEJob> jobs) {
        if (null == jobs) {
            return new LinkedList<BEJobID>()
        } else {
            return jobs.collect { it.runResult.jobID }
        }
    }

    private static List<BEJob> reconcileParentJobInformation(List<BEJob> parentJobs,
                                                             List<BEJobID> parentJobIDs,
                                                             BatchEuphoriaJobManager jobManager) {
        List<BEJob> pJobs
        if ((null != parentJobIDs && !parentJobIDs.isEmpty()) &&
                (null != parentJobs && !parentJobs.isEmpty())) {
            def validJobs = jobsWithUniqueValidJobId(parentJobs)
            def validIds = uniqueValidJobIDs(parentJobIDs).collect { it.toString() }
            def idsOfValidJobs = jobs2jobIDs(validJobs).collect { it.toString() }
            if (validIds != idsOfValidJobs) {
                throw new RuntimeException(
                        "parentJobBEJob needs to be called with one of parentJobs, parentJobIDs, or " +
                                "parentJobsIDs and *corresponding* parentJobs.")
            }
            pJobs = validJobs
        } else if (null == parentJobIDs && null == parentJobs) {
            pJobs = new LinkedList<BEJob>()
        } else if (null != parentJobs) {
            pJobs = jobsWithUniqueValidJobId(parentJobs)
        } else {
            pJobs = uniqueValidJobIDs(parentJobIDs).collect { new BEJob(it, jobManager) }
        }
        return pJobs
    }

    String getToolMD5() throws ConfigurationError {
        context.getToolMd5(toolID)
    }


    static private List<BEJob> collectParentJobsFromFiles(List<BaseFile> parentFiles) {
        if (!parentFiles) return []
        List<BEJob> parentJobs = parentFiles.collect {
            BaseFile bf -> bf?.creatingJobsResult?.job
        }.findAll {
            BEJob job -> job
        } as List<BEJob>
        return parentJobs
    }

    private static List<BEJobID> collectJobIDsFromFiles(List<BaseFile> parentFiles) {
        List<BEJobID> dIDs = []
        if (parentFiles != null) {
            for (BaseFile bf : parentFiles) {
                if (bf.isSourceFile() && bf.creatingJobsResult == null) continue
                try {
                    BEJobID jobid = bf.creatingJobsResult?.jobID
                    if (jobid?.isValidID()) {
                        dIDs << jobid
                    }
                } catch (Exception ex) {
                    logger.severe("No job dependency id could be retrieved from BaseFile: " + bf)
                    logger.postSometimesInfo(ex.message)
                    logger.postSometimesInfo(RoddyIOHelperMethods.getStackTraceAsString(ex))
                }
            }
        }
        return dIDs
    }

    private static String fileToParameterString(File file) {
        return file.absolutePath
    }

    private static Integer generateHashCode(String template, List<BaseFile> parentFiles) {
        if (parentFiles) {
            parentFiles.each {
                BaseFile p ->
                    if (!p instanceof BaseFile) return

                    BaseFile _bf = (BaseFile) p
                    template += _bf.absolutePath.toString()

            }
        }
        return Math.abs(template.hashCode()) as Integer
    }

    /** The auto filename is generated from the raw job parameters as background information.
     *  If feature toggle FailOnAutoFilename is set, this method will fail with a RuntimeException.
     *
     *  The auto file name will be composed from the jobName, the key value, a hash code generated
     *  from these information and the list of parent files and the .auto suffix.
     *
     * @param parameterName
     * @param baseFile
     * @return auto filename as path string
     */
    private String generateAutoFilename(String parameterName, BaseFile baseFile) {
        int slotPosition = allRawInputParameters.keySet().asList().indexOf(parameterName)

        if (Roddy.isStrictModeEnabled() && context.getFeatureToggleStatus(FeatureToggles.FailOnAutoFilenames))
            throw new ConfigurationError("Auto filenames are forbidden when strict mode is active.", context.configuration)
        else
            context.addError(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("An auto filename will be used for ${jobName}:${slotPosition} / ${baseFile.class.name}"))

        Integer hashCode = generateHashCode(jobName + parameterName + slotPosition, parentFiles)

        File autoPath = new File(context.getOutputDirectory(), 
                [jobName, jobCreationCounter, parameterName, hashCode, slotPosition].join("_") + AUTO_FILENAME_SUFFIX)
        baseFile.path = autoPath
        baseFile.setAsTemporaryFile()
        return autoPath.absolutePath
    }

    /** Convert a BaseFile object into a path string. First try to replace all variables using the raw job input parameters as background variables.
     *  If that fails, generate an auto file name.
     *
     * @param parameterName
     * @param baseFile
     * @return path to basefile
     */
    private String baseFileToParameterString(String parameterName, BaseFile baseFile) {
        String newPath = replaceParametersInFilePath(baseFile, allRawInputParameters)
        //Explicitly query newPath for a proper value!
        if (newPath == null) {
            newPath = generateAutoFilename(parameterName, baseFile)
        }
        return newPath
    }


    private String collectionToParameterString(String parameterName, Collection collection) {
        //TODO This is not the best way to do this, think of a better one which is more generic.
        List<Object> convertedParameters = new LinkedList<>()
        for (Object o : collection) {
            if (o instanceof BaseFile) {
                if (((BaseFile) o).path != null)
                    convertedParameters.add(baseFileToParameterString(parameterName, o as BaseFile))
            } else
                convertedParameters.add(o.toString())
        }
        return BashConverter.convertListToBashArrayString(convertedParameters,
            context.getFeatureToggleStatus(FeatureToggles.AutoQuoteBashArrayVariables))
    }


    private String parameterObjectToString(String parameterName, Object value) {
        if (value instanceof File) {
            return fileToParameterString(value as File)
        } else if (value instanceof BaseFile) {
            return baseFileToParameterString(parameterName, value as BaseFile)
        } else if (value instanceof FileGroup) {
            return collectionToParameterString(parameterName, (value as FileGroup).filesInGroup)
        } else if (value instanceof Collection) {
            return collectionToParameterString(parameterName, value as Collection)
        } else {
            return value.toString()
        }
    }

    private Map<String, EscapableString> convertResourceSetToParameters() {
        def rs = resourceSet
        Map<String, EscapableString> rsParameters = [:]
        if (rs == null) return rsParameters

        if (rs.memSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_MEM"] = u(rs.mem.toString())
        if (rs.coresSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_CORES"] = u(rs.cores.toString())
        if (rs.nodesSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_NODES"] = u(rs.nodes.toString())
        if (rs.queueSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_QUEUE"] = u(rs.queue.toString())
        if (rs.walltimeSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_WALLTIME"] = e(rs.walltime.toString())
        if (rs.additionalNodeFlagSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_NODEFLAGS"] = e(rs.additionalNodeFlag.toString())
        if (rs.storageSet) rsParameters["RODDY_JOBRESOURCE_REQUEST_STORAGE"] = u(rs.storage.toString())
        return rsParameters
    }

    static String createJobName(BaseFile bf, String toolName, boolean reduceLevel) {
        return createJobName(bf, toolName, reduceLevel, null)
    }

    static String createJobName(BaseFile bf,
                                String toolName,
                                boolean reduceLevel,
                                List<BaseFile> inputFilesForSizeCalculation) {
        ExecutionContext rp = bf.executionContext
        String runtime = rp.timestampString
        StringBuilder sb = new StringBuilder()
        sb.append("r").append(runtime).append("_").append(bf.pid).append("_").append(toolName)
        return sb.toString()
    }

    static String createJobName(ExecutionContext rp, String postfix) {
        String runtime = rp.timestampString
        StringBuilder sb = new StringBuilder()
        sb.append("r").append(runtime).append("_").append(rp.getDataSet().getId()).append("_").append(postfix)
        return sb.toString()
    }

    static String createJobName(FileGroup fg,
                                String toolName,
                                boolean reduceLevel,
                                List<BaseFile> inputFilesForSizeCalculation) {
        BaseFile bf = (BaseFile) fg.filesInGroup.get(0)
        return createJobName(bf, toolName, reduceLevel, inputFilesForSizeCalculation)
    }

    static File replaceParametersInFilePath(BaseFile bf, Map<String, Object> parameters) {
        //TODO: It can occur that the regeneration of the filename is not valid!

        // Replace $_JOBPARAMETER items in path with proper values.
        File path = bf.path
        if (path == null) {
            return null
        }

        String absolutePath = path.absolutePath
        List<FilenamePatternHelper.Command> commands = 
                FilenamePatternHelper.extractCommands(bf.executionContext, PLACEHOLDER_JOBPARAMETER, absolutePath)
        for (FilenamePatternHelper.Command command : commands) {
            FilenamePatternHelper.CommandAttribute name = command.attributes.get("name")
            if (name != null) {
                String val = parameters[name.value]
                if (val == null) {
                    val = NO_VALUE
                    bf.executionContext.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.
                            expand("A value named " +
                                    name.value +
                                    " cannot be found in the jobs parameter list for a file of ${bf.class.name}. " +
                                    "The value is set to <NO_VALUE>"))
                }
                absolutePath = absolutePath.replace(command.fullString, val)
            }
            path = new File(absolutePath)
            bf.path = path
        }
        path
    }

    private static String jobStateInfoLine(String jobId, String code, String millis, String toolID) {
        return String.format("%s:%s:%s:%s", jobId, code, millis, toolID)
    }

    /**
     * Stores a new job jobState info to an execution context's job jobState log file.
     *
     * @param job
     */
    @CompileDynamic
    private void appendToJobStateLogfile(BatchEuphoriaJobManager jobManager,
                                         ExecutionContext executionContext,
                                         BEJobResult res,
                                         OutputStream out = null) {
        if (!jobManager.holdJobsEnabled && !jobManager instanceof DirectSynchronousExecutionJobManager) {
            throw new RuntimeException("Appending to JobManager ${ConfigurationConstants.RODDY_JOBSTATE_LOGFILE} " +
                    "not supported when JobManager does not submit on hold: '${jobManager.class.name}")
        }
        if (res.successful) {
            def job = res.beCommand.job
            String jobInfoLine
            String millis = "" + System.currentTimeMillis()
            millis = millis.substring(0, millis.length() - 3)
            String jobId = job.jobID
            if (jobId != null) {
                if (job.jobState == JobState.UNSTARTED)
                    jobInfoLine = jobStateInfoLine(jobId, JobState.UNSTARTED.toString(), millis, toolID)
                else if (job.jobState == JobState.ABORTED)
                    jobInfoLine = jobStateInfoLine(jobId, JobState.ABORTED.toString(), millis, toolID)
                // TODO https://odcf-gitlab.dkfz.de/ilp/odcf/components/BatchEuphoria/-/issues/117
                //      COMPLETED_SUCCESSFUL is set by BE.ExecutionService, when the
                //      execution result is successful, i.e. the submission (by `qsub`) -- not when
                //      the job finished successfully on the cluster!
                else if (job.jobState == JobState.FAILED)
                    jobInfoLine = jobStateInfoLine(jobId, "" + res.executionResult.exitCode, millis, toolID)
                else
                    jobInfoLine = null
            } else {
                logger.postSometimesInfo("Did not store info for job " + job.jobName + ", job id was null.")
                jobInfoLine = null
            }
            if (jobInfoLine != null)
                FileSystemAccessProvider.instance.
                        appendLineToFile(true,
                                executionContext.runtimeService.getJobStateLogFile(executionContext), 
                                jobInfoLine,
                                false)
        }
    }

    /**
     * Stores a new job jobState info to an execution contexts job jobState log file.
     *
     * @param job
     */
    @CompileDynamic
    private void appendToJobStateLogfile(DirectSynchronousExecutionJobManager jobManager,
                                         ExecutionContext executionContext,
                                         BEJobResult res,
                                         OutputStream outputStream = null) {
//        if (res.command.isBlockingCommand()) {
//            assert (null != outputStream)
//            File logFile = (res.command.getTag(COMMAND_TAG_EXECUTION_CONTEXT)
//                              as ExecutionContext).runtimeService.getLogFileForCommand(res.command)
//
//            // Use reflection to get access to the hidden path field :p The stream object does not natively give
//            // access to it and I do not want to create a new class just for this.
//            Field fieldOfFile = FileOutputStream.class.getDeclaredField("path")
//            fieldOfFile.setAccessible(true);
//            File tmpFile2 = new File((String) fieldOfFile.get(outputStream))
//
//            FileSystemAccessProvider.instance.moveFile(tmpFile2, logFile)
//        } else {
        if (res.successful) {
            String millis = "" + System.currentTimeMillis()
            millis = millis.substring(0, millis.length() - 3)
            String code = "255"
            if (res.job.jobState == JobState.SUBMITTED)
                code = "UNSTARTED" // N
            else if (res.job.jobState == JobState.ABORTED)
                code = "ABORTED" // A
            else if (res.job.jobState == JobState.COMPLETED_SUCCESSFUL)
                code = "SUCCESSFUL"  // C
            else if (res.job.jobState == JobState.FAILED)
                code = "FAILED" // E

            if (null != res.job.jobID) {
                // That is indeed funny here: on our cluster, the following line did not work without the
                // forced toString(), however on our local machine it always worked! Don't know why it worked
                // for PBS... Now we force-convert the parameters.
                String jobInfoLine = jobStateInfoLine("" + res.job.jobID, code, millis, toolID)
                FileSystemAccessProvider.instance.
                        appendLineToFile(true,
                                executionContext.runtimeService.getJobStateLogFile(executionContext),
                                jobInfoLine,
                                false)
            } else {
                logger.postSometimesInfo("Did not store info for job " + res.job.jobName + ", job id was null.")
            }

        }
//        }
    }

    /** Keep only the essential command line configurations for the job, due to argument length restrictions
     *  (e.g. -env in LSF).
     */
    void keepOnlyEssentialParameters() {
        Set<String> nonEssentialParameters = parameters.keySet().findAll { key ->
            ![PARAMETER_FILE, CONFIG_FILE, DEBUG_WRAP_IN_SCRIPT,
              APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT, PRM_TOOL_ID
            ].contains(key)
        }
        parameters.keySet().removeAll(nonEssentialParameters)
    }

    @NotNull BEJobResult run(boolean appendToJobStateLogfile = true) {
        if (runResult != null)
            throw new RuntimeException(ERR_MSG_ONLY_ONE_JOB_ALLOWED)

        ExecutionContextLevel contextLevel = context.executionContextLevel
        Configuration configuration = context.configuration

        StringBuilder dbgMessage = new StringBuilder()
        StringBuilder jobDetailsLine = new StringBuilder()

        // Remove duplicate job ids as PBS qsub cannot handle duplicate keys => job will hold forever as it
        // releases the dependency queue linearly.
        this.parameters[RODDY_PARENT_JOBS] = u(parameterObjectToString(RODDY_PARENT_JOBS, parentJobIDs.unique()*.id))
        this.parameters[PARAMETER_FILE] = u(parameterObjectToString(PARAMETER_FILE, parameterFile))
        // The CONFIG_FILE variable is set to the same value as the PARAMETER_FILE to keep older scripts working
        // with job-specific only environments.
        // TODO Deprecated. Remove this line in Roddy 4.0. No config file.
        this.parameters[CONFIG_FILE] = u(this.parameters[PARAMETER_FILE])

        boolean debugWrapInScript = false
        if (configuration.configurationValues.hasValue(DEBUG_WRAP_IN_SCRIPT)) {
            debugWrapInScript = configuration.configurationValues.getBoolean(DEBUG_WRAP_IN_SCRIPT)
        }
        this.parameters.put(DEBUG_WRAP_IN_SCRIPT, e(parameterObjectToString(DEBUG_WRAP_IN_SCRIPT, debugWrapInScript)))

        if (Roddy.applicationConfiguration.containsKey(APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT)) {
            this.parameters.put(APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT,
                    e(Roddy.applicationConfiguration.getOrSetApplicationProperty(APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT,
                            "")))
        }

        // See if the job should be executed
        boolean runJob
        if (contextLevel == ExecutionContextLevel.RUN || contextLevel == ExecutionContextLevel.CLEANUP) {
            runJob = true
            jobDetailsLine << "  Running job " + jobName
        } else if (contextLevel == ExecutionContextLevel.RERUN || contextLevel == ExecutionContextLevel.TESTRERUN) {
            runJob = checkIfJobShouldRerun(dbgMessage)
            jobDetailsLine << "  Rerun job " + jobName
        }

        // Execute the job or create a dummy command.
        if (runJob) {
            storeJobConfigurationFile(createJobConfiguration())
            keepOnlyEssentialParameters()
            runResult = jobManager.submitJob(this)
            wasSubmittedOnHold = jobManager.holdJobsEnabled
            if (appendToJobStateLogfile)
                this.appendToJobStateLogfile(jobManager, executionContext, runResult, null)
            BECommand cmd = runResult.beCommand

            // If we have os process id attached, we'll need some space, so pad the output.
            jobDetailsLine << " => " + cmd.job.jobID.toString().padRight(10)

            // For direct execution it can be very helpful to know the id of the started process.
            // Sometimes, sub processes remain and need to be killed.
            if (cmd instanceof DirectCommand)
                jobDetailsLine << " [OS Process ID: ${runResult.executionResult.processID}]"

            System.out.println(jobDetailsLine.toString())
            if (!cmd.jobID) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_SUBMISSION_FAILURE.
                        expand("Please check your submission command manually.\n\t  Is your access group " +
                                "set properly? [${context.getAnalysis().getUsergroup()}]\n\t  Can the submission " +
                                "binary handle your binary?\n\t  Is your submission system offline?"))
                logger.postSometimesInfo("Command: ${runResult.executionResult.toStatusLine()}")
                if (Roddy.getFeatureToggleValue(FeatureToggles.BreakSubmissionOnError)) {
                    context.abortJobSubmission()
                }
            }
            lastCommand = cmd

            // For auto filenames. Get the job id and push propagate it to all filenames.
            if (runResult.jobID?.shortID) {
                allRawInputParameters.each { String k, Object o ->
                    BaseFile bf = o instanceof BaseFile ? (BaseFile) o : null
                    if (!bf) return

                    String absolutePath = bf.path.absolutePath
                    if (absolutePath.contains(CVALUE_PLACEHOLDER_RODDY_JOBID)) {
                        bf.setPath(new File(absolutePath.replace(CVALUE_PLACEHOLDER_RODDY_JOBID, runResult.jobID.shortID)))
                    }
                }
            }

        } else {
            dbgMessage << "\tdummy job created." + ENV_LINESEPARATOR
            resetJobID(new BEFakeJobID(BEFakeJobID.FakeJobReason.NOT_EXECUTED))
            runResult = new BEJobResult(
                    null as SubmissionCommand,
                    this,
                    null,
                    parameters,
                    parentFiles.collect { it.creatingJobsResult?.getJob() }.findAll { it })
            jobState = JobState.DUMMY
            wasSubmittedOnHold = false
        }

        return runResult
    }

    BECommand getLastCommand() {
        return lastCommand
    }

    /**
     * There are several reasons, why we need a job configuration.
     * The most important reason is, that we need to replicate Roddy's configuration mechanism with all
     * it's functionality for any target system (Bash so far). If we don't do it, we will suffer from:
     * - wrong values
     * - wrong resolved value dependencies
     * - mistakenly overriden values
     * There might be more, but these are the ones for now.
     * @return
     */
    Configuration createJobConfiguration() {
        Configuration jobConfiguration = new Configuration(null, executionContext.configuration)
        jobConfiguration.configurationValues.addAll(parameters.collect { String k, EscapableString v ->
            new ConfigurationValue(jobConfiguration, k, forBash(v))
        })
        return jobConfiguration
    }

    void storeJobConfigurationFile(Configuration cfg) {
        String configText = ConfigurationConverter.convertAutomatically(context, cfg)
        FileSystemAccessProvider.instance.writeTextFile(getParameterFile(), configText, context)
    }

    /**
     * Checks, if a job needs to be rerun.
     * @param dbgMessage
     * @return
     */
    private boolean checkIfJobShouldRerun(StringBuilder dbgMessage) {
        def isVerbosityHigh = logger.verbosityHigh
        String sep = ENV_LINESEPARATOR
        if (isVerbosityHigh) dbgMessage << "Rerunning job " + jobName

        // Check the parents of the new files to see if one of those is invalid for the current context! A file might
        // be validated during a dirty context...
        boolean parentFileIsDirty = false

        if (isVerbosityHigh) dbgMessage << sep << "\tchecking parent files for validity"
        for (BaseFile pf : parentFiles) {
            if (!pf.isFileValid()) {
                if (isVerbosityHigh)
                    dbgMessage << sep << "\tfile " << pf.path.getName() << " is dirty or does not exist." << sep
                parentFileIsDirty = true
            }
        }

        Integer knownFilesCnt = 0
        Boolean fileUnverified = false
        //Now check if the new created files are in the list of already existing files and if those files are valid.
        if (!parentFileIsDirty) {
            Tuple2 res = verifyFiles(dbgMessage)
            fileUnverified = res.x
            knownFilesCnt = res.y
        }

        boolean parentJobIsDirty = parentJobs.any { BEJob job -> job.isDirty }

        boolean knownFilesCountMismatch = knownFilesCnt != filesToVerify.size()

        boolean rerunIsNecessary = fileUnverified || parentFileIsDirty || parentJobIsDirty || knownFilesCountMismatch

        if (isVerbosityHigh && rerunIsNecessary) dbgMessage << "\tBEJob will rerun" << sep

        //If all files could be found rerun if necessary otherwise rerun it definitive.
        if (!rerunIsNecessary) {
            if (isVerbosityHigh) dbgMessage << "\tBEJob was verified and will not be rerun" << sep
            return false
        }

        //More detailed if then because of enhanced debug / breakpoint possibilities
        if (isVerbosityHigh) {
            if (parentJobIsDirty)
                dbgMessage << "\t* BEJob is set to rerun because a parent job is marked dirty" << sep
            if (knownFilesCountMismatch)
                dbgMessage << "\t* The number of existing files does not match with the number of files which should be created" << sep
            if (fileUnverified)
                dbgMessage << "\t* One or more files could not be verified" << sep
            if (parentFileIsDirty)
                dbgMessage << "\t* One or more of the jobs parent files could not be verified" << sep
        }
        isDirty = true
        return true
    }

    /** Wait for all file objects in the run to be prepared. This is necessary, because these file
     *  may be updated concurrently (e.g., via runParallel).
     *
     *  Note: The previous implementation waited for each files in filesToVerify and waited for
     *        each combination of filesToVerify and allFilesInRun. However, this was an all vs.
     *        all comparison, even waiting for files that are not related to each other.
     *
     *        This new implementation is conceptually simpler and should be sufficient. Just wait
     *        for all files objects to be ready and continue.
     */
    protected void waitForFilesInRun() {
        List<BaseFile> observedFiles = context.allFilesInRun
        Integer numAttempts = context.maxFileAppearanceAttempts
        Integer waitTime = context.fileAppearanceRetryWaitTimeMS

        Thread.sleep(100) // Initial short sleep to let other threads start working.
        for (int i = 0; i < numAttempts; i++) {
            Queue<BaseFile> filesToWaitFor = new LinkedList<BaseFile>()
            for (BaseFile obsFile : observedFiles) {
                if (obsFile == null) {
                    logger.severe("File object in ExecutionContext.allFilesToRun is null. " +
                                  "This should not happen. Please report this issue.")
                } else if (obsFile.path == null) {
                    filesToWaitFor.push(obsFile)
                }
            }
            if (!filesToWaitFor.empty) {
                try {
                    logger.severe("Taking a short nap because a file does not seem to be finished." +
                                  " Attempt ${i + 1}/$numAttempts")
                    logger.rare(filesToWaitFor.collect {
                        "Waiting for: " + it.toString()
                    }.join("\n"))
                    Thread.sleep(waitTime)
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted for ${filesToWaitFor.size()} files: " + e.message)
                    e.printStackTrace()
                }
            }
        }
    }

    /** For all files to verify (expected files), check if they exist. If they exist, check if they are valid.
     *
     * @param dbgMessage
     * @return A tuple where the first value indicates if one or more files could not be verified,
     *         and the second value indicates  how many files were known (found).
     */
    protected @NotNull Tuple2<Boolean, Integer> verifyFiles(@NotNull StringBuilder dbgMessage) {
        String sep = ENV_LINESEPARATOR
        Boolean fileUnverified = false
        Integer knownFilesCnt = 0
        boolean isVerbosityHigh = LoggerWrapper.isVerbosityHigh()

        if (isVerbosityHigh) dbgMessage << "\tverifying specified files" << sep

        waitForFilesInRun()
        List<BaseFile> observedFiles = context.allFilesInRun

        // TODO what about the case if no verifiable files where specified? Or if the know files count does not match
        for (BaseFile expFile : filesToVerify) {
            for (BaseFile obsFile : observedFiles) {
                if (expFile.absolutePath == obsFile.path.absolutePath) {
                    // "Verification" and "validation" are the same in Roddy.
                    if (!obsFile.fileValid) {
                        fileUnverified = true
                        if (isVerbosityHigh) {
                            dbgMessage << "\tfile ${obsFile.path.name} could not be verified!$sep"
                        }
                    }
                    knownFilesCnt++
                    break
                }
            }
        }
        return new Tuple2<Boolean, Integer>(fileUnverified, knownFilesCnt)
    }

    ExecutionContext getExecutionContext() {
        return context
    }

    @NotNull List<BaseFile> getParentFiles() {
        if (parentFiles != null)
            return new LinkedList<>(parentFiles)
        else
            return new LinkedList<>()
    }

    private File _logFile = null

    /**
     * Returns the path to an existing log file.
     * If no logfile exists this returns null.
     *
     * @return
     */
    synchronized File getLogFile() {
        if (_logFile == null)
            _logFile = this.executionContext.runtimeService.getLogFileForJob(this);
        return _logFile;
    }

    boolean hasLogFile() {
        if (jobState.isPlannedOrRunning())
            return false;
        if (_logFile == null)
            return this.executionContext.runtimeService.hasLogFileForJob(this);
        return true;
    }

    File getParameterFile() {
        return this.context.getParameterFilename(this)
    }

    @NotNull List<BaseFile> getFilesToVerify() {
        return new LinkedList<>(filesToVerify)
    }

    synchronized boolean areAllFilesValid() {
        boolean allValid = true
        for (BaseFile baseFile : filesToVerify) {
            allValid &= baseFile.isFileValid()
        }
        return allValid
    }

    @NotNull AnyToolCommand getToolCommand() {
        this.toolCommand
    }

    @NotNull String getToolID() {
        return this.toolCommand.toolId
    }

    @NotNull Boolean getWasSubmittedOnHold() {
        return wasSubmittedOnHold
    }

}
