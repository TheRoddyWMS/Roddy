package de.dkfz.roddy.execution.jobs.cluster.pbs;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.BufferUnit;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider
import de.dkfz.roddy.execution.jobs.cluster.ClusterCommandFactory;
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo;
import de.dkfz.roddy.tools.*;
import de.dkfz.roddy.config.*;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.io.ExecutionResult;
import de.dkfz.roddy.execution.jobs.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static de.dkfz.roddy.StringConstants.*;

/**
 * A job submission implementation for standard PBS systems.
 *
 * @author michael
 */
@groovy.transform.CompileStatic
public class PBSCommandFactory extends ClusterCommandFactory<PBSCommand> {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(PBSCommandFactory.class.getName());

    public static final String PBS_JOBSTATE_RUNNING = "R";
    public static final String PBS_JOBSTATE_HOLD = "H";
    public static final String PBS_JOBSTATE_QUEUED = "Q";
    public static final String PBS_COMMAND_QUERY_STATES = "qstat -t";
    public static final String PBS_COMMAND_DELETE_JOBS = "qdel";
    public static final String PBS_LOGFILE_WILDCARD = "*.o";

    public PBSCommandFactory() {
        //Create a daemon thread which automatically calls queryJobStatus from time to time...
        int interval = Integer.parseInt(Roddy.getApplicationProperty("commandFactoryUpdateInterval", "60"));
        createUpdateDaemonThread(interval);
    }

    @Override
    public void createUpdateDaemonThread(int _interval) {
        final long interval = _interval * 1000000000L;

        if (updateDaemonThread != null) {
            closeThread = true;
            try {
                updateDaemonThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            updateDaemonThread = null;
        }

        updateDaemonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long nanoTime = System.nanoTime();
                boolean intervalReached = true;
                while (!closeThread) {
                    long currentTime = System.nanoTime();
                    long diff = currentTime - nanoTime;
                    while (diff > 0) {
                        nanoTime += interval;
                        diff -= interval;
                        intervalReached = true;
                    }
                    if (intervalReached) {
                        try {
                            updateJobStatus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        intervalReached = false;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        });
        updateDaemonThread.setName("PBS command factory update daemon.");
        updateDaemonThread.setDaemon(true);
        updateDaemonThread.start();
    }

    @Override
    public PBSCommand createCommand(GenericJobInfo jobInfo) {
        Job job = new Job(jobInfo.getExecutionContext(), jobInfo.getJobName(), jobInfo.getToolID(), new LinkedHashMap<String, Object>(jobInfo.getParameters()));
        PBSCommand pbsCommand = new PBSCommand(job, jobInfo.getExecutionContext(), ExecutionService.getInstance(), job.getJobName(), null, job.getParameters(), null, jobInfo.getParentJobIDs(), jobInfo.getExecutionContext().getConfiguration().getProcessingToolPath(jobInfo.getExecutionContext(), jobInfo.getToolID()).getAbsolutePath());
        return pbsCommand;
    }

    @Override
    public PBSCommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
        PBSCommand pbsCommand = new PBSCommand(job, run, ExecutionService.getInstance(), jobName, processingCommands, parameters, arraySettings, dependencies, tool.getAbsolutePath());
        return pbsCommand;
    }

    private Map<String, Boolean> mapOfInitialQueries = new LinkedHashMap<>();

    @Override
    public int waitForJobsToFinish() {
        logger.info("The user requested to wait for all jobs submitted by this process to finish.");
        List<String> ids = new LinkedList<>();
        List<ExecutionContext> listOfContexts = new LinkedList<>();
        synchronized (listOfCreatedCommands) {
            for (Object _command : listOfCreatedCommands) {
                PBSCommand command = (PBSCommand) _command;
                if (command.getJob() instanceof Job.FakeJob)
                    continue;
                ids.add(command.getExecutionID().getShortID());
                ExecutionContext context = command.getExecutionContext();
                if (!listOfContexts.contains(context)) {
                    listOfContexts.add(context);
                }
            }
        }

        boolean isRunning = true;
        while (isRunning) {

            isRunning = false;
            Map<String, JobState> stringJobStateMap = queryJobStatus(ids, true);
            if (logger.isVerbosityHigh()) {
                for (String s : stringJobStateMap.keySet()) {
                    if (stringJobStateMap.get(s) != null)
                        System.out.println(s + " = " + stringJobStateMap.get(s));
                }
            }
            for (JobState js : stringJobStateMap.values()) {
                if (js == null) //Only one job needs to be active.
                    continue;

                if (js.isPlannedOrRunning()) {
                    isRunning = true;
                    break;
                }
            }
            if (isRunning) {
                try {
                    logger.info("Waiting for jobs to finish.");
                    Thread.sleep(5000); //Sleep one minute until the next query.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("Finished waiting");
            }
        }
        int errnousJobs = 0;
        for (ExecutionContext context : listOfContexts) {
            for (Job job : context.getExecutedJobs())
                if (job.getJobID() != null) errnousJobs++; //Skip null jobs.

            Map<String, JobState> statesMap = context.getRuntimeService().readInJobStateLogFile(context);
            statesMap.each {
                String s, JobState integer ->
                    if (integer == 0)
                        errnousJobs--;
                    else
                        logger.info("Job " + s + " exited with an error.");
            }
            int unknown = context.getExecutedJobs().size() - statesMap.size();
            if (unknown > 0) {
                logger.info("There were " + unknown + " jobs with an unknown state.");
//                for (String s : statesMap.keySet()) {
//                    logger.info("\t" + s + " => " + statesMap.get(s));
//                }
            }
        }

        return errnousJobs;
    }

    @Override
    public JobDependencyID createJobDependencyID(Job job, String jobResult) {
        return new PBSJobDependencyID(job, jobResult);
    }

    @Override
    public ProcessingCommands parseProcessingCommands(String processingString) {
        return convertPBSResourceOptionsString(processingString);
    }

    @Override
    public ProcessingCommands getProcessingCommandsFromConfiguration(Configuration configuration, String toolID) {
        ToolEntry toolEntry = configuration.getTools().getValue(toolID);
        if (toolEntry.hasResourceSets()) {
            return convertResourceSet(configuration, toolEntry.getResourceSet(configuration));
        }
        String resourceOptions = configuration.getConfigurationValues().getString(getResourceOptionsPrefix() + toolID, "");
        if (resourceOptions.trim().length() == 0)
            return null;

        return convertPBSResourceOptionsString(resourceOptions);
    }

    @Override
    public ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet) {
        StringBuilder sb = new StringBuilder();
        if (resourceSet.isMemSet()) {
            Float memo = resourceSet.getMem();
            int memoryInMB = (int) (memo * 1024);
            sb.append(" -l mem=").append(memoryInMB).append("m");
        }
        if (resourceSet.isCoresSet() && resourceSet.isNodesSet()) {
            String enforceSubmissionNodes = configuration.getConfigurationValues().getString(CVALUE_ENFORCE_SUBMISSION_TO_NODES, null);
            if (!enforceSubmissionNodes) {
                sb.append(" -l nodes=").append(resourceSet.getNodes()).append(":ppn=").append(resourceSet.getCores());
                if (resourceSet.isAdditionalNodeFlagSet()) {
                    sb.append(":").append(resourceSet.getAdditionalNodeFlag());
                }
            } else {
                String[] nodes = enforceSubmissionNodes.split(StringConstants.SPLIT_SEMICOLON);
                nodes.each {
                    String node ->
                        sb.append(" -l nodes=").append(node).append(":ppn=").append(resourceSet.getCores());
                }
            }
        }
        if (resourceSet.isWalltimeSet()) {
            sb.append(" -l walltime=").append(resourceSet.getWalltime()).append(":00:00");
        }
        if (resourceSet.isStorageSet()) {
//            sb.append(" -l mem=").append(resourceSet.getMem()).append("g");
        }
        if (resourceSet.isQueueSet()) {
            sb.append(" -q ").append(resourceSet.getQueue());
        }
        return new PBSResourceProcessingCommand(sb.toString());
    }

    public String getResourceOptionsPrefix() {
        return "PBSResourceOptions_";
    }

    static ProcessingCommands convertPBSResourceOptionsString(String processingString) {
        return new PBSResourceProcessingCommand(processingString);
    }

    @Override
    public ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        String[] text = RoddyIOHelperMethods.loadTextFile(file);

        List<String> lines = new LinkedList<String>();
        boolean preambel = true;
        for (String line : text) {
            if (preambel && !line.startsWith("#PBS"))
                continue;
            preambel = false;
            if (!line.startsWith("#PBS"))
                break;
            lines.add(line);
        }

//        #PBS -l walltime=8:00:00
//        #PBS -l nodes=1:ppn=12:lsdf
//        #PBS -S /bin/bash
//        #PBS -l mem=3600m
//        #PBS -m a

//        #PBS -l walltime=50:00:00
//        #PBS -l nodes=1:ppn=6:lsdf
//        #PBS -l mem=52g
//        #PBS -m a
        StringBuilder processingOptionsStr = new StringBuilder();
        for (String line : lines) {
            processingOptionsStr.append(" ").append(line.substring(5));
        }
        return convertPBSResourceOptionsString(processingOptionsStr.toString());
    }

    @Override
    public Job parseToJob(ExecutionContext executionContext, String commandString) {

        GenericJobInfo jInfo = parseGenericJobInfo(executionContext, commandString);
        Job job = new ReadOutJob(executionContext, jInfo.getJobName(), jInfo.getToolID(), jInfo.getID(), jInfo.getParameters(), jInfo.getParentJobIDs());

        //Autmatically get the status of the job and if it is planned or running add it as a job status listener.
        String shortID = job.getJobID();
        job.setJobState(queryJobStatus(Arrays.asList(shortID)).get(shortID));
        if (job.getJobState().isPlannedOrRunning()) addJobStatusChangeListener(job);

        return job;
    }

    @Override
    public GenericJobInfo parseGenericJobInfo(ExecutionContext executionContext, String commandString) {
        commandString = commandString.trim();
        String[] split = commandString.split(", ");
        String id = split[0];
        commandString = split[1].trim();

        if (!commandString.startsWith("qsub")) return null;
        int skriptIndex = -1;
        final String DBL_QUOTE = "\"";
        if (commandString.endsWith(DBL_QUOTE)) {
            skriptIndex = commandString[0..-2].lastIndexOf(DBL_QUOTE);
        } else {
            skriptIndex = commandString.lastIndexOf(" ");
        }

        String skript = commandString[skriptIndex..-1].trim();
        commandString = commandString[5..skriptIndex].trim();
        String[] options = (" " + commandString).split(" [-]");   //Put " " in front of the string so that every command can be recognized properly beginning with the first one.
        String jobName = "not readable";
        String toolID = "";
        String walltime;
        String memory;
        BufferUnit bufferUnit = BufferUnit.G;
        String cores;
        String nodes;
        String queue;
        String otherSettings;
        List<String> dependencies = new LinkedList<>();

        Map<String, String> parameters = new LinkedHashMap<>();
        for (String option : options) {
            if (option.trim().length() == 0) continue; //Sometimes empty options occur.
            String var = option.substring(0, 1);
            String parms = option.substring(1).trim();
            if (var.equals("N")) {
                jobName = parms;
                try {
                    toolID = jobName.substring(jobName.lastIndexOf("_") + 1);
                } catch (Exception e) {
                    //TODO Jobnames not read out properly in some cases
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else if (var.equals("v")) {
                String[] variables = parms.split(SPLIT_COMMA);
                for (String variable : variables) {
                    String[] varSplit = variable.split(SPLIT_EQUALS);
                    String header = varSplit[0];
                    String value = Constants.UNKNOWN;
                    if (varSplit.length > 1)
                        value = varSplit[1];
                    parameters.put(header, value);
                }
            } else if (var.equals("l")) { //others
                def splitParms = parms.split("[,]");
                splitParms.each {
                    String parm ->
                        String parmID = parm.split(StringConstants.SPLIT_EQUALS)[0];
                        String parmVal = parm.split(StringConstants.SPLIT_EQUALS)[1];
                        if (parmID == "mem") {
                            bufferUnit = BufferUnit.valueOf(parmVal[-1]);
                            memory = parmVal[0..-2];
                        } else if (parmID == "walltime") {
                            String[] splitParm = parmVal.split(StringConstants.SPLIT_COLON);
                            //TODO Increase overall granularity for walltime and mem in config.
                        } else if (parmID == "nodes") {
                            String[] splitParm = parmVal.split(StringConstants.SPLIT_COLON);
                            nodes = splitParm[0];
                            cores = splitParm[1];
                        }
                }
            } else if (var.equals("W")) {
                if (parms.startsWith("depend")) {
                    def deps = parms[7..-1].split("[:]");
//                    println deps;
                    if (deps[0] != "afterok")
                        println "Not supported: " + deps[0];
//                    println deps[1 .. -1]
                    try {
                        dependencies.addAll(deps[1..-1]);
                    } catch (Exception ex) {
                        println(parms)
                        println(ex);
                    }
                }
            }
        }

        GenericJobInfo jInfo = new GenericJobInfo(executionContext, jobName, toolID, id, parameters, dependencies);
        jInfo.setCpus(cores);
        jInfo.setNodes(nodes);
        jInfo.setMemory(memory);
        jInfo.setMemoryBufferUnit(bufferUnit);
        jInfo.setWalltime(walltime);
//        jInfo.set
        return jInfo;
    }

    @Override
    public JobResult convertToArrayResult(Job arrayChildJob, JobResult parentJobsResult, int arrayIndex) {
        String newID;
        if (parentJobsResult.getJobID() instanceof JobDependencyID.FakeJobID) {
            newID = parentJobsResult.getJobID().getId() + "[" + arrayIndex + "]";
        } else {
            String parentID = (parentJobsResult.getJobID()).getId();
            String[] split = parentID.split("\\[");
            newID = split[0] + "[" + arrayIndex + split[1];
        }
        PBSJobDependencyID pjid = new PBSJobDependencyID(arrayChildJob, newID);
        return new JobResult(parentJobsResult.getRun(), parentJobsResult.getCommand(), pjid, parentJobsResult.isWasExecuted(), false, parentJobsResult.getToolID(), parentJobsResult.getJobParameters(), parentJobsResult.getParentFiles());
    }

    private static final ReentrantLock cacheLock = new ReentrantLock();

    private static ExecutionResult cachedExecutionResult;

    protected Map<String, JobState> allStates = new LinkedHashMap<String, JobState>();

    protected Map<String, Job> jobStatusListeners = new LinkedHashMap<>();

    /**
     * Queries the jobs states.
     *
     * @return
     */
    protected void updateJobStatus() {
        updateJobStatus(false);
    }

    protected String getQueryCommand() {
        return PBS_COMMAND_QUERY_STATES;
    }

    protected void updateJobStatus(boolean forceUpdate) {

        if (!ExecutionService.getInstance().isAvailable())
            return;

        String queryCommand = getQueryCommand();

        if (Roddy.queryOnlyStartedJobs() && listOfCreatedCommands.size() < 10) {
            for (Object _l : listOfCreatedCommands) {
                PBSCommand listOfCreatedCommand = (PBSCommand) _l;
                queryCommand += " " + listOfCreatedCommand.getJob().getJobID();
            }
        }
        if (Roddy.isTrackingOfUserJobsEnabled())
            queryCommand += " -u " + FileSystemInfoProvider.getInstance().callWhoAmI() + " ";


        Map<String, JobState> allStatesTemp = new LinkedHashMap<String, JobState>();
        ExecutionResult er;
        List<String> resultLines = new LinkedList<String>();
        cacheLock.lock();
        try {
            if (forceUpdate || cachedExecutionResult == null || cachedExecutionResult.getAgeInSeconds() > 30) {
                cachedExecutionResult = ExecutionService.getInstance().execute(queryCommand.toString());
            }
        } finally {
        }
        er = cachedExecutionResult;
        resultLines.addAll(er.resultLines);

        cacheLock.unlock();

        if (er.successful) {
            if (resultLines.size() > 2) {
//                resultLines.remove(0);
//                resultLines.remove(0);

                for (String line : resultLines) {
                    //if (line.equals("Finished")) continue;
                    line = line.trim();
                    if (line.length() == 0) continue;
                    if (!RoddyConversionHelperMethods.isInteger(line.substring(0, 1)))
                        continue; //Filter out lines which have been missed which do not start with a number.

                    //TODO Put to a common class, is used multiple times.
                    line = line.replaceAll("\\s+", " ").trim();       //Replace multi white space with single whitespace
                    String[] split = line.split(" ");
                    final int ID = getPositionOfJobID();
                    final int JOBSTATE = getPositionOfJobState();
                    if (logger.isVerbosityHigh()) {
                        System.out.println("QStat Job line: " + line);
                        System.out.println("	Entry in arr[" + ID + "]: " + split[ID]);
                        System.out.println("    Entry in arr[" + JOBSTATE + "]: " + split[JOBSTATE]);
                    }

                    String[] idSplit = split[ID].split("[.]");
                    //(idSplit.length <= 1) continue;
                    String id = idSplit[0];
                    JobState js = JobState.UNKNOWN;
                    if (split[JOBSTATE].equals(getStringForRunningJob()))
                        js = JobState.RUNNING;
                    if (split[JOBSTATE].equals(getStringForJobOnHold()))
                        js = JobState.HOLD;
                    if (split[JOBSTATE].equals(getStringForQueuedJob()))
                        js = JobState.QUEUED;

                    allStatesTemp.put(id, js);
                    if (logger.isVerbosityHigh())
                        System.out.println("   Extracted state: " + js.toString());
                }
            }

            //Create a local cache of jobstate logfile entries.
            Map<ExecutionContext, Map<String, JobState>> map = new LinkedHashMap<>();
            List<Job> removejobs = new LinkedList<>();
            synchronized (jobStatusListeners) {
                for (String id : jobStatusListeners.keySet()) {
                    JobState js = allStatesTemp.get(id);
                    Job job = jobStatusListeners.get(id);

                    if (js == JobState.UNKNOWN_SUBMITTED) {
                        // If the state is unknown and the job is not running anymore it is counted as failed.
                        job.setJobState(JobState.FAILED);
                        removejobs.add(job);
                        continue;
                    }

                    if (JobState._isPlannedOrRunning(js)) {
                        job.setJobState(js);
                        continue;
                    }

                    if (job.getJobState() == JobState.OK || job.getJobState() == JobState.FAILED)
                        continue; //Do not query jobs again if their status is already final. TODO Remove final jobs from listener list?

                    if (js == null || js == JobState.UNKNOWN) {
                        //Read from jobstate logfile.
                        try {
                            ExecutionContext executionContext = job.getExecutionContext();
                            if (!map.containsKey(executionContext))
                                map.put(executionContext, executionContext.readJobStateLogFile());

                            JobState jobsCurrentState = null;
                            Map<String, JobState> statesMap = map.get(executionContext);
                            if (job.getRunResult() != null) {
                                jobsCurrentState = statesMap.get(job.getRunResult().getJobID().getId());
                            } else { //Search within entries.
                                for (String s : statesMap.keySet()) {
                                    if (s.startsWith(id)) {
                                        jobsCurrentState = statesMap.get(s);
                                        break;
                                    }
                                }
                            }
                            js = jobsCurrentState;
                        } catch (Exception ex) {
                            //Could not read out job state from file
                        }
                    }
                    job.setJobState(js);
                }
            }
        }
        cacheLock.lock();
        allStates.clear();
        allStates.putAll(allStatesTemp);
        cacheLock.unlock();
    }

    protected String getStringForQueuedJob() {
        return PBS_JOBSTATE_QUEUED;
    }

    protected String getStringForJobOnHold() {
        return PBS_JOBSTATE_HOLD;
    }

    protected String getStringForRunningJob() {
        return PBS_JOBSTATE_RUNNING;
    }

    protected int getPositionOfJobID() {
        return 0;
    }

    /**
     * Return the position of the status string within a stat result line. This changes if -u USERNAME is used!
     *
     * @return
     */
    protected int getPositionOfJobState() {
        if (Roddy.isTrackingOfUserJobsEnabled())
            return 9;
        return 4;
    }

    public Map<String, JobState> queryJobStatus(List<String> jobIDs) {
        return queryJobStatus(jobIDs, false);
    }

    public Map<String, JobState> queryJobStatus(List<String> jobIDs, boolean forceUpdate) {

        if (allStates == null || forceUpdate)
            updateJobStatus(forceUpdate);

        Map<String, JobState> queriedStates = new LinkedHashMap<String, JobState>();

        for (String id : jobIDs) {
            cacheLock.lock();
            JobState state = allStates.get(id);
            cacheLock.unlock();
            queriedStates.put(id, state);
        }

        return queriedStates;
    }

    @Override
    public void queryJobAbortion(List<Job> executedJobs) {
        StringBuilder queryCommand = new StringBuilder(PBS_COMMAND_DELETE_JOBS);

        for (Job job : executedJobs)
            queryCommand.append(" ").append(job.getJobID());

        ExecutionService.getInstance().execute(queryCommand.toString(), false);
    }

    @Override
    public void addJobStatusChangeListener(Job job) {
        synchronized (jobStatusListeners) {
            jobStatusListeners.put(job.getJobID(), job);
        }
    }

    @Override
    public String getLogFileWildcard(Job job) {
        String id = job.getJobID();
        String searchID = id;
        if (id == null) return null;
        if (id.contains("[]"))
            return "";
        if (id.contains("[")) {
            String[] split = id.split("\\]")[0].split("\\[");
            searchID = split[0] + "-" + split[1];
        }
        return PBS_LOGFILE_WILDCARD + searchID;
    }

//    /**
//     * Returns the path to the jobs logfile (if existing). Otherwise null.
//     * Throws a runtime exception if more than one logfile exists.
//     *
//     * @param readOutJob
//     * @return
//     */
//    @Override
//    public File getLogFileForJob(ReadOutJob readOutJob) {
//        List<File> files = Roddy.getInstance().listFilesInDirectory(readOutJob.context.getExecutionDirectory(), Arrays.asList("*" + readOutJob.getJobID()));
//        if (files.size() > 1)
//            throw new RuntimeException("There should only be one logfile for this job: " + readOutJob.getJobID());
//        if (files.size() == 0)
//            return null;
//        return files.get(0);
//    }

    @Override
    public boolean compareJobIDs(String jobID, String id) {
        if (jobID.length() == id.length()) {
            return jobID.equals(id);
        } else {
            String id0 = jobID.split("[.]")[0];
            String id1 = id.split("[.]")[0];
            return id0.equals(id1);
        }
    }

    @Override
    public void addSpecificSettingsToConfiguration(Configuration configuration) {
//        <!--<cvalue name="RODDY_JOBID" value="${PBS_JOBID}" />-->
//        <!--<cvalue name="RODDY_JOBARRAYINDEX" value="${PBS_ARRAYID}" />-->
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_JOBID", '${PBS_JOBID}'));
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_JOBARRAYINDEX", '${PBS_ARRAYID}'));
        configuration.getConfigurationValues().add(new ConfigurationValue('RODDY_SCRATCH', '${PBS_SCRATCH_DIR}/${PBS_JOBID}'));
    }

    @Override
    public String[] peekLogFile(Job job) {
        String user = FileSystemInfoProvider.getInstance().callWhoAmI();
        String id = job.getJobID();
        String searchID = id;
        if (id.contains(SBRACKET_LEFT)) {
            String[] split = id.split(SPLIT_SBRACKET_RIGHT)[0].split(SPLIT_SBRACKET_LEFT);
            searchID = split[0] + MINUS + split[1];
        }
        String cmd = String.format("jobHost=`qstat -f %s  | grep exec_host | cut -d \"/\" -f 1 | cut -d \"=\" -f 2`; ssh %s@${jobHost: 1} 'cat /opt/torque/spool/spool/*'%s'*'", id, user, searchID);
        ExecutionResult executionResult = ExecutionService.getInstance().execute(cmd);
        if (executionResult.successful)
            return executionResult.resultLines.toArray(new String[0]);
        return new String[0];
    }

    @Override
    public String parseJobID(String commandOutput) {
        return commandOutput;
    }

    @Override
    public String getSubmissionCommand() {
        return PBSCommand.QSUB;
    }
}
