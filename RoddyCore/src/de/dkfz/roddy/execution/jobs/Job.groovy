/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize
import de.dkfz.eilslabs.batcheuphoria.jobs.Command
import de.dkfz.eilslabs.batcheuphoria.jobs.DummyCommand
import de.dkfz.eilslabs.batcheuphoria.jobs.JobState
import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.FilenamePatternHelper
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import static de.dkfz.roddy.Constants.NO_VALUE
import static de.dkfz.roddy.config.FilenamePattern.PLACEHOLDER_JOBPARAMETER

@groovy.transform.CompileStatic
class Job extends de.dkfz.eilslabs.batcheuphoria.jobs.Job<Job> {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(Job.class.getSimpleName())

    public static final String TOOLID_WRAPIN_SCRIPT = "wrapinScript"
    public static final String PARM_WRAPPED_SCRIPT = "WRAPPED_SCRIPT"
    public static final String PARM_WRAPPED_SCRIPT_MD5 = "WRAPPED_SCRIPT_MD5"

    /**
     * The current context.
     */
    public final ExecutionContext context

    /**
     * The tool you want to call.
     */
    private final String toolID

    private boolean isDirty

    /**
     * Keeps a list of all unchanged, initial parameters, including default job parameters.
     */
    private final Map<String, Object> allRawInputParameters

    /**
     * Provide a list of files if you want to generate job dependencies.
     */
    public transient final List<BaseFile> parentFiles

    private Map<String, Object> initialInputParameters = [:]

    /**
     * The list contains files which should be validated for a job restart.
     */
    public final List<BaseFile> filesToVerify

    Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> parameters, List<BaseFile> parentFiles) {
        this(context, jobName, toolID, arrayIndices, parameters, parentFiles, null)
    }

    Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
        this(context, jobName, toolID, null, parameters, parentFiles, filesToVerify)
    }

    Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters, List<BaseFile> parentFiles) {
        this(context, jobName, toolID, null, parameters, parentFiles, null)
    }

    Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, List<BaseFile> filesToVerify, Map<String, Object> parameters) {
        this(context, jobName, toolID, arrayIndices, parameters, null, filesToVerify)
    }

    Job(ExecutionContext context, String jobName, List<BaseFile> filesToVerify, String toolID, Map<String, Object> parameters) {
        this(context, jobName, toolID, null, parameters, null, filesToVerify)
    }

    Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> parameters) {
        this(context, jobName, toolID, arrayIndices, parameters, null, null)
    }

    Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters) {
        this(context, jobName, toolID, null, parameters, null, null)
    }

//    /**
//     * This is for job implementations which do the writeConfigurationFile on their own.
//     */
//    protected Job(String jobName, ExecutionContext context, String toolID, Map<String, String> parameters, List<String> arrayIndices, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
//        super(jobName
//                , context.getConfiguration().getProcessingToolPath(context, toolID)
//                , toolID != null && toolID.trim().length() > 0 ? context.getConfiguration().getProcessingToolMD5(toolID): null
//                , arrayIndices
//                , parameters
//                , collectParentJobsFromFiles(parentFiles)
//                , collectDependencyIDsFromFiles(parentFiles)
//                , Roddy.getJobManager())
//        this.toolID = toolID
//        this.context = context
//        this.parentFiles = parentFiles
//        this.filesToVerify = filesToVerify
//        this.context.addExecutedJob(this)
//    }

    Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> inputParameters, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
        super(jobName
                , context.getConfiguration().getProcessingToolPath(context, TOOLID_WRAPIN_SCRIPT)
                , null
                , getToolMD5(TOOLID_WRAPIN_SCRIPT, context)
                , getResourceSetFromConfiguration(toolID, context)
                , arrayIndices
                , [:]
                , collectParentJobsFromFiles(parentFiles)
                , collectDependencyIDsFromFiles(parentFiles)
                , Roddy.getJobManager())
        this.context = context
        this.toolID = toolID

        Map<String, Object> defaultParameters = context.getDefaultJobParameters(toolID)

        if (inputParameters != null)
            defaultParameters.putAll(inputParameters)
        this.allRawInputParameters = defaultParameters

        for (String k : defaultParameters.keySet()) {
            if (this.parameters.containsKey(k)) continue
            Object _v = defaultParameters[k]
            if (_v == null) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("The parameter " + k + " has no valid value and will be set to <NO_VALUE>."))
                this.parameters[k] = NO_VALUE
            } else {
                Map<String, String> newParameters = convertParameterObject(k, _v)
                this.parameters.putAll(newParameters)
            }
        }

        parameters.putAll(convertResourceSetToParameters())

        this.parameters[PARM_WRAPPED_SCRIPT] = context.getConfiguration().getProcessingToolPath(context, toolID).getAbsolutePath()
        this.parameters[PARM_WRAPPED_SCRIPT_MD5] = getToolMD5(toolID, context)

        if (inputParameters != null)
            initialInputParameters.putAll(inputParameters)

        this.parentFiles = parentFiles ?: new LinkedList<BaseFile>()

        this.filesToVerify = filesToVerify ?: new LinkedList<BaseFile>()
        this.context.addExecutedJob(this)
    }

    static ResourceSet getResourceSetFromConfiguration(String toolID, ExecutionContext context) {
        ToolEntry te = context.getConfiguration().getTools().getValue(toolID)
        return te.getResourceSet(context.configuration) ?: new ResourceSet(null, null, null, null, null, null, null, null);
    }

    static String getToolMD5(String toolID, ExecutionContext context) {
        toolID != null && toolID.trim().length() > 0 ? context.getConfiguration().getProcessingToolMD5(toolID) : null
    }

    static List<Job> collectParentJobsFromFiles(List<BaseFile> parentFiles) {
        List<Job> parentJobs = parentFiles.collect {
            BaseFile bf -> bf?.getCreatingJobsResult()?.job
        }.findAll {
            de.dkfz.eilslabs.batcheuphoria.jobs.Job job -> job
        } as List<Job>
        return parentJobs
    }

    static List<JobDependencyID> collectDependencyIDsFromFiles(List<BaseFile> parentFiles) {
        List<JobDependencyID> dIDs = []
        if (parentFiles != null) {
            for (BaseFile bf : parentFiles) {
                if (bf.isSourceFile() && bf.getCreatingJobsResult() == null) continue
                try {
                    JobDependencyID jobid = bf.getCreatingJobsResult()?.getJobID()
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

    private Map<String, String> convertParameterObject(String k, Object _v) {
        Map<String, String> newParameters = new LinkedHashMap<>()
//            String v = "";
        if (_v instanceof File) {
            newParameters.put(k, ((File) _v).getAbsolutePath())
        } else if (_v instanceof BaseFile) {
            BaseFile bf = (BaseFile) _v
            String newPath = replaceParametersInFilePath(bf, allRawInputParameters)

            //Explicitely query newPath for a proper value!
            if (newPath == null) {
                // Auto path!
                int slotPosition = allRawInputParameters.keySet().asList().indexOf(k)
                if(Roddy.isStrictModeEnabled() && context.getFeatureToggleStatus(AvailableFeatureToggles.FailOnAutoFilenames))
                    throw new RuntimeException("Auto filenames are forbidden when strict mode is active.")
                else
                    context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("An auto filename will be used for ${jobName}:${slotPosition} / ${bf.class.name}"))
                String completeString = jobName + k + slotPosition
                if (parentFiles)
                    parentFiles.each {
                        BaseFile p ->
                            if (!p instanceof BaseFile) return

                            BaseFile _bf = (BaseFile) p
                            completeString += ("" + _bf.getAbsolutePath())

                    }

                File autoPath = new File(context.getOutputDirectory(), [jobName, k, Math.abs(completeString.hashCode()) as int, slotPosition].join("_") + ".auto")
//                File autoPath = new File(context.getOutputDirectory(), [jobName, k, '${RODDY_JOBID}', slotPosition].join("_") + ".auto")
                bf.setPath(autoPath)
                bf.setAsTemporaryFile()
                newPath = autoPath.absolutePath
            }

            newParameters.put(k, newPath)
//            newParameters.put(k + "_path", newPath);
            //TODO Create a toStringList method for filestages. The method should then be very generic.
//                this.parameters.put(k + "_fileStage_numericIndex", "" + bf.getFileStage().getNumericIndex());
//                this.parameters.put(k + "_fileStage_index", bf.getFileStage().getIndex());
//                this.parameters.put(k + "_fileStage_laneID", bf.getFileStage().getLaneId());
//                this.parameters.put(k + "_fileStage_runID", bf.getFileStage().getRunID());
        } else if (_v instanceof Collection) {
            //TODO This is not the best way to do this, think of a better one which is more generic.

            List<Object> convertedParameters = new LinkedList<>()
            for (Object o : _v as Collection) {
                if (o instanceof BaseFile) {
                    if (((BaseFile) o).getPath() != null)
                        convertedParameters.add(((BaseFile) o).getAbsolutePath())
                } else
                    convertedParameters.add(o.toString())
            }
            this.parameters[k] = "(${RoddyIOHelperMethods.joinArray(convertedParameters.toArray(), " ")})".toString()

//        } else if(_v.getClass().isArray()) {
//            newParameters.put(k, "parameterArray=(" + RoddyIOHelperMethods.joinArray((Object[]) _v, " ") + ")"); //TODO Put conversion to roddy helper methods?
        } else {
            try {
                newParameters[k] = _v.toString()
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        return newParameters
    }

    Map<String, String> convertResourceSetToParameters() {
        def rs = getResourceSet()
        Map<String, String> rsParameters = [:]
        if (rs == null) return rsParameters

        if (rs.isMemSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_MEM"] = rs.mem.toString()
        if (rs.isCoresSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_CORES"] = rs.cores.toString()
        if (rs.isNodesSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_NODES"] = rs.nodes.toString()
        if (rs.isQueueSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_QUEUE"] = rs.queue.toString()
        if (rs.isWalltimeSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_WALLTIME"] = rs.walltime.toString()
        if (rs.isAdditionalNodeFlagSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_NODEFLAGS"] = rs.additionalNodeFlag.toString()
        if (rs.isStorageSet()) rsParameters["RODDY_JOBRESOURCE_REQUEST_STORAGE"] = rs.storage.toString()
        return rsParameters
    }

    static String createJobName(BaseFile bf, String toolName, boolean reduceLevel) {
        return createJobName(bf, toolName, reduceLevel, null)
    }

    static String createJobName(BaseFile bf, String toolName, boolean reduceLevel, List<BaseFile> inputFilesForSizeCalculation) {
        ExecutionContext rp = bf.getExecutionContext()
        String runtime = rp.getTimestampString()
        StringBuilder sb = new StringBuilder()
        sb.append("r").append(runtime).append("_").append(bf.getPid()).append("_").append(toolName)
        return sb.toString()
    }

    static String createJobName(ExecutionContext rp, String postfix) {
        String runtime = rp.getTimestampString()
        StringBuilder sb = new StringBuilder()
        sb.append("r").append(runtime).append("_").append(rp.getDataSet().getId()).append("_").append(postfix)
        return sb.toString()
    }

    static String createJobName(FileGroup fg, String toolName, boolean reduceLevel, List<BaseFile> inputFilesForSizeCalculation) {
        BaseFile bf = (BaseFile) fg.getFilesInGroup().get(0)
        return createJobName(bf, toolName, reduceLevel, inputFilesForSizeCalculation)
    }

    static File replaceParametersInFilePath(BaseFile bf, Map<String, Object> parameters) {
        //TODO: It can occur that the regeneration of the filename is not valid!

        // Replace $_JOBPARAMETER items in path with proper values.
        File path = bf.getPath()
        if (path == null) {
            return null
        }

        String absolutePath = path.getAbsolutePath()
        List<FilenamePatternHelper.Command> commands = FilenamePatternHelper.extractCommands(bf.getExecutionContext(), PLACEHOLDER_JOBPARAMETER, absolutePath)
        for (FilenamePatternHelper.Command command : commands) {
            FilenamePatternHelper.CommandAttribute name = command.attributes.get("name")
            if (name != null) {
                String val = parameters[name.value]
                if (val == null) {
                    val = NO_VALUE
                    bf.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("A value named " + name.value + " cannot be found in the jobs parameter list for a file of ${bf.class.name}. The value is set to <NO_VALUE>"))
                }
                absolutePath = absolutePath.replace(command.fullString, val)
            }
            path = new File(absolutePath)
            bf.setPath(path)
        }
        path
    }

    //TODO Create a runArray method which returns several job results with proper array ids.
    @Override
    JobResult run() {
        if (runResult != null)
            throw new RuntimeException(Constants.ERR_MSG_ONLY_ONE_JOB_ALLOWED)

        ExecutionContextLevel contextLevel = context.getExecutionContextLevel()
        Configuration configuration = context.getConfiguration()
        File tool = configuration.getProcessingToolPath(context, toolID)

        StringBuilder dbgMessage = new StringBuilder()
        StringBuilder jobDetailsLine = new StringBuilder()
        Command cmd
        boolean isArrayJob = arrayIndices// (arrayIndices != null && arrayIndices.size() > 0);
        boolean runJob

        //Remove duplicate job ids as qsub cannot handle duplicate keys => job will hold forever as it releases the dependency queue linearly
        List<String> dependencies = dependencyIDsAsString.unique()
        //.collect { JobDependencyID jobDependencyID -> return jobDependencyID.getId() }.unique() as List<String>
        this.parameters.putAll(convertParameterObject(Constants.RODDY_PARENT_JOBS, dependencies))

        appendProcessingCommands(configuration)

        //See if the job should be executed
        if (contextLevel == ExecutionContextLevel.RUN || contextLevel == ExecutionContextLevel.CLEANUP) {
            runJob = true //The job is always executed if run is selected
            jobDetailsLine << "  Running job " + jobName
        } else if (contextLevel == ExecutionContextLevel.RERUN || contextLevel == ExecutionContextLevel.TESTRERUN) {
            runJob = checkIfJobShouldRerun(dbgMessage)
            jobDetailsLine << "  Rerun job " + jobName
        } else {
            return handleDifferentJobRun(dbgMessage)
        }

        //Execute the job or create a dummy command.
        if (runJob) {
            runResult = Roddy.getJobManager().runJob(this)
            cmd = runResult.command
            jobDetailsLine << " => " + cmd.getExecutionID()
            System.out.println(jobDetailsLine.toString())
            if (cmd.getExecutionID() == null) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_SUBMISSION_FAILURE.expand("Please check your submission command manually.\n\t  Is your access group set properly? [${context.getAnalysis().getUsergroup()}]\n\t  Can the submission binary handle your binary?\n\t  Is your submission system offline?"))
                if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.BreakSubmissionOnError)) {
                    context.abortJobSubmission()
                }
            }
        } else {
            Command command = new DummyCommand(Roddy.getJobManager(), this, jobName, false)
            setJobState(JobState.DUMMY)
            setRunResult(new de.dkfz.eilslabs.batcheuphoria.jobs.JobResult(command, command.getExecutionID(), false, false, this.tool, parameters, parentJobs as List<de.dkfz.eilslabs.batcheuphoria.jobs.Job>))
            this.setJobState(JobState.DUMMY)
        }

        //For auto filenames. Get the job id and push propagate it to all filenames.

        if (runResult?.jobID?.shortID) {
            allRawInputParameters.each { String k, Object o ->
                BaseFile bf = o instanceof BaseFile ? (BaseFile) o : null
                if (!bf) return

                String absolutePath = bf.getPath().getAbsolutePath()
                if (absolutePath.contains('${RODDY_JOBID}')) {
                    bf.setPath(new File(absolutePath.replace('${RODDY_JOBID}', runResult.jobID.shortID)))
                }
            }
        }

        if (isArrayJob) {
//            postProcessArrayJob(runResult)
            throw new NotImplementedException()
        } else {
            Roddy.getJobManager().addJobStatusChangeListener(this)
        }
        lastCommand = cmd
        return runResult
    }

    private void appendProcessingCommands(Configuration configuration) {
// Only extract commands from file if none are set
        if (getListOfProcessingCommand().size() == 0) {
            File srcTool = configuration.getSourceToolPath(toolID)

            logger.severe("Appending processing commands from config is currently not supported: Roddy/../Job.groovy appendProcessingCommands")
//            //Look in the configuration for resource options
//            ProcessingCommands extractedPCommands = Roddy.getJobManager().getProcessingCommandsFromConfiguration(configuration, toolID);
//
//            //Look in the script if no options are configured
//            if (extractedPCommands == null)
//                extractedPCommands = Roddy.getJobManager().extractProcessingCommandsFromToolScript(srcTool);
//
//            if (extractedPCommands != null)
//                this.addProcessingCommand(extractedPCommands);
        }
    }

    private JobResult handleDifferentJobRun(StringBuilder dbgMessage) {
//        logger.severe("Handling different job run is currently not supported: Roddy/../Job.groovy handleDifferentJobRun")
        dbgMessage << "\tdummy job created." + Constants.ENV_LINESEPARATOR
        File tool = context.getConfiguration().getProcessingToolPath(context, toolID)
        runResult = new JobResult(context, (Command) null, JobDependencyID.getNotExecutedFakeJob(this), false, tool, parameters, parentFiles.collect { it.getCreatingJobsResult()?.getJob() }.findAll { it })
        this.setJobState(JobState.DUMMY)
        return runResult
    }

    /**
     * Checks, if a job needs to be rerun.
     * @param dbgMessage
     * @return
     */
    private boolean checkIfJobShouldRerun(StringBuilder dbgMessage) {
        def isVerbosityHigh = logger.isVerbosityHigh()
        String sep = Constants.ENV_LINESEPARATOR
        if (isVerbosityHigh) dbgMessage << "Rerunning job " + jobName

        //Check the parents of the new files to see if one of those is invalid for the current context! A file might be validated during a dry context...
        boolean parentFileIsDirty = false

        if (isVerbosityHigh) dbgMessage << sep << "\tchecking parent files for validity"
        for (BaseFile pf : parentFiles) {
            if (!pf.isFileValid()) {
                if (isVerbosityHigh) dbgMessage << sep << "\tfile " << pf.getPath().getName() << " is dirty or does not exist." << sep
                parentFileIsDirty = true
            }
        }

        Integer knownFilesCnt = 0
        Boolean fileUnverified = false
        //Now check if the new created files are in the list of already existing files and if those files are valid.
        if (!parentFileIsDirty) {
            List res = verifyFiles(dbgMessage)
            fileUnverified = (Boolean) res[0]
            knownFilesCnt = (Integer) res[1]
        }

        boolean parentJobIsDirty = getParentJobs().collect { Job job -> job.isDirty }.any { boolean dirty -> dirty }

        boolean knownFilesCountMismatch = knownFilesCnt != filesToVerify.size()

        boolean rerunIsNecessary = fileUnverified || parentFileIsDirty || parentJobIsDirty || knownFilesCountMismatch

        if (isVerbosityHigh && rerunIsNecessary) dbgMessage << "\tJob will rerun" << sep

        //If all files could be found rerun if necessary otherwise rerun it definitive.
        if (!rerunIsNecessary) {
            if (isVerbosityHigh) dbgMessage << "\tJob was verified and will not be rerun" << sep
            return false
        }

        //More detailed if then because of enhanced debug / breakpoint possibilities
        if (isVerbosityHigh) {
            if (parentJobIsDirty)
                dbgMessage << "\t* Job is set to rerun because a parent job is marked dirty" << sep
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

    private List verifyFiles(StringBuilder dbgMessage) {
        String sep = Constants.ENV_LINESEPARATOR
        Boolean fileUnverified = false
        Integer knownFilesCnt = 0
        boolean isVerbosityHigh = LoggerWrapper.isVerbosityHigh()

        if (isVerbosityHigh) dbgMessage << "\tverifying specified files" << sep

        //TODO what about the case if no verifiable files where specified? Or if the know files count does not match
        for (BaseFile fp : filesToVerify) {
            //See if we know the file... so this way we can use the BaseFiles verification method.
            List<BaseFile> knownFiles = context.getAllFilesInRun()
            for (BaseFile bf : knownFiles) {
                for (int i = 0; i < 3; i++) {
                    if (fp == null || bf == null || fp.getAbsolutePath() == null || bf.getPath() == null)
                        try {
                            logger.severe("Taking a short nap because a file does not seem to be finished.")
                            Thread.sleep(100)
                        } catch (InterruptedException e) {
                            e.printStackTrace()
                        }
                }
                if (fp.getAbsolutePath().equals(bf.getPath().getAbsolutePath())) {
                    if (!bf.isFileValid()) {
                        fileUnverified = true
                        if (isVerbosityHigh) dbgMessage << "\tfile " << bf.getPath().getName() << " could not be verified!" << sep
                    }
                    knownFilesCnt++
                    break
                }
            }
        }
        return [fileUnverified, knownFilesCnt]
    }

    /**
     * Finally execute a job.
     * @param dependencies
     * @param dbgMessage
     * @param cmd
     * @return
     */
    private Command executeJob(List<String> dependencies, StringBuilder dbgMessage) {
        String sep = Constants.ENV_LINESEPARATOR
        File tool = context.getConfiguration().getProcessingToolPath(context, toolID)
        setJobState(JobState.UNSTARTED)
        Command cmd = Roddy.getJobManager().createCommand(this, tool, dependencies)
        ExecutionService.getInstance().execute(cmd)
        if (LoggerWrapper.isVerbosityMedium()) {
            dbgMessage << sep << "\tcommand was created and executed for job. ID is " + cmd.getExecutionID() << sep
        }
        if (LoggerWrapper.isVerbosityHigh()) logger.info(dbgMessage.toString())
        return cmd
    }

    ExecutionContext getExecutionContext() {
        return context
    }

    List<BaseFile> getParentFiles() {
        if (parentFiles != null)
            return new LinkedList<>(parentFiles)
        else
            return new LinkedList<>()
    }

    @Override
    File getLoggingDirectory() {
        return executionContext.getLoggingDirectory()
    }
/**
 * Returns the path to an existing log file.
 * If no logfile exists this returns null.
 *
 * @return
 */
    @Override
    public synchronized File getLogFile() {
        if (_logFile == null)
            _logFile = this.getExecutionContext().getRuntimeService().getLogFileForJob(this);
        return _logFile;
    }

    @Override
    public boolean hasLogFile() {
        if (getJobState().isPlannedOrRunning())
            return false;
        if (_logFile == null)
            return this.getExecutionContext().getRuntimeService().hasLogFileForJob(this);
        return true;
    }

    @Override
    List<String> finalParameters() {
        List<String> allParametersForFile = new LinkedList<>();
        if (parameters.size() > 0) {
            for (String parm : parameters.keySet()) {
                String val = parameters.get(parm);
                if (val.contains(StringConstants.DOLLAR_LEFTBRACE) && val.contains(StringConstants.BRACE_RIGHT)) {
                    val = val.replace(StringConstants.DOLLAR_LEFTBRACE, "#{"); // Replace variable names so they can be passed to qsub.
                }
                String key = parm;
                allParametersForFile << FileSystemAccessProvider.getInstance().getConfigurationConverter().convertConfigurationValue(new ConfigurationValue(key, val), executionContext).toString();
            }
        }
        return allParametersForFile;
    }

    @Override
    File getParameterFile() {
        return this.context.getParameterFilename(this)
    }

    List<BaseFile> getFilesToVerify() {
        return new LinkedList<>(filesToVerify)
    }

    synchronized boolean areAllFilesValid() {
        boolean allValid = true
        for (BaseFile baseFile : filesToVerify) {
            allValid &= baseFile.isFileValid()
        }
        return allValid
    }

    String getToolID() {
        return toolID
    }

    File getToolPath() {
        return getExecutionContext().getConfiguration().getProcessingToolPath(getExecutionContext(), toolID)
    }

    File getLocalToolPath() {
        return getExecutionContext().getConfiguration().getSourceToolPath(toolID)
    }

    String getToolMD5() {
        return toolID == null ? "-" : getExecutionContext().getConfiguration().getProcessingToolMD5(toolID)
    }
}
