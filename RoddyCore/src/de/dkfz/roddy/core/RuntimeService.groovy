package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.LoadedFile
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import groovy.xml.MarkupBuilder

import static de.dkfz.roddy.StringConstants.EMPTY
import static de.dkfz.roddy.StringConstants.SPLIT_COLON
import static de.dkfz.roddy.StringConstants.SPLIT_COMMA

/**
 * A RuntimeService provides path calculations for file access.
 * Project specific runtime service instances should also provide methods for those projects like the data set collection, information retrieval and caching...
 * Runtime services should not be created directly. Instead use getInstance to get a provider.
 *
 * @author michael
 */
@CompileStatic
public abstract class RuntimeService extends CacheProvider {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(RuntimeService.class.getName());
    public static final String FILENAME_RUNTIME_INFO = "versionsInfo.txt"
    public static final String FILENAME_RUNTIME_CONFIGURATION = "runtimeConfig.sh"
    public static final String FILENAME_RUNTIME_CONFIGURATION_XML = "runtimeConfig.xml"
    public static final String FILENAME_REALJOBCALLS = "realJobCalls.txt"
    public static final String FILENAME_REPEATABLEJOBCALLS = "repeatableJobCalls.sh"
    public static final String FILENAME_EXECUTEDJOBS_INFO = "executedJobs.txt"
    public static final String FILENAME_ANALYSES_MD5_OVERVIEW = "zippedAnalysesMD5.txt"
    public static final String DIRECTORY_RODDY_COMMON_EXECUTION = ".roddyExecutionDirectory"
    public static final String DIRNAME_ANALYSIS_TOOLS = "analysisTools"

    /**
     * Loads a list of input data set for the specified analysis.
     * In this method the data set is defined by its main directory (i.e. a pid like ICGC_PCA004)
     *
     * @param analysis
     * @return
     */
    public List<DataSet> loadListOfInputDataSets(Analysis analysis) {
        //TODO: Better logging if the directory could not be read or no files were found.
        def directory = analysis.getInputBaseDirectory()
        return loadDataSetsFromDirectory(directory, analysis)
    }

    public List<DataSet> loadListOfOutputDataSets(Analysis analysis) {
        def directory = analysis.getOutputBaseDirectory()
        return loadDataSetsFromDirectory(directory, analysis)
    }

    public List<DataSet> loadCombinedListOfDataSets(Analysis analysis) {
        List<DataSet> lid = loadListOfInputDataSets(analysis);
        List<DataSet> lod = loadListOfOutputDataSets(analysis);

        //Now combine lid and lod.
        Collection<DataSet> additional = lod.findAll {
            DataSet ds -> !lid.find { DataSet inLid -> inLid.getId() == ds.getId(); };
        }
        lid += additional.each { DataSet ds -> ds.setAsAvailableInOutputOnly(); }
        lid.removeAll { DataSet ds -> ds.getId().startsWith(".roddy"); } //Filter out roddy specific files or folders.
        lid.sort { DataSet a, DataSet b -> a.getId().compareTo(b.getId()); }
        logger.postAlwaysInfo("Found ${lid.size()} datasets in the in- and output directories.")
        return lid;
    }

    /**
     * The method looks for datasets / directories in a path.
     * TODO This is project specific and should be handled as such in the future. Some projects i.e. rely on cohorts and not single datasets.
     *
     * @param directory
     * @param analysis
     * @return
     */
    private static LinkedList<DataSet> loadDataSetsFromDirectory(File directory, Analysis analysis) {
        logger.postRareInfo("Looking for datasets in ${directory.absolutePath}.");

        FileSystemAccessProvider fip = FileSystemAccessProvider.getInstance();

        List<DataSet> results = new LinkedList<DataSet>();
        List<File> files = fip.listDirectoriesInDirectory(directory);

        logger.postSometimesInfo("Found ${files.size()} datasets in ${directory.absolutePath}.");

        File pOut = analysis.getOutputBaseDirectory();
        String pOutStr = pOut.getAbsolutePath();
        for (File f : files) {
            String id = f.getName();
            //TODO: Correct writeConfigurationFile of the output directory! like basepath/dataSet/analysis/
            DataSet dataSet = new DataSet(analysis, id, new File(pOutStr + File.separator + id));
            results.add(dataSet);
        }
        return results;
    }

    public RuntimeService() {
        super("RuntimeService");
    }

    /**
     * The method tries to read back an execution context from a directory structure.
     * In some cases, the method tries to recover information from other sources, if files are missing.
     *
     * @param api
     * @return
     */
    public ExecutionContext readInExecutionContext(AnalysisProcessingInformation api) {
//        Roddy.getInstance().queryJobStatus(new LinkedList<String>());
        FileSystemAccessProvider fip = FileSystemAccessProvider.getInstance();

        File executionDirectory = api.getExecPath();

        //Set the read time stamp before anything else. Otherwise a new Run Directory will be created!
        String[] executionContextDirName = executionDirectory.getName().split(StringConstants.SPLIT_UNDERSCORE);
        String timeStamp = executionContextDirName[1] + StringConstants.UNDERSCORE + executionContextDirName[2];
        String userID = null;
        String analysisID = null;
        if (executionContextDirName.length > 3) {
            //New style with additional information
            userID = executionContextDirName[3];
            analysisID = executionContextDirName[4];
        }

        //ERROR?
        ExecutionContext context = new ExecutionContext(api, InfoObject.parseTimestampString(timeStamp));

        try {
            if (userID == null)
                context.setExecutingUser(fip.getOwnerOfPath(executionDirectory));
            else
                context.setExecutingUser(userID);

            //First, try to read in the executedJobInfo file
            //All necessary information about jobs is stored there.
            List<Job> jobsStartedInContext = readJobInfoFile(context);
            if (jobsStartedInContext == null || jobsStartedInContext.size() == 0) {
                context.addErrorEntry(ExecutionContextError.READBACK_NOEXECUTEDJOBSFILE);
                jobsStartedInContext = new LinkedList<Job>();
            }

            if (jobsStartedInContext.size() == 0) {
                String[] jobCalls = fip.loadTextFile(getNameOfRealCallsFile(context));
                if (jobCalls == null || jobCalls.size() == 0) {
                    context.addErrorEntry(ExecutionContextError.READBACK_NOREALJOBCALLSFILE);
                } else {
                    //TODO Load a list of the previously created jobs and query those using qstat!
                    for (String call : jobCalls) {
                        //TODO. How can we recognize different command factories? i.e. for other cluster systems?
                        Job job = CommandFactory.getInstance().parseToJob(context, call);
                        jobsStartedInContext.add(job);
                    }
                }
            }

            Map<String, JobState> statusList = readInJobStateLogFile(context)

            for (Job job : jobsStartedInContext) {
                if (job == null) continue;
                if (job.jobID == "Unknown")
                    job.setJobState(JobState.FAILED)
                else
                    job.setJobState(JobState.UNSTARTED);
                for (String id : statusList.keySet()) {
                    JobState status = statusList[id];

                    if (!CommandFactory.getInstance().compareJobIDs(job.getJobID(), (id)))
                        continue;
                    job.setJobState(status);
                }
            }


            Map<String, Job> unknownJobs = new LinkedHashMap<>();
            Map<String, Job> possiblyRunningJobs = new LinkedHashMap<>();
            List<String> queryList = new LinkedList<>();
            //For every job which is still unknown or possibly running get the actual state from the cluster
            for (Job job : jobsStartedInContext) {
                if (job.getJobState().isUnknown() || job.getJobState() == JobState.UNSTARTED) {
                    unknownJobs.put(job.getJobID(), job);
                    queryList.add(job.getJobID());
                } else if (job.getJobState() == JobState.STARTED) {
                    possiblyRunningJobs.put(job.getJobID(), job);
                    queryList.add(job.getJobID());
                }
            }

            Map<String, JobState> map = CommandFactory.getInstance().queryJobStatus(queryList);
            for (String jobID : unknownJobs.keySet()) {
                Job job = unknownJobs.get(jobID);
                job.setJobState(map.get(jobID));
                CommandFactory.getInstance().addJobStatusChangeListener(job);
            }
            for (String jobID : possiblyRunningJobs.keySet()) {
                Job job = possiblyRunningJobs.get(jobID);
                if (map.get(jobID) == null) {
                    job.setJobState(JobState.FAILED);
                } else {
                    job.setJobState(map.get(jobID));
                    CommandFactory.getInstance().addJobStatusChangeListener(job);
                }
            }

        } catch (Exception ex) {
            System.out.println(ex);
            for (Object o : ex.getStackTrace())
                System.out.println(o.toString());
        }
        return context;
    }

    /**
     * Read in all states from the job states logfile and set those to all jobsStartedInContext entries.
     * @param context
     * @param jobsStartedInContext
     */
    public Map<String, JobState> readInJobStateLogFile(ExecutionContext context) {
        FileSystemAccessProvider fip = FileSystemAccessProvider.getInstance();
        File jobStatesLogFile = context.getRuntimeService().getNameOfJobStateLogFile(context);
        String[] jobStateList = fip.loadTextFile(jobStatesLogFile);

        if (jobStateList == null || jobStateList.size() == 0) {
            context.addErrorEntry(ExecutionContextError.READBACK_NOJOBSTATESFILE);
            return [:];
        } else {
            //All in which were completed!
            Map<String, JobState> statusList = new LinkedHashMap<>();
            Map<String, Long> timestampList = new LinkedHashMap<>();

            for (String stateEntry : jobStateList) {
                if (stateEntry.startsWith("null"))
                    continue; //Skip null:N:...
                String[] split = stateEntry.split(SPLIT_COLON);
                if (split.length < 2) continue;

                String id = split[0];
                JobState status = JobState.parseJobState(split[1]);
                long timestamp = 0;
                if (split.length == 3)
                    timestamp = Long.parseLong(split[2]);

                //Override if previous timestamp is lower or equal
                if (timestampList.get(id, 0L) <= timestamp) {
                    statusList[id] = status;
                    timestampList[id] = timestamp;
                }
            }

            return statusList;
        }
    }

    /**
     * Read in the real job calls file which contains a detailed description of all started jobs for a context.
     * The real job calls file is an xml file, thus the method itself is not checked on compilation.
     * @param context
     * @return
     */
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private static List<Job> readJobInfoFile(ExecutionContext context) {
        List<Job> jobList = new LinkedList<>();
        final File jobInfoFile = context.getRuntimeService().getNameOfJobInfoFile(context);
        String[] _text = FileSystemAccessProvider.getInstance().loadTextFile(jobInfoFile);
        String text = _text.join(EMPTY);
        if (!text)
            return jobList;

        NodeChild jobinfo = (NodeChild) new XmlSlurper().parseText(text);

        try {
            for (job in jobinfo.jobs.job) {
                String jobID = job.@id.text()
                String jobToolID = job.tool.@toolid.text()
                String jobToolMD5 = job.tool.@md5
                String jobName = job.@name.text()
                Map<String, String> jobsParameters = [:];
                List<LoadedFile> loadedFiles = [];
                List<String> parentJobsIDs = [];

                for (parameter in job.parameters.parameter) {
                    String name = parameter.@name.text();
                    String value = parameter.@value.text();
                    jobsParameters[name] = value;
                }

                for (file in job.filesbyjob.file) {
                    String fileid = file.@id.text();
                    String path = file.@path.text();
                    String cls = file.@class.text();
                    List<String> _parentFiles = Arrays.asList(file.@parentfiles.text().split(SPLIT_COMMA));
                    LoadedFile rb = new LoadedFile(new File(path), jobID, context, _parentFiles, cls);
                    loadedFiles << rb;
                }

                for (dependency in job.dependencies.job) {
                    String id = dependency.@id.text();
                    String fileid = dependency.@fileid.text();
                    String filepath = dependency.@filepath.text();
                    parentJobsIDs << id;
                }
                jobList << new LoadedJob(context, jobName, jobID, jobToolID, jobToolMD5, jobsParameters, loadedFiles, parentJobsIDs);
            }
        } catch (Exception ex) {
            logger.warning("Could not read in xml file " + ex.toString());
            context.addErrorEntry(ExecutionContextError.READBACK_NOEXECUTEDJOBSFILE.expand(ex));
            return null;
        }
        return jobList;
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public String writeJobInfoFile(ExecutionContext context) {
        final File jobInfoFile = context.getRuntimeService().getNameOfJobInfoFile(context);
        final List<Job> executedJobs = context.getExecutedJobs();

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.jobinfo {
            jobs {
                for (Job ej in executedJobs) {
                    if (ej.isFakeJob()) continue; //Skip fake jobs.
                    job(id: ej.getJobID(), name: ej.jobName) {
                        String commandString = ej.getLastCommand() != null ? ej.getLastCommand().toString() : "";
                        calledcommand(command: commandString);
                        tool(id: ej.getToolID(), md5: ej.getToolMD5())
                        parameters {
                            ej.getParameters().each {
                                String k, String v ->
                                    parameter(name: k, value: v)
                            }
                        }
                        filesbyjob {
                            for (BaseFile bf in ej.getFilesToVerify()) {
                                String pfiles = bf.getParentFiles().collect({ BaseFile baseFile -> baseFile.absolutePath.hashCode(); }).join(",");
                                file(class: bf.class.name, id: bf.absolutePath.hashCode(), path: bf.absolutePath, parentfiles: pfiles)
                            }
                        }
                        dependendies {
                            for (BaseFile bf in ej.getParentFiles()) {
                                String depJobID;
                                try {
                                    depJobID = bf.getCreatingJobsResult().getJob().getJobID();
                                } catch (Exception ex) {
                                    depJobID = "Error"
                                }
                                job(id: depJobID, fileid: bf.absolutePath.hashCode(), filepath: bf.absolutePath)
                            }
                        }
                    }
                }
            }
        }
        FileSystemAccessProvider.getInstance().writeTextFile(jobInfoFile, writer.toString(), context);
    }

    public List<String> collectNamesOfRunsForPID(DataSet dataSet) {
        FileSystemAccessProvider fip = FileSystemAccessProvider.getInstance();
        List<File> execList = fip.listDirectoriesInDirectory(dataSet.getOutputBaseFolder(), Arrays.asList(ConfigurationConstants.RODDY_EXEC_DIR_PREFIX + "*"));
        List<String> names = new LinkedList<String>();
        for (File f : execList) {
            names.add(f.getName());
        }
        return names;
    }

    public File getDirectory(String dirName, ExecutionContext run) {
        Analysis analysis = run.getAnalysis();
        Configuration c = analysis.getConfiguration();
        File path = new File(c.getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY).toFile(run).toString() + File.separator + dirName);
//        File path = new File(c.getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY).toFile(run).toString() + File.separator + run.getDataSet() + File.separator + dirName);
//        if (Roddy.getInstance().checkDirectory(path, context.getExecutionContextLevel() == ExecutionContextLevel.RUN || context.getExecutionContextLevel() == ExecutionContextLevel.RERUN)) {
        return path;
//        }
//        logger.warning("Path ${path.absolutePath} cannot be accessed.")
//        return null;
//        throw new RuntimeException("Path " + path + " cannot be accessed.");
    }

    public File getBaseExecutionDirectory(ExecutionContext context) {
        String outPath = getOutputFolderForDataSetAndAnalysis(context.getDataSet(), context.getAnalysis()).absolutePath
        String sep = FileSystemAccessProvider.getInstance().getPathSeparator();
        return new File("${outPath}${sep}roddyExecutionStore");
    }

    public File getExecutionDirectory(ExecutionContext context) {
        if (context.hasExecutionDirectory())
            return context.getExecutionDirectory();

        String outPath = getOutputFolderForDataSetAndAnalysis(context.getDataSet(), context.getAnalysis()).absolutePath
        String sep = FileSystemAccessProvider.getInstance().getPathSeparator();

        String dirPath = "${outPath}${sep}roddyExecutionStore${sep}${ConfigurationConstants.RODDY_EXEC_DIR_PREFIX}${context.getTimeStampString()}_${context.getExecutingUser()}_${context.getAnalysis().getName()}"
        if (context.getExecutionContextLevel() == ExecutionContextLevel.CLEANUP)
            dirPath += "_cleanup"
        return new File(dirPath);
    }

    public File getCommonExecutionDirectory(ExecutionContext context) {
        return new File(getOutputFolderForProject(context).getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + DIRECTORY_RODDY_COMMON_EXECUTION);
    }

    public File getAnalysedMD5OverviewFile(ExecutionContext context) {
        return new File(getCommonExecutionDirectory(context).getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + FILENAME_ANALYSES_MD5_OVERVIEW);
    }

    public File getLoggingDirectory(ExecutionContext run) {
        return getExecutionDirectory(run);
//        String path = getTemporaryDirectory(context).getAbsolutePath();
//        return new File(path + Roddy.getInstance().getPathSeparator() + "logs");
    }

    public File getLockFilesDirectory(ExecutionContext run) {
        File f = new File(getTemporaryDirectory(run), "lockfiles");
        return f;
//        return getDirectory(getExecutionDirFilePrefixString(run) + "temp" + FileSystemAccessManager.getInstance().getPathSeparator() + "lockfiles", run);
    }

    public File getTemporaryDirectory(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + "temp");
    }

    public Date extractDateFromExecutionDirectory(File dir) {
        String path = dir.getAbsolutePath();
//        int index = path.indexOf(ConfigurationConstants.RODDY_EXEC_DIR_PREFIX);
        String[] splitted = dir.getName().split(StringConstants.SPLIT_UNDERSCORE);
        return InfoObject.parseTimestampString(splitted[1] + StringConstants.SPLIT_UNDERSCORE + splitted[2]);
    }

    private String getExecutionDirFilePrefixString(ExecutionContext run) {
        try {
            return String.format("%s%s", run.getExecutionDirectory().getAbsolutePath(), FileSystemAccessProvider.getInstance().getPathSeparator());
        } catch (Exception ex) {
            return String.format("%s%s", run.getExecutionDirectory().getAbsolutePath(), FileSystemAccessProvider.getInstance().getPathSeparator());
        }
    }

    public File getNameOfConfigurationFile(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + FILENAME_RUNTIME_CONFIGURATION);
    }

    public File getNameOfXMLConfigurationFile(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + FILENAME_RUNTIME_CONFIGURATION_XML);
    }

    public File getNameOfRealCallsFile(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + FILENAME_REALJOBCALLS);
    }

    public File getNameOfRepeatableJobCallsFile(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + FILENAME_REPEATABLEJOBCALLS);
    }

    public File getNameOfJobInfoFile(ExecutionContext context) {
        return new File(getExecutionDirFilePrefixString(context) + FILENAME_EXECUTEDJOBS_INFO);
    }

    public File getNameOfJobStateLogFile(ExecutionContext run) {
        return new File(run.getExecutionDirectory().getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + ConfigurationConstants.RODDY_JOBSTATE_LOGFILE);
    }

    public File getNameOfExecCacheFile(Analysis analysis) {
        return new File(analysis.getOutputBaseDirectory().getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + ConfigurationConstants.RODDY_EXEC_CACHE_FILE);
    }

    public File getNameOfRuntimeFile(ExecutionContext context) {
        return new File(getExecutionDirFilePrefixString(context) + FILENAME_RUNTIME_INFO);
    }

    public String extractDataSetIDFromPath(File p, Analysis analysis) {
        //Try new version first, fallback to old version if necessary.
//        if (!Roddy.getFeatureToggleValue(AvailableFeatureToggles.UseOldDataSetIDExtraction)) {
//            def instance = FileSystemAccessManager.getInstance();
//            //TODO Hack! This will only work on Linux systems using bash. Extract the PID from the realjobcalls file. PID is always a parameter.
//            def realJobCalls = new File(p, "realJobCalls.txt")
//            try {
//                if (instance.checkFile(realJobCalls)) {
//                    String line = instance.getLineOfFile(realJobCalls, 0); //Get the first line of a file.
//                    int indexOfPID = line.indexOf("PID=");
//                    String datasetID = line.substring(indexOfPID + 4).split(StringConstants.SPLIT_COMMA)[0].split(StringConstants.SPLIT_WHITESPACE)[0]
//                    return datasetID;
//                }
//            } catch (Exception ex) {
//                //Fallback...
//            }
//        }

        getOutputFolderForAnalysis(analysis);

        String realString = analysis.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY).toFile(analysis).getAbsolutePath();
        String[] split = realString.split("/");
        //TODO can be quite error prone! what if a user puts an additional / somewhere?
        int index = -1;
        for (int i = 0; i < split.length; i++) {
            index++;
            if (split[i].equals('${pid}')) {
                break;
            }
        }
        if (index == -1) {
            return Constants.UNKNOWN;
        }

        split = p.getAbsolutePath().split("/");
        return split[index];

    }

    /**
     * Looks in the projects output directory if the cache file exists. If it does not exist it is created using the find command.
     * TODO Think if this is the right place to have this method! Should this be in the runtime service?
     * @param project
     */
    public List<AnalysisProcessingInformation> readoutExecCacheFile(Analysis analysis) {
        File cacheFile = getNameOfExecCacheFile(analysis);
        String[] execCache = FileSystemAccessProvider.getInstance().loadTextFile(cacheFile);
        List<AnalysisProcessingInformation> processInfo = [];
        List<File> execDirectories = [];
        Arrays.asList(execCache).parallelStream().each {
            String line ->
                if (line == "") return;
                String[] info = line.split(",");
                if (info.length < 2) return;
                File path = new File(info[0]);
                execDirectories << path;
                String dataSetID = analysis.getProject().getRuntimeService().extractDataSetIDFromPath(path, analysis);
                Analysis dataSetAnalysis = analysis.getProject().getAnalysis(info[1])
                DataSet ds = analysis.getDataSet(dataSetID);
                if (dataSetAnalysis == analysis) {
                    AnalysisProcessingInformation api = new AnalysisProcessingInformation(dataSetAnalysis, ds, path);
                    if (info.length > 3) {
                        String user = info[3];
                        api.setExecutingUser(user);
                    }
                    synchronized (processInfo) {
                        processInfo << api;
                    }
                }
        }
        if (processInfo.size() == 0) {
            logger.postSometimesInfo("No process info objects could be matched for ${execCache.size()} lines in the cache file.")
            //TODO Possible input output directory mismatch or configuration error!
        }
        processInfo.sort { AnalysisProcessingInformation p1, AnalysisProcessingInformation p2 -> p1.getExecPath().absolutePath.compareTo(p2.getExecPath().getAbsolutePath()); }
        return processInfo;
    }

    public File getLogFileForJob(Job job) {
        //Returns the log files path of the job.
        File f = new File(job.getExecutionContext().getExecutionDirectory(), CommandFactory.getInstance().getLogFileName(job));
    }

    public File getLogFileForCommand(Command command) {
        //Nearly the same as for the job but with a process id
        File f = new File(getExecutionDirectory(command.getExecutionContext()), CommandFactory.getInstance().getLogFileName(command));
    }

    public boolean hasLogFileForJob(Job job) {
        return getLogFileForJob(job) != null;
    }

    public List<File> getAdditionalLogFilesForContext(ExecutionContext executionContext) {
        File loggingDirectory = executionContext.getLoggingDirectory();
        List<File> files = FileSystemAccessProvider.getInstance().listFilesInDirectory(loggingDirectory);
        List<File> notUsed = executionContext.getLogFilesForExecutedJobs();
        for (File f : notUsed)
            files.remove(f);
        return files;
    }

    public List<File> getResultsFilesForDataSetAndAnalysis(DataSet dataSet, Analysis analysis) {
        File outFolder = getOutputFolderForDataSetAndAnalysis(dataSet, analysis);
        return [];
    }

    public File getInputFolderForAnalysis(Analysis analysis) {
        return analysis.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY).toFile(analysis);
    }

    public File getOutputFolderForProject(ExecutionContext context) {
        return context.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY).toFile(context);
    }

    public File getOutputFolderForAnalysis(Analysis analysis) {
        return analysis.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY).toFile(analysis);
    }

    public File getInputFolderForDataSetAndAnalysis(DataSet dataSet, Analysis analysis) {
        File analysisInFolder = new File(getInputFolderForAnalysis(analysis).absolutePath + FileSystemAccessProvider.instance.getPathSeparator() + dataSet.getId());
        return analysisInFolder;
    }

    public File getOutputFolderForDataSetAndAnalysis(DataSet dataSet, Analysis analysis) {
        getOutputFolderForAnalysis(analysis);
        File analysisOutFolder = analysis.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY).toFile(analysis, dataSet);
        return analysisOutFolder;
    }

    public File getAnalysisToolsDirectory(ExecutionContext executionContext) {
        File analysisToolsDirectory = new File(getExecutionDirectory(executionContext), DIRNAME_ANALYSIS_TOOLS);
        return analysisToolsDirectory;
    }

    public abstract Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID)

    public abstract String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel)

    public abstract boolean isFileValid(BaseFile baseFile)
}
