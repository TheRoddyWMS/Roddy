package de.dkfz.roddy.core;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationFactory;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.*
import de.dkfz.roddy.tools.LoggerWrapper

import java.util.*;

/**
 * An ExecutionContect is the runtime context for an analysis and a DataSet.<br />
 * It keeps track of context relevant information like:<br />
 * <ul>
 * <li>Created files</li>
 * <li>Executed jobs and commands</li>
 * <li>Job states</li>
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
@groovy.transform.CompileStatic
public class ExecutionContext implements JobStatusListener {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionContext.class.name);

    private static final List<ExecutionContextListener> staticListeners = new LinkedList<>();
    /**
     * The project to which this context belongs
     */
    protected final Project project;
    /**
     * The analysis for which this context was created
     */
    protected final Analysis analysis;
    /**
     * The source data set on which we work.
     */
    protected final DataSet dataSet;
    /**
     * Keeps a list of all files which were created with this process.
     */
    protected final List<BaseFile> allFilesInRun = new LinkedList<BaseFile>();
    /**
     * Keeps a list of all (previously) started jobs which belong to this process.
     */
    protected final List<Job> jobsForProcess = new LinkedList<Job>();
    /**
     * Stores a list of all calls which were passed to the job system within this context.
     */
    private final List<Command> commandCalls = new LinkedList<Command>();
    /**
     * This is some sort of synchronization checkpoint marker.
     * Contexts which were started with the same
     */
    private final long creationCheckPoint;
    /**
     * Lockfiles are a central process locking and synchronization mechanism.
     * Therefore they are stored in this central position.
     */
    private final Map<Object, LockFile> lockFiles = new HashMap<Object, LockFile>();
    /**
     * Keeps a list of errors which happen either on read back or on execution.
     * The list is not stored and rebuilt if necessary, so not all errors might be available.
     */
    private final List<ExecutionContextError> errors = new LinkedList<>();
    private final List<ExecutionContextListener> listeners = new LinkedList<>();
    /**
     * The timestamp of this context object
     */
    protected Date timestamp = new Date();
    /**
     * The input directory for the context's dataset
     */
    private File inputDirectory;
    /**
     * The output directory for the context's dataset
     */
    private File outputDirectory;
    /**
     * The path to the execution directory for this context.
     */
    private File executionDirectory = null;
    /**
     * The path to the directory for process lock files.
     */
    private File lockFilesDirectory = null;
    /**
     * The directory for storing temporary files.
     */
    private File temporaryDirectory = null;
    /**
     * The path to process log files.
     */
    private File loggingDirectory = null;
    /**
     * This directory contains common information for the current project.
     */
    private File commonExecutionDirectory;
    /**
     * The file containing a list of all known copied analysis tools archives and their md5 sum.
     */
    private File md5OverviewFile
    /**
     * The path in which a copy of the analysis tools are put to.
     */
    private File analysisToolsDirectory;
    private ExecutionContextLevel executionContextLevel;
    private ExecutionContextSubLevel executionContextSubLevel = ExecutionContextSubLevel.RUN_UNINITIALIZED;
    private ProcessingFlag processingFlag = ProcessingFlag.STORE_EVERYTHING;
    /**
     * The user who created the context (if known)
     */
    private String executingUser = Constants.UNKNOWN_USER;

    public ExecutionContext(String userID, Analysis analysis, DataSet dataSet, ExecutionContextLevel executionContextLevel, File outputDirectory, File inputDirectory, File executionDirectory) {
        this(userID, analysis, dataSet, executionContextLevel, outputDirectory, inputDirectory, executionDirectory, -1);
    }

    public ExecutionContext(String userID, Analysis analysis, DataSet dataSet, ExecutionContextLevel executionContextLevel, File outputDirectory, File inputDirectory, File executionDirectory, long creationCheckPoint, boolean dontRaise) {
        this.executionDirectory = executionDirectory;
        this.outputDirectory = outputDirectory;
        this.inputDirectory = inputDirectory;
        this.creationCheckPoint = creationCheckPoint;
        this.project = analysis?.getProject();
        this.analysis = analysis;
        this.executionContextLevel = executionContextLevel;
        this.dataSet = dataSet;
        setExecutingUser(userID);
        if (!dontRaise)
            raiseNewExecutionContextListenerEvent(this);
    }

    public ExecutionContext(String userID, Analysis analysis, DataSet dataSet, ExecutionContextLevel executionContextLevel, File outputDirectory, File inputDirectory, File executionDirectory, long creationCheckPoint) {
        this(userID, analysis, dataSet, executionContextLevel, outputDirectory, inputDirectory, executionDirectory, creationCheckPoint, false);
    }

    /**
     * The constructor will be called if one attempts to read out a a stored execution context.
     */
    public ExecutionContext(AnalysisProcessingInformation api, Date readOutTimestamp) {
        this.executionDirectory = api.getExecPath();
        this.dataSet = api.getDataSet();
        this.analysis = api.getAnalysis();
        this.project = analysis?.getProject();
        this.executionContextLevel = ExecutionContextLevel.READOUT;
        this.inputDirectory = dataSet.getInputFolderForAnalysis(analysis);
        this.outputDirectory = dataSet.getOutputFolderForAnalysis(analysis);
        this.setTimestamp(readOutTimestamp);
        creationCheckPoint = -1;
    }

    /**
     * Cloning constructor to create a shallow object copy.
     */
    private ExecutionContext(ExecutionContext p) {
        this.project = p.project;
        this.analysis = p.analysis;
        this.dataSet = p.dataSet;
        this.timestamp = p.timestamp;
        this.inputDirectory = p.inputDirectory;
        this.outputDirectory = p.outputDirectory;
        this.executionDirectory = p.executionDirectory;
        this.lockFilesDirectory = p.lockFilesDirectory;
        this.temporaryDirectory = p.temporaryDirectory;
        this.loggingDirectory = p.loggingDirectory;
        this.executionContextLevel = p.executionContextLevel;
        this.executionContextSubLevel = p.executionContextSubLevel;
        this.processingFlag = p.processingFlag;
        this.executingUser = p.executingUser;
        this.allFilesInRun.addAll(p.getAllFilesInRun());
        this.jobsForProcess.addAll(p.jobsForProcess);
        this.commandCalls.addAll(p.commandCalls);
        this.lockFiles.putAll(p.lockFiles);
        this.errors.addAll(p.errors);
        creationCheckPoint = -1;
    }

    public static void registerStaticContextListener(ExecutionContextListener ecl) {
        staticListeners.add(ecl);
    }

    private static void raiseNewExecutionContextListenerEvent(ExecutionContext ec) {
        for (ExecutionContextListener ecl : staticListeners) ecl.newExecutionContextEvent(ec);
    }

    /**
     * Creates a shallow copy if this context.
     *
     * @return
     */
    public ExecutionContext clone() {
        return new ExecutionContext(this);
    }

    /**
     * Create a group of locked files for a file group. The files are create in a Job.
     *
     * @return
     */
    public LockFileGroup createLockFiles(FileGroup fileGroup) {
        return createLockFiles(fileGroup.getFilesInGroup() as List<Object>);
    }

    /**
     * Create a group of locked files for a file group. The files are create in a Job.
     *
     * @return
     */
    public LockFileGroup createLockFiles(List<Object> identifiers) {
        LockFileGroup lfg = new LockFileGroup(null);
        for (int i = 0; i < identifiers.size(); i++) {
            LockFile lockfile = new LockFile(this, new File(getLockFilesDirectory().getAbsolutePath() + File.separator + "~" + System.nanoTime()));
            lfg.addFile(lockfile);
            lockFiles.put(identifiers.get(i), lockfile);
        }

        ExecutionContext run = this;

        final String TOOL = Constants.TOOLID_CREATE_LOCKFILES;
        run.getDefaultJobParameters(TOOL);
        Map<String, Object> parameters = getDefaultJobParameters(TOOL);
        int i = 0;
        for (BaseFile bf : lfg.getFilesInGroup()) {
            parameters.put("LOCKFILE_" + (i++), bf.getPath().getAbsolutePath());
        }

        Job job = new Job(run, CommandFactory.createJobName(run, "roddy_lockfile_creation"), TOOL, parameters);

        JobResult jobResult = job.run();
        for (BaseFile bf : lfg.getFilesInGroup())
            bf.setCreatingJobsResult(jobResult);
        return lfg;
    }

    public Map<String, Object> getDefaultJobParameters(String TOOLID) {
        return getRuntimeService().getDefaultJobParameters(this, TOOLID);
    }

    public String createJobName(BaseFile p, String TOOLID) {
        return createJobName(p, TOOLID, false, new LinkedList<BaseFile>());
    }

    public String createJobName(BaseFile p, String TOOLID, boolean reduceLevel) {
        return createJobName(p, TOOLID, reduceLevel, new LinkedList<BaseFile>());
    }

    public String createJobName(BaseFile p, String TOOLID, List<BaseFile> inputFilesForSizeCalculation) {
        return createJobName(p, TOOLID, false, inputFilesForSizeCalculation);
    }

    public String createJobName(BaseFile p, String TOOLID, boolean reduceLevel, List<BaseFile> inputFilesForSizeCalculatio) {
        return getRuntimeService().createJobName(this, p, TOOLID, reduceLevel);
    }

    /**
     * Returns an (available) lockfile for the passed identifier.
     *
     * @param identifier
     * @return
     */
    public LockFile getLockFile(Object identifier) {
        return lockFiles.get(identifier);
    }

    /**
     * This creates a job for memory or other n x n buffers.
     * This should be a bit more flexible (much more!) so it is really only temporary.
     * TODO Create a versatile buffer and locking logic.
     */
    public StreamingBufferFileGroup runStreamingBuffer(LockFileGroup lockfilesInput, LockFileGroup lockfilesOutput, int buffers, int sizePerBuffer, BufferUnit unit) {
        ExecutionContext run = this;

        final String TOOL = Constants.TOOLID_STREAM_BUFFER;

        List<File> inFileNames = new LinkedList<File>();
        List<File> outFileNames = new LinkedList<File>();
        List<StreamingBufferConnectionFile> inFiles = new LinkedList<StreamingBufferConnectionFile>();
        List<StreamingBufferConnectionFile> outFiles = new LinkedList<StreamingBufferConnectionFile>();

        Map<String, Object> parameters = getDefaultJobParameters(TOOL);
        int i = 0;
        for (BaseFile bf : lockfilesInput.getFilesInGroup()) {
            parameters.put("IN_LOCKFILE_" + i, bf.getPath().getAbsolutePath());
            i++;
        }
        i = 0;
        for (BaseFile bf : lockfilesOutput.getFilesInGroup()) {
            parameters.put("OUT_LOCKFILE_" + i, bf.getPath().getAbsolutePath());
            i++;
        }
        for (i = 0; i < buffers; i++) {
            File inFile = new File(run.getTemporaryDirectory().getAbsolutePath() + File.separator + System.nanoTime() + "_portExchange.tmp");
            File outFile = new File(run.getTemporaryDirectory().getAbsolutePath() + File.separator + System.nanoTime() + "_portExchange.tmp");
            inFileNames.add(inFile);
            outFileNames.add(outFile);
            parameters.put("PORTEXCHANGE_INFILE_" + i, inFile.getAbsolutePath());
            parameters.put("PORTEXCHANGE_OUTFILE_" + i, outFile.getAbsolutePath());
        }

        parameters.put("BUFFER_COUNT", "" + buffers);
        parameters.put("BUFFER_SIZE", "" + sizePerBuffer);
        parameters.put("BUFFER_UNIT", "" + unit);

        List<BaseFile> pFiles = new LinkedList<BaseFile>(lockfilesInput.getFilesInGroup());
        pFiles.addAll(lockfilesOutput.getFilesInGroup());

//        StreamingBufferFileGroup

        JobResult jr = (new Job(this, CommandFactory.createJobName(run, "roddy_streamingbuffer_" + buffers + "x" + sizePerBuffer + unit), TOOL, parameters, pFiles)).run();
        for (i = 0; i < buffers; i++) {
            inFiles.add(new StreamingBufferConnectionFile(inFileNames.get(i), this, jr, null, lockfilesInput.getFilesInGroup().get(0).getFileStage()));
            outFiles.add(new StreamingBufferConnectionFile(outFileNames.get(i), this, jr, null, lockfilesInput.getFilesInGroup().get(0).getFileStage()));
        }
        return new StreamingBufferFileGroup(inFiles, outFiles);
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public RuntimeService getRuntimeService() {
        return project.getRuntimeService();
    }

    public ExecutionContextLevel getExecutionContextLevel() {
        return executionContextLevel;
    }

    public void setExecutionContextLevel(ExecutionContextLevel newLevel) {
        this.executionContextLevel = newLevel;
    }

    public ExecutionContextSubLevel getDetailedExecutionContextLevel() {
        return executionContextSubLevel;
    }

    public synchronized void setDetailedExecutionContextLevel(ExecutionContextSubLevel subLevel) {
        ExecutionContextSubLevel temp = this.executionContextSubLevel;
        this.executionContextSubLevel = subLevel;
        if (temp != subLevel)
            raiseDetailedExecutionContextLevelChanged();
    }

    public ProcessingFlag getProcessingFlag() {
        return processingFlag;
    }

    public ProcessingFlag setProcessingFlag(ProcessingFlag processingFlag) {
        ProcessingFlag temp = this.processingFlag;
        this.processingFlag = processingFlag;
        return temp;
    }

    /**
     * Sets the level to ABORTED if it was allowed to submit jobs.
     */
    public void abortJobSubmission() {
        if (executionContextLevel.canSubmitJobs)
            executionContextLevel = ExecutionContextLevel.ABORTED;
    }

    public Project getProject() {
        return project;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public Configuration getConfiguration() {
        return analysis.getConfiguration();
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getExecutingUser() {
        return this.executingUser;
    }

    public void setExecutingUser(String p) {
        this.executingUser = p;
    }

    /**
     * Returns the execution directory for this context. If it was not set this is done here.
     *
     * @return
     */
    public synchronized File getExecutionDirectory() {
        if (executionDirectory == null)
            executionDirectory = project.getRuntimeService().getExecutionDirectory(this);
        return executionDirectory;
    }

    public File getFileForAnalysisToolsArchiveOverview() {
        if (!md5OverviewFile)
            md5OverviewFile = project.getRuntimeService().getAnalysedMD5OverviewFile(this)
        return md5OverviewFile;
    }

    public synchronized File getCommonExecutionDirectory() {
        if (!commonExecutionDirectory)
            commonExecutionDirectory = project.getRuntimeService().getCommonExecutionDirectory(this);
        return commonExecutionDirectory;
    }

    public boolean hasExecutionDirectory() {
        return executionDirectory != null;
    }
    Boolean checkedIfAccessRightsCanBeSet = null;

    public boolean isAccessRightsModificationAllowed() {
        // Include an additional check, if the target filesystem allows the modification and disable this, if necessary.
        boolean modAllowed = getConfiguration().getConfigurationValues().getBoolean(ConfigurationConstants.CFG_ALLOW_ACCESS_RIGHTS_MODIFICATION, true)
        if (modAllowed && checkedIfAccessRightsCanBeSet == null) {
            checkedIfAccessRightsCanBeSet = FileSystemAccessProvider.getInstance().checkIfAccessRightsCanBeSet(this)
            if (!checkedIfAccessRightsCanBeSet) {
                modAllowed = false
                logger.severe("Access rights modification was disabled. The test on the file system raised an error.");
            };
        }
        return modAllowed;
    }

    public FileSystemAccessProvider getFileSystemAccessProvider() {
        return FileSystemAccessProvider.getInstance()
    }

    public String getOutputDirectoryAccess() {
        if (!isAccessRightsModificationAllowed()) return null;
        return getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ACCESS_RIGHTS_FOR_DIRECTORIES,
                fileSystemAccessProvider.commandSet.getDefaultAccessRightsString()).toString()
    }

    public String getOutputFileAccessRights() {
        if (!isAccessRightsModificationAllowed()) return null;
        return getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ACCESS_RIGHTS,
                fileSystemAccessProvider.commandSet.getDefaultAccessRightsString()).toString()
    }

    public String getOutputGroupString() {
        if (!isAccessRightsModificationAllowed()) return null;
        return getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_FILE_GROUP)
        fileSystemAccessProvider.getMyGroup().toString()
    }

    public String getUMask() {
        return getConfiguration().getConfigurationValues().getString(ConfigurationConstants.CFG_OUTPUT_UMASK,
                fileSystemAccessProvider.commandSet.getDefaultUMask());
    }

    public synchronized File getLockFilesDirectory() {
        if (lockFilesDirectory == null)
            lockFilesDirectory = project.getRuntimeService().getLockFilesDirectory(this);
        return lockFilesDirectory;
    }

    public synchronized File getTemporaryDirectory() {
        if (temporaryDirectory == null)
            temporaryDirectory = project.getRuntimeService().getTemporaryDirectory(this);
        return temporaryDirectory;
    }

    public synchronized File getLoggingDirectory() {
        if (loggingDirectory == null)
            loggingDirectory = project.getRuntimeService().getLoggingDirectory(this);
        return loggingDirectory;
    }

    public synchronized File getAnalysisToolsDirectory() {
        if (analysisToolsDirectory == null)
            analysisToolsDirectory = project.getRuntimeService().getAnalysisToolsDirectory(this);
        return analysisToolsDirectory;
    }

    public File getParameterFilename(Command command) {
        new File(getExecutionDirectory(), "${command.getJob().getJobName()}_${command.getJob().jobCreationCounter}.parameters");
    }

    public String getTimeStampString() {
        return InfoObject.formatTimestamp(timestamp);
    }

    public Date getTimeStamp() {
        return timestamp;
    }

    public long getCreationCheckPoint() {
        return creationCheckPoint;
    }

    public void addFile(BaseFile file) {
        if (!processingFlag.contains(ProcessingFlag.STORE_FILES))
            return;
        if (executionContextLevel == ExecutionContextLevel.QUERY_STATUS
                || executionContextLevel == ExecutionContextLevel.RERUN
                || executionContextLevel == ExecutionContextLevel.TESTRERUN
                || executionContextLevel == ExecutionContextLevel.RUN) {
            synchronized (allFilesInRun) {
                this.allFilesInRun.add(file);
            }
        }
    }

    public List<BaseFile> getAllFilesInRun() {
        List<BaseFile> newList = new LinkedList<BaseFile>();
        synchronized (allFilesInRun) {
            newList.addAll(allFilesInRun);
        }
        return newList;
    }

    public Map<String, JobState> readJobStatesFromLogFile() {
        return null;
    }

    public Map<String, JobState> readJobStateLogFile() {
        return getRuntimeService().readInJobStateLogFile(this);
    }

    public List<Job> getExecutedJobs() {
        return new LinkedList<Job>(jobsForProcess);
    }

    public Collection<Job> getStartedJobs() {
        return jobsForProcess.findAll { Job job -> job != null && !job.isFakeJob() && !job.getJobState().dummy }
    }

    public void setExecutedJobs(List<Job> previousJobs) {
        //TODO Add processing flag query
        jobsForProcess.clear();
        jobsForProcess.addAll(previousJobs);
        for (Job job : previousJobs) {
            job.addJobStatusListener(this);
            raiseJobAddedEvent(job);
        }
    }

    public void addExecutedJob(Job job) {
        if ((job.getJobState() == JobState.DUMMY || job.getJobState() == JobState.UNKNOWN) && !processingFlag.contains(ProcessingFlag.STORE_DUMMY_JOBS))
            return;

        //TODO Synchronize jobsForProcess!
        jobsForProcess.add(job);
        job.addJobStatusListener(this);
        raiseJobAddedEvent(job);
    }

    /**
     * Determines if an execution context still has some jobs which are running.
     * Only works for the latest readout execution context and a context which was just created.
     *
     * @return
     */
    public boolean hasRunningJobs() {
        if (jobsForProcess.size() == 0) {
            return false;
        }

        //Query readout jobs.
        if (jobsForProcess.get(0) instanceof ReadOutJob) {
            for (Job job : jobsForProcess) {
                ReadOutJob rj = (ReadOutJob) job;

                def state = rj.getJobState()
                if (state.isPlannedOrRunning()) {
                    if (state.isRunning())
                        return true;
                    else {
                        //Check previous jobs... Or just wait for the next step???

                    }
                }
            }
        }

        //Query current jobs, i.e. on recheck
        List<String> jobIDsForQuery = new LinkedList<>();
        for (Job job : jobsForProcess) {
            JobResult runResult = job.getRunResult();
            if (runResult != null && runResult.getJobID().getId() != null) {
                jobIDsForQuery.add(runResult.getJobID().getId());
            }
        }
        Map<String, JobState> map = CommandFactory.getInstance().queryJobStatus(jobIDsForQuery);
        for (JobState js : map.values()) {
            if (js.isPlannedOrRunning())
                return true;
        }

        return false;
    }

    public void addCalledCommand(Command command) {
        commandCalls.add(command);
    }

    public List<Command> getCommandCalls() {
        return commandCalls;
    }

    public List<File> getLogFilesForExecutedJobs() {
        List<File> allFiles = new LinkedList<>();
        for (Job j : getExecutedJobs()) {
            allFiles.add(j.getLogFile());
        }
        return allFiles;
    }

    public List<File> getAdditionalLogFiles() {
        return getRuntimeService().getAdditionalLogFilesForContext(this);
    }

    /**
     * Appends an entry to the errors list.
     *
     * @param error
     */
    public void addErrorEntry(ExecutionContextError error) {
        this.errors.add(error);
    }

    /**
     * Returns a shallow copy of the errors list.
     *
     * @return
     */
    public List<ExecutionContextError> getErrors() {
        return new LinkedList<>(errors);
    }

    /**
     * Execute the stored dataset with the stored analysis.
     *
     * @return
     */
    public boolean checkExecutability() {
        return analysis.getWorkflow().checkExecutability(this);
    }

    private boolean checkDirectoryOrFile(File f) {
        boolean readable = fileSystemAccessProvider.isReadable(f);
        fileSystemAccessProvider.isWritable(f);
        return true;
    }

    /**
     * Checks various files on the file system for read and write access.
     *
     * @return
     */
    public boolean checkFileSystemAccessibility() {
        //See if input directory is readable.

        analysis.getInputBaseDirectory();
        dataSet.getInputFolderForAnalysis(analysis);
        dataSet.getOutputBaseFolder();
        dataSet.getOutputFolderForAnalysis(analysis);

        return false;
    }

    /**
     * Execute the stored dataset with the stored analysis.
     *
     * @return
     */
    public boolean execute() {
        return analysis.getWorkflow().execute(this);
    }

    /**
     * Create testdata for the stored dataset.
     *
     * @return
     */
    public boolean createTestdata() {
        return analysis.getWorkflow().createTestdata(this);
    }

    public void registerContextListener(ExecutionContextListener ecl) {
        if (!listeners.contains(ecl))
            listeners.add(ecl);
    }

    private void raiseJobAddedEvent(Job job) {
        for (ExecutionContextListener ecl : listeners) ecl.jobAddedEvent(job);
    }

    private void raiseJobStateChangedEvent(Job job) {
        for (ExecutionContextListener ecl : listeners) ecl.jobStateChangedEvent(job);
    }

    private void raiseDetailedExecutionContextLevelChanged() {
        for (ExecutionContextListener ecl : listeners) ecl.detailedExecutionContextLevelChanged(this);
    }

    @Override
    public String toString() {
        return String.format("Context [%s-%s:%s, %s]", project.getName(), analysis, dataSet, InfoObject.formatTimestamp(timestamp));
    }

    @Override
    public void jobStatusChanged(Job job, JobState oldState, JobState newState) {
        raiseJobStateChangedEvent(job);
    }
}
