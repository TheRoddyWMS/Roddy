package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.AvailableFeatureToggles;
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.knowledge.files.Tuple2;
import de.dkfz.roddy.tools.LoggerWrapper;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong;

import static de.dkfz.roddy.Constants.NO_VALUE;

@groovy.transform.CompileStatic
public class Job {
//    implements } Serializable {

    public static class FakeJob extends Job {
        public FakeJob() {
            super(null, "Fakejob", null, null);
        }

        public FakeJob(ExecutionContext context) {
            super(context, "Fakejob", null, null);
        }
    }

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(Job.class.getName());

    private JobType jobType = JobType.STANDARD;

    private final List<Job> arrayChildJobs = new LinkedList<>();

    private static AtomicLong absoluteJobCreationCounter = new AtomicLong();

    /**
     * The name of the job which should be passed to the execution system.
     */
    public final String jobName;

    /**
     * An internal job creation count. Has nothing to do with e.g. PBS / cluster / process id's!
     */
    public final long jobCreationCounter = absoluteJobCreationCounter.incrementAndGet();

    /**
     * The current context.
     */
    public transient final ExecutionContext context;
    /**
     * The tool you want to call.
     */
    public String toolID;

    public String toolMD5;

    /**
     * Parameters for the tool you want to call
     */
    private final Map<String, String> parameters;
    /**
     * If you want to generated arrays use this. <p>You can do things like: n-m,
     */
    public final List<String> arrayIndices;
    /**
     * Provide a list of files if you want to generate job dependencies.
     */
    public transient final List<BaseFile> parentFiles;
    /**
     * You should provide i.e. job ids of qsub jobs to automatically create job
     * dependencies.
     */
    public final List<JobDependencyID> dependencyIDs = new LinkedList<JobDependencyID>();
    /**
     * The list contains files which should be validated for a job restart.
     */
    public final List<BaseFile> filesToVerify;
    /**
     * Temporary value which defines the jobs state.
     */
    protected transient JobState currentJobState;

    private List<ProcessingCommands> processingCommand = new LinkedList<ProcessingCommands>();

    /**
     * Command object of last execution.
     */
    private transient Command lastCommand;

    /**
     * Stores the result when the job was executed.
     */
    private JobResult runResult;

    public Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> inputParameters, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
        this.context = context;
        this.toolID = toolID;
        this.toolMD5 = toolID ? context.getConfiguration().getProcessingToolMD5(toolID) : "";
        this.parameters = [:];

        Map<String, Object> defaultParameters = context.getDefaultJobParameters(toolID);
        if (inputParameters != null)
            defaultParameters.putAll(inputParameters);
        for (String k : defaultParameters.keySet()) {
            if (this.parameters.containsKey(k)) continue;
            Object _v = defaultParameters[k];
            if (_v == null) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("The parameter " + k + " has no valid value and will be set to <NO_VALUE>."));
                this.parameters[k] = NO_VALUE;
            } else {
                Map<String, String> newParameters = convertParameterObject(k, _v);
                this.parameters.putAll(newParameters);
            }
        }

        this.arrayIndices = arrayIndices == null ? new LinkedList<String>() : arrayIndices;
        this.parentFiles = parentFiles == null ? new LinkedList<BaseFile>() : parentFiles;
        if (parentFiles != null) {
            for (BaseFile bf : parentFiles) {
                try {
                    JobDependencyID jobid = bf.getCreatingJobsResult().getJobID();
                    if (jobid.isValidID()) {
                        dependencyIDs << jobid;
                    }
                } catch (Exception ex) {
                    logger.severe("Something is wrong for file: " + bf);
                }
            }
        }
        this.filesToVerify = filesToVerify == null ? new LinkedList<BaseFile>() : filesToVerify;
        this.jobName = jobName;
        this.currentJobState = JobState.UNKNOWN;
        this.context.addExecutedJob(this);
        if (arrayIndices == null)
            return;
    }

    public boolean isFakeJob() {
        if (this instanceof FakeJob)
            return true;
        if (jobName != null && jobName.equals("Fakejob"))
            return true;
        String jobID = getJobID();
        if (jobID == null)
            return false;
        return JobDependencyID.FakeJobID.isFakeJobID(jobID);
    }

    private Map<String, String> convertParameterObject(String k, Object _v) {
        Map<String, String> newParameters = new LinkedHashMap<>();
//            String v = "";
        if (_v instanceof File) {
            newParameters.put(k, ((File) _v).getAbsolutePath());
        } else if (_v instanceof BaseFile) {
            BaseFile bf = (BaseFile) _v;
            newParameters.put(k, bf.getPath().getAbsolutePath());
            newParameters.put(k + "_path", bf.getPath().getAbsolutePath());
            //TODO Create a toStringList method for filestages. The method should then be very generic.
//                this.parameters.put(k + "_fileStage_numericIndex", "" + bf.getFileStage().getNumericIndex());
//                this.parameters.put(k + "_fileStage_index", bf.getFileStage().getIndex());
//                this.parameters.put(k + "_fileStage_laneID", bf.getFileStage().getLaneId());
//                this.parameters.put(k + "_fileStage_runID", bf.getFileStage().getRunID());
        } else if (_v instanceof Collection) {
            //TODO This is not the best way to do this, think of a better one which is more generic.

            List<Object> convertedParameters = new LinkedList<>();
            for (Object o : ((Collection) _v)) {
                if (o instanceof BaseFile) {
                    if (((BaseFile) o).getPath() != null)
                        convertedParameters.add(((BaseFile) o).getAbsolutePath());
                } else
                    convertedParameters.add(o.toString());
            }
            this.parameters[k] = "parameterArray=(${RoddyIOHelperMethods.joinArray(convertedParameters.toArray(), " ")})".toString();

//        } else if(_v.getClass().isArray()) {
//            newParameters.put(k, "parameterArray=(" + RoddyIOHelperMethods.joinArray((Object[]) _v, " ") + ")"); //TODO Put conversion to roddy helper methods?
        } else {
            try {
                newParameters[k] = _v.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newParameters;
    }

    public Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> parameters, List<BaseFile> parentFiles) {
        this(context, jobName, toolID, arrayIndices, parameters, parentFiles, null);
    }

    public Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
        this(context, jobName, toolID, null, parameters, parentFiles, filesToVerify);
    }

    public Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters, List<BaseFile> parentFiles) {
        this(context, jobName, toolID, null, parameters, parentFiles, null);
    }

    public Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, List<BaseFile> filesToVerify, Map<String, Object> parameters) {
        this(context, jobName, toolID, arrayIndices, parameters, null, filesToVerify);
    }

    public Job(ExecutionContext context, String jobName, List<BaseFile> filesToVerify, String toolID, Map<String, Object> parameters) {
        this(context, jobName, toolID, null, parameters, null, filesToVerify);
    }

    public Job(ExecutionContext context, String jobName, String toolID, List<String> arrayIndices, Map<String, Object> parameters) {
        this(context, jobName, toolID, arrayIndices, parameters, null, null);
    }

    public Job(ExecutionContext context, String jobName, String toolID, Map<String, Object> parameters) {
        this(context, jobName, toolID, null, parameters, null, null);
    }

    /**
     * This is for job implementations which do the writeConfigurationFile on their own.
     */

    protected Job(String jobName, ExecutionContext context, String toolID, Map<String, String> parameters, List<String> arrayIndices, List<BaseFile> parentFiles, List<BaseFile> filesToVerify) {
        this.jobName = jobName;
        this.context = context;
        this.toolID = toolID;
        this.parameters = parameters;
        this.arrayIndices = arrayIndices;
        this.parentFiles = parentFiles;
        this.filesToVerify = filesToVerify;
        this.context.addExecutedJob(this);
        if (toolID != null && toolID.trim().length() > 0)
            this.toolMD5 = context.getConfiguration().getProcessingToolMD5(toolID);
    }

//    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
//        try {
//            s.writeUTF(jobName);
//            s.writeUTF(toolID);
//            s.writeObject(arrayIndices);
//            s.writeObject(dependencyIDs);
//            s.writeObject(filesToVerify);
//            s.writeObject(processingCommand);
//            s.writeInt(parameters.size());
//            for (String k : parameters.keySet()) {
//                Object o = parameters.get(k);
//                s.writeUTF(k);
//                s.writeUTF(o.toString());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
//        try {
//            s.defaultReadObject();
//            this.setJobState(JobState.UNKNOWN);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    private void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    //TODO Create a runArray method which returns several job results with proper array ids.
    public JobResult run() {
        if (runResult != null)
            throw new RuntimeException(Constants.ERR_MSG_ONLY_ONE_JOB_ALLOWED);

        ExecutionContextLevel contextLevel = context.getExecutionContextLevel()
        Configuration configuration = context.getConfiguration();
        File tool = configuration.getProcessingToolPath(context, toolID);

        StringBuilder dbgMessage = new StringBuilder();
        Command cmd;
        boolean isArrayJob = arrayIndices;// (arrayIndices != null && arrayIndices.size() > 0);
        boolean runJob;

        //Remove duplicate job ids as qsub cannot handle duplicate keys => job will hold forever as it releases the dependency queue linearly
        List<String> dependencies = dependencyIDs.collect { JobDependencyID jobDependencyID -> return jobDependencyID.getId(); }.unique() as List<String>;
        this.parameters.putAll(convertParameterObject(Constants.RODDY_PARENT_JOBS, dependencies));

        appendProcessingCommands(configuration)

        //See if the job should be executed
        if (contextLevel == ExecutionContextLevel.RUN || contextLevel == ExecutionContextLevel.CLEANUP) {
            dbgMessage << "  Running job " << jobName;
            runJob = true; //The job is always executed if run is selected
        } else if (contextLevel == ExecutionContextLevel.RERUN || contextLevel == ExecutionContextLevel.TESTRERUN) {
            runJob = handleRerunJob(dbgMessage);
        } else {
            return handleDifferentJobRun(dbgMessage);
        }

        //Execute the job or create a dummy command.
        if (runJob) {
            cmd = executeJob(dependencies, dbgMessage)
            if (cmd.getExecutionID() == null) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_SUBMISSION_FAILURE.expand("Please check your submission command manually.\n\t  Is your access group set properly? [${context.getAnalysis().getUsergroup()}]\n\t  Can the submission binary handle your binary?\n\t  Is your submission system offline?"));
                if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.BreakSubmissionOnError)) {
                    context.abortJobSubmission();
                }
            }
        } else {
            cmd = CommandFactory.getInstance().createDummyCommand(this, context, jobName, arrayIndices);
            this.setJobState(JobState.DUMMY);
        }

        runResult = new JobResult(context, cmd, cmd.getExecutionID(), runJob, isArrayJob, tool, parameters, parentFiles);
        if (isArrayJob) {
            postProcessArrayJob(runResult)
        } else {
            CommandFactory.getInstance().addJobStatusChangeListener(this);
        }
        lastCommand = cmd;
        return runResult;
    }

    private void appendProcessingCommands(Configuration configuration) {
// Only extract commands from file if none are set
        if (getListOfProcessingCommand().size() == 0) {
            File srcTool = configuration.getSourceToolPath(toolID);

            //Look in the configuration for resource options
            ProcessingCommands extractedPCommands = CommandFactory.getInstance().getProcessingCommandsFromConfiguration(configuration, toolID);

            //Look in the script if no options are configured
            if (extractedPCommands == null)
                extractedPCommands = CommandFactory.getInstance().extractProcessingCommandsFromToolScript(srcTool);

            if (extractedPCommands != null)
                this.addProcessingCommand(extractedPCommands);
        }
    }

    private JobResult handleDifferentJobRun(StringBuilder dbgMessage) {
        dbgMessage << "\tdummy job created." + Constants.ENV_LINESEPARATOR;
        File tool = context.getConfiguration().getProcessingToolPath(context, toolID);
        runResult = new JobResult(context, null, JobDependencyID.getNotExecutedFakeJob(this), false, tool, parameters, parentFiles);
        this.setJobState(JobState.DUMMY);
        return runResult;
    }

    private boolean handleRerunJob(StringBuilder dbgMessage) {
        def isVerbosityHigh = logger.isVerbosityHigh()
        String sep = Constants.ENV_LINESEPARATOR;
        if (isVerbosityHigh) dbgMessage << "  Rerunning job " + jobName;

        //Check the parents of the new files to see if one of those is invalid for the current context! A file might be validated during a dry context...
        boolean parentFileIsDirty = false;

        if (isVerbosityHigh) dbgMessage << sep << "\tchecking parent files for validity";
        for (BaseFile pf : parentFiles) {
            if (!pf.isFileValid()) {
                if (isVerbosityHigh) dbgMessage << sep << "\tfile " << pf.getPath().getName() << " is dirty or does not exist." << sep;
                parentFileIsDirty = true;
            }
        }

        Integer knownFilesCnt = 0;
        Boolean fileUnverified = false;
        //Now check if the new created files are in the list of already existing files and if those files are valid.
        if (!parentFileIsDirty) {
            List res = verifyFiles(dbgMessage)
            fileUnverified = (Boolean) res[0];
            knownFilesCnt = (Integer) res[1];
        }

        boolean rerunIsNecessary = fileUnverified || parentFileIsDirty;

        if (isVerbosityHigh) dbgMessage << "\tverification was successful ? " << !rerunIsNecessary << sep;
        //If all files could be found rerun if necessary otherwise rerun it definitive.
        if (knownFilesCnt != filesToVerify.size() ? true : rerunIsNecessary) {
            //More detailed if then because of enhanced debug / breakpoint possibilities
            if (isVerbosityHigh) dbgMessage << "\tjob will be rerun because either the number of existing files does not match with the number of files which should be created or because something could not be verified." << sep;
            return true;
        }
        if (isVerbosityHigh) dbgMessage << "\tjob will not be rerun.";
        return false;
    }

    private List verifyFiles(StringBuilder dbgMessage) {
        String sep = Constants.ENV_LINESEPARATOR;
        Boolean fileUnverified = false;
        Integer knownFilesCnt = 0;
        boolean isVerbosityHigh = LoggerWrapper.isVerbosityHigh();

        if (isVerbosityHigh) dbgMessage << "\tverifying specified files" << sep;

        //TODO what about the case if no verifiable files where specified? Or if the know files count does not match
        for (BaseFile fp : filesToVerify) {
            //See if we know the file... so this way we can use the BaseFiles verification method.
            List<BaseFile> knownFiles = context.getAllFilesInRun();
            for (BaseFile bf : knownFiles) {
                for (int i = 0; i < 3; i++) {
                    if (fp == null || bf == null || fp.getAbsolutePath() == null || bf.getPath() == null)
                        try {
                            logger.severe("Taking a short nap because a file does not seem to be finished.");
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
                if (fp.getAbsolutePath().equals(bf.getPath().getAbsolutePath())) {
                    if (!bf.isFileValid()) {
                        fileUnverified = true;
                        if (isVerbosityHigh) dbgMessage << "\tfile " << bf.getPath().getName() << " could not be verified!" << sep;
                    }
                    knownFilesCnt++;
                    break;
                }
            }
        }
        return [fileUnverified, knownFilesCnt];
    }

    private void postProcessArrayJob(JobResult runResult) {
        Map<String, Object> prmsAsStringMap = new LinkedHashMap<>();
        for (String k : parameters.keySet()) {
            prmsAsStringMap.put(k, parameters.get(k));
        }
        jobType = JobType.ARRAY_HEAD;
        //TODO Think of proper array index handling!
        int i = 1;
        for (String arrayIndex : arrayIndices) {
            JobResult jr = CommandFactory.getInstance().convertToArrayResult(this, runResult, i++);

            Job childJob = new Job(context, jobName + "[" + arrayIndex + "]", toolID, prmsAsStringMap, parentFiles, filesToVerify);
            childJob.setJobType(JobType.ARRAY_CHILD);
            childJob.setRunResult(jr);
            arrayChildJobs.add(childJob);
            CommandFactory.getInstance().addJobStatusChangeListener(childJob);
            this.context.addExecutedJob(childJob);
        }
    }

    /**
     * Finally execute a job.
     * @param dependencies
     * @param dbgMessage
     * @param cmd
     * @return
     */
    private Command executeJob(List<String> dependencies, StringBuilder dbgMessage) {
        String sep = Constants.ENV_LINESEPARATOR;
        File tool = context.getConfiguration().getProcessingToolPath(context, toolID);
        setJobState(JobState.UNSTARTED);
        Command cmd = CommandFactory.getInstance().createCommand(this, tool, dependencies);
        ExecutionService.getInstance().execute(cmd);
        if (LoggerWrapper.isVerbosityMedium()) {
            dbgMessage << sep << "\tcommand was created and executed for job. ID is " + cmd.getExecutionID() << sep;
        }
        logger.info(dbgMessage.toString());
        // The following line is part of the OTP interface and should not be changed without communicating to the OTP team.
        System.out.println("  Rerun job " + jobName + " => " + cmd.getExecutionID())
        return cmd;
    }

    public void addProcessingCommand(ProcessingCommands processingCommand) {
        if (processingCommand == null) return;
        this.processingCommand.add(processingCommand);
    }

    public JobType getJobType() {
        return jobType;
    }

    public List<ProcessingCommands> getListOfProcessingCommand() {
        return processingCommand;
    }

    public ExecutionContext getExecutionContext() {
        return context;
    }

    public List<BaseFile> getParentFiles() {
        if (parentFiles != null)
            return new LinkedList<>(parentFiles);
        else
            return new LinkedList<>();
    }

    public List<BaseFile> getFilesToVerify() {
        return new LinkedList<>(filesToVerify);
    }

    public synchronized boolean areAllFilesValid() {
        boolean allValid = true;
        for (BaseFile baseFile : filesToVerify) {
            allValid &= baseFile.isFileValid();
        }
        return allValid;
    }

    public JobResult getRunResult() {
        return runResult;
    }

    public void setRunResult(JobResult result) {
        this.runResult = result;
    }

    /**
     * If the job was executed this return the jobs id otherwise null.
     *
     * @return
     */
    public String getJobID() {
        if (runResult != null)
            if (runResult.getJobID() != null)
                return runResult.getJobID().getShortID();
            else
                return "Unknown";
        else
            return null;
    }

    public String getToolID() {
        return toolID;
    }

    public File getToolPath() {
        return getExecutionContext().getConfiguration().getProcessingToolPath(getExecutionContext(), toolID);
    }

    public File getLocalToolPath() {
        return getExecutionContext().getConfiguration().getSourceToolPath(toolID);
    }

    public String getToolMD5() {
        return toolID == null ? "-" : getExecutionContext().getConfiguration().getProcessingToolMD5(toolID);
    }

    private File _logFile = null;

    public String getJobName() {
        return jobName;
    }

    /**
     * Returns the path to an existing log file.
     * If no logfile exists this returns null.
     *
     * @return
     */
    public synchronized File getLogFile() {
        if (_logFile == null)
            _logFile = this.getExecutionContext().getProject().getRuntimeService().getLogFileForJob(this);
        return _logFile;
    }

    public boolean hasLogFile() {
        if (getJobState().isPlannedOrRunning())
            return false;
        if (_logFile == null)
            return this.getExecutionContext().getProject().getRuntimeService().hasLogFileForJob(this);
        return true;
    }

    public void setJobState(JobState js) {
        if (jobType == JobType.ARRAY_HEAD)
            return;
        JobState old = this.currentJobState;
        this.currentJobState = js;
        fireJobStatusChangedEvent(old, js);
    }

    public JobState getJobState() {
        if (jobType == JobType.ARRAY_HEAD) {
            int runningJobs = 0;
            int failedJobs = 0;
            int finishedJobs = 0;
            int unknownJobs = 0;
            for (Job job : arrayChildJobs) {
                if (job.getJobState().isPlannedOrRunning())
                    runningJobs++;
                else if (job.getJobState() == JobState.FAILED)
                    failedJobs++;
                else if (job.getJobState() == JobState.OK)
                    finishedJobs++;
                else if (job.getJobState() == JobState.UNKNOWN)
                    unknownJobs++;
            }
            if (failedJobs > 0) return JobState.FAILED;
            if (unknownJobs > 0) return JobState.UNKNOWN;
            if (runningJobs > 0) return JobState.RUNNING;
            return JobState.OK;
        }
        return currentJobState != null ? currentJobState : JobState.UNKNOWN;
    }

    public Command getLastCommand() {
        return lastCommand;
    }

    private transient final Deque<JobStatusListener> listeners = new ConcurrentLinkedDeque<>();

    public void addJobStatusListener(JobStatusListener listener, boolean replaceOfSameClass) {
        //TODO Synchronize
        if (replaceOfSameClass) {
            Class c = listener.getClass();
            Deque<JobStatusListener> listenersToKeep = new ConcurrentLinkedDeque<>();
            for (JobStatusListener jsl : this.listeners) {
                if (jsl.getClass() != c)
                    listenersToKeep.add(jsl);
            }
            listeners.clear();
            listeners.addAll(listenersToKeep);
        }
        addJobStatusListener(listener);
    }

    public void addJobStatusListener(JobStatusListener listener) {
        synchronized (listeners) {
            if (this.listeners.contains(listener)) return;
            this.listeners.add(listener);
        }
    }

    private void fireJobStatusChangedEvent(JobState old, JobState newState) {
        //TODO Synchronize
        if (old == newState) return;
        for (JobStatusListener jsl : listeners)
            jsl.jobStatusChanged(this, old, newState);
    }

    public void removeJobStatusListener(JobStatusListener listener) {
        if (listener == null)
            return;
        synchronized (listeners) {
            if (this.listeners.contains(listener))
                this.listeners.remove(listener);
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

}
