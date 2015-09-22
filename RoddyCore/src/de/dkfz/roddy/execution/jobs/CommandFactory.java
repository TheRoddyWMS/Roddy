package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessManager;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommandFactory;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo;
import de.dkfz.roddy.plugins.LibrariesFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Basic factory class for Cluster / Job command creation
 * Currently supported is QSub / PBS. Other cluster systems or custom job submission / execution system are possible.
 *
 * @author michael
 */
public abstract class CommandFactory<C extends Command> {

    private static CommandFactory commandFactory;

    protected Thread updateDaemonThread;

    protected boolean closeThread;
    protected List<C> listOfCreatedCommands = new LinkedList<>();

    public CommandFactory() {
    }

    public static void initializeFactory(boolean fullSetup) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!fullSetup)
            return;

        ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader();
        String commandFactoryClassID = Roddy.getApplicationProperty(Constants.APP_PROPERTY_COMMAND_FACTORY_CLASS, PBSCommandFactory.class.getName());
        Class commandFactoryClass = classLoader.loadClass(commandFactoryClassID);
        Constructor[] c = commandFactoryClass.getConstructors();
        Constructor first = c[0];
        commandFactory = (CommandFactory) first.newInstance();
    }

    public static String createJobName(BaseFile bf, String toolName, boolean reduceLevel) {
        return createJobName(bf, toolName, reduceLevel, null);
    }

    public static String createJobName(BaseFile bf, String toolName, boolean reduceLevel, List<BaseFile> inputFilesForSizeCalculation) {
        ExecutionContext rp = bf.getExecutionContext();
//        String idString = bf.getFileStage().getIDString();
//        if (reduceLevel)
//            idString = bf.getFileStage().decreaseLevel().getIDString();
//        String prefix = rp.getConfiguration().getConfigurationValues().get("jobnamePrefix").toString();
        String runtime = rp.getTimeStampString();
        StringBuilder sb = new StringBuilder();
//        sb.append("run_").append(runtime).append("_");
//        if (prefix != null && !prefix.equals("")) {
//            sb.append(prefix).append("_");
//        }
//        sb.append(bf.getPid()).append("_").append(idString).append("_").append(toolName);
        sb.append("r").append(runtime).append("_").append(bf.getPid()).append("_").append(toolName);
        return sb.toString();
    }

    public static String createJobName(ExecutionContext rp, String postfix) {
        String prefix = rp.getConfiguration().getConfigurationValues().getString("jobnamePrefix");
        String runtime = rp.getTimeStampString();
        StringBuilder sb = new StringBuilder();
//        sb.append("run_").append(runtime).append("_");
//        if (prefix != null && !prefix.equals("")) {
//            sb.append(prefix).append("_");
//        }
//        sb.append(rp.getDataSet().getId()).append("_").append("_").append(postfix);
        sb.append("r").append(runtime).append("_").append(rp.getDataSet().getId()).append("_").append(postfix);
        return sb.toString();
    }

    public static String createJobName(FileGroup fg, String toolName, boolean reduceLevel, List<BaseFile> inputFilesForSizeCalculation) {
        BaseFile bf = (BaseFile) fg.getFilesInGroup().get(0);
        return createJobName(bf, toolName, reduceLevel, inputFilesForSizeCalculation);
    }

    public static CommandFactory getInstance() {

        return commandFactory;
    }

    public abstract void createUpdateDaemonThread(int interval);

    public abstract C createCommand(GenericJobInfo jobInfo);

    public abstract C createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings);

    public C createCommand(Job job, File tool, List<String> dependencies) {
        C c = createCommand(job, job.context, job.jobName, job.getListOfProcessingCommand(), tool, job.getParameters(), dependencies, job.arrayIndices);
        c.setJob(job);
        return c;
    }

    public Command.DummyCommand createDummyCommand(Job job, ExecutionContext run, String jobName, List<String> arraySettings) {
        return new Command.DummyCommand(job, run, jobName, arraySettings != null && arraySettings.size() > 0);
    }

    public abstract JobDependencyID createJobDependencyID(Job job, String jobResult);

    public abstract ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet);

    public abstract ProcessingCommands parseProcessingCommands(String alignmentProcessingOptions);

    public abstract ProcessingCommands getProcessingCommandsFromConfiguration(Configuration configuration, String toolID);

    public abstract ProcessingCommands extractProcessingCommandsFromToolScript(File file);

    public List<C> getListOfCreatedCommands() {
        List<C> newList = new LinkedList<>();
        synchronized (listOfCreatedCommands) {
            newList.addAll(listOfCreatedCommands);
        }
        return newList;
    }

    /**
     * Tries to reverse assemble job information out of an executed command.
     * The format should be [id], [command, i.e. qsub...]
     *
     * @param commandString
     * @return
     */
    public abstract Job parseToJob(ExecutionContext executionContext, String commandString);

    public abstract GenericJobInfo parseGenericJobInfo(ExecutionContext context, String command);

    public abstract JobResult convertToArrayResult(Job arrayChildJob, JobResult parentJobsResult, int arrayIndex);

    /**
     * Queries the status of all jobs in the list.
     *
     * @param jobIDs
     * @return
     */
    public abstract Map<String, JobState> queryJobStatus(List<String> jobIDs);

    public abstract void queryJobAbortion(List<Job> executedJobs);

    public abstract void addJobStatusChangeListener(Job job);

    public abstract String getLogFileWildcard(Job job);

    public abstract boolean compareJobIDs(String jobID, String id);

    public void addCommandToList(C pbsCommand) {
        synchronized (listOfCreatedCommands) {
            listOfCreatedCommands.add(pbsCommand);
        }
    }

    public int waitForJobsToFinish() {
        return 0;
    }

    public void addSpecificSettingsToConfiguration(Configuration configuration) {

    }

    /**
     * Tries to get the log for a running job.
     * Returns an empty array, if the job's state is not RUNNING
     *
     * @param job
     * @return
     */
    public abstract String[] peekLogFile(Job job);

    /**
     * Stores a new job state info to an execution contexts job state log file.
     *
     * @param job
     */
    public void storeJobStateInfo(Job job) {
//        if(job.getJobID() == null)
        String millis = "" + System.currentTimeMillis();
        millis = millis.substring(0, millis.length() - 3);
        ExecutionContext currentContext = job.getExecutionContext();
        String code = "255";
        if (job.getJobState() == JobState.UNSTARTED)
            code = "N";
        else if (job.getJobState() == JobState.ABORTED)
            code = "A";
        else if (job.getJobState() == JobState.OK)
            code = "C";
        else if (job.getJobState() == JobState.FAILED)
            code = "E";
        FileSystemAccessManager.getInstance().appendLineToFile(true, currentContext.getRuntimeService().getNameOfJobStateLogFile(currentContext), String.format("%s:%s:%s", job.getJobID(), code, millis), false);
    }

    public String getLogFileName(Job p) {
        return p.getJobName() + ".o" + p.getJobID();
    }

    public String getLogFileName(Command command) {
        return command.getJob().getJobName() + ".o" + command.getExecutionID().getId();
    }

    public boolean executesWithoutJobSystem() {
        return false;
    }

    public abstract String parseJobID(String commandOutput);

    public abstract String getSubmissionCommand();
}
