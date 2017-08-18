/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.execution.io.BaseMetadataTable
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic
import org.apache.commons.io.filefilter.WildcardFileFilter

/**
 * A RuntimeService provides path calculations for file access.
 * Project specific runtime service instances should also provide methods for those projects like the data set collection, information retrieval and caching...
 * Runtime services should not be created directly. Instead use getInstance to get a provider.
 *
 * @author michael
 */
@CompileStatic
public class RuntimeService {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(RuntimeService.class.getSimpleName());
    public static final String FILENAME_RUNTIME_INFO = "versionsInfo.txt"
    public static final String FILENAME_RUNTIME_CONFIGURATION = "runtimeConfig.sh"
    public static final String FILENAME_RUNTIME_CONFIGURATION_XML = "runtimeConfig.xml"
    public static final String FILENAME_REALJOBCALLS = "realJobCalls.txt"
    public static final String FILENAME_REPEATABLEJOBCALLS = "repeatableJobCalls.sh"
    public static final String FILENAME_EXECUTEDJOBS_INFO = "executedJobs.txt"
    public static final String FILENAME_ANALYSES_MD5_OVERVIEW = "zippedAnalysesMD5.txt"
    public static final String DIRECTORY_RODDY_COMMON_EXECUTION = ".roddyExecutionStore"
    public static final String DIRNAME_RESOURCES = "resources"
    public static final String DIRNAME_ANALYSIS_TOOLS = "analysisTools"
    public static final String DIRNAME_BRAWLWORKFLOWS = "brawlworkflows"
    public static final String DIRNAME_CONFIG_FILES = "configurationFiles"
    public static final String RODDY_CENTRAL_EXECUTION_DIRECTORY = "RODDY_CENTRAL_EXECUTION_DIRECTORY"

    public RuntimeService() {
        logger.warning("Reading in jobs is not fully enabled! See RuntimeService readInExecutionContext(). The method does not reconstruct parent files and dependencies.")
    }

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

    public List<DataSet> loadCombinedListOfPossibleDataSets(Analysis analysis) {

        if (Roddy.isMetadataCLOptionSet()) {

            BaseMetadataTable table = MetadataTableFactory.getTable(analysis)
            List<String> _datasets = table.listDatasets();
            String pOut = analysis.getOutputBaseDirectory().getAbsolutePath() + File.separator;
            return _datasets.collect { new DataSet(analysis, it, new File(pOut + it), table); }

        } else {

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

    /**
     * A small cache which prevents Roddy from querying the file system for datasets several times.
     */
    private Map<Analysis, List<DataSet>> _listOfPossibleDataSetsByAnalysis = [:]

    /**
     * Fetches information to the projects data sets and various analysis related additional information for those data sets.
     * Retrieves the data sets for this analysis from it's project and appends the data for this analysis (if not already set).
     * <p>
     * getListOfPossibleDataSets without a parameter
     *
     * @return
     */
    public List<DataSet> getListOfPossibleDataSets(Analysis analysis, boolean avoidRecursion = false) {

        if (_listOfPossibleDataSetsByAnalysis[analysis] == null)
            _listOfPossibleDataSetsByAnalysis[analysis] = loadCombinedListOfPossibleDataSets(analysis);

        if (!avoidRecursion) {
            List<AnalysisProcessingInformation> previousExecs = readoutExecCacheFile(analysis);

            for (DataSet ds : _listOfPossibleDataSetsByAnalysis[analysis]) {
                for (AnalysisProcessingInformation api : previousExecs) {
                    if (api.getDataSet() == ds) {
                        ds.addProcessingInformation(api);
                    }
                }
                analysis.getProject().updateDataSet(ds, analysis);
            }
        }

        return _listOfPossibleDataSetsByAnalysis[analysis];
    }


    boolean validateCohortDataSetLoadingString(String s) {
        String PID = '[\\w*?_-]+'
        // First part PID, followed by 0 to n ;PID
        String COHORT = "c[:]${PID}(;${PID}){0,}"

        // First part COHORT, followed by 0 to n |COHORT
        def regex = "s\\[${COHORT}(\\|$COHORT){0,}\\]"

        return s ==~ regex
    }

    List<DataSet> loadDatasetsWithFilter(Analysis analysis, List<String> pidFilters, boolean suppressInfo = false) {
        if (analysis.configuration.configurationValues.getBoolean("loadCohortDatasets", false)) {
            return loadCohortDatasetsWithFilter(analysis, pidFilters, suppressInfo)
        } else {
            loadStandardDatasetsWithFilter(analysis, pidFilters, suppressInfo)
        }
    }

    /** There are non-cohort (=standard) datasets and cohort datasets */
    List<DataSet> loadStandardDatasetsWithFilter(Analysis analysis, List<String> pidFilters, boolean suppressInfo = false) {
        if (pidFilters == null || pidFilters.size() == 0 || pidFilters.size() == 1 && pidFilters.get(0).equals("[ALL]")) {
            pidFilters = Arrays.asList("*");
        }
        List<DataSet> listOfDataSets = getListOfPossibleDataSets(analysis);
        return selectDatasetsFromPattern(analysis, pidFilters, listOfDataSets, suppressInfo);
    }

    /**
     * In difference to the normal behaviour, the method / workflow loads with supercohorts and cohorts thus using a different
     * dataset id format on the command line:
     * run p@a s:[c:DS0;DS1;...;DSn|c:DS2;...]
     *
     * comma separation stays
     * a cohort must be marked with c: in front of it. That is just to make the call clear and has no actual use!
     * entries in a cohort are separated by semicolon!
     * Remark, that order matters! The first entry of a cohor is the major cohort dataset which will e.g. be used for file
     * output, if applicable.
     *
     * TODO Identify cases, where the matching mechanism should fail! Like e.g. when a dataset is missing but requested.
     * - Using wildcards? At least one dataset should be matched for each wildcard entry.
     * - Using no wildcards? There must be one dataset match.
     *
     * @param analysis
     * @param pidFilters
     * @param suppressInfo
     * @return
     */
    List<DataSet> loadCohortDatasetsWithFilter(Analysis analysis, List<String> pidFilters, boolean suppressInfo) {
        List<DataSet> listOfDataSets = getListOfPossibleDataSets(analysis);
        List<SuperCohortDataSet> datasets

        if (!checkCohortDataSetIdentifiers(pidFilters)) return []

        boolean error = false

        // Checks are all done, now get the datasets..
        datasets = pidFilters.collect { String superCohortDescription ->

            // Remove leading s[ and trailing ], split by |
            List<CohortDataSet> cohortDatasets = superCohortDescription[2..-2].split("[|]").collect { String cohortDescription ->

                // Remove leading c:, split by ;
                String[] datasetFilters = cohortDescription[2..-1].split(StringConstants.SPLIT_SEMICOLON);

                List<DataSet> dList = collectDataSetsForCohort(datasetFilters, analysis, listOfDataSets)

                // Sort the list, but keep the primary set the primary set.
                DataSet primaryDataSet = dList[0];
                dList = dList.sort().unique() as ArrayList<DataSet>
                dList.remove(primaryDataSet)
                dList.add(0, primaryDataSet)
                if (primaryDataSet && dList)
                    return new CohortDataSet(analysis, cohortDescription, primaryDataSet, dList)
                else
                    return null;
            }.findAll { it } as List<CohortDataSet>
            if (cohortDatasets)
                return new SuperCohortDataSet(analysis, superCohortDescription, cohortDatasets)
            else
                return (SuperCohortDataSet) null;
        }.findAll { it }
        if (error)
            return []
        return datasets as List<DataSet>;
    }

    private boolean checkCohortDataSetIdentifiers(List<String> pidFilters) {
        // First some checks, if the cohort loading string was set properly.
        boolean foundFaulty = false;
        for (filter in pidFilters) {
            boolean faulty = !validateCohortDataSetLoadingString(filter)
            if (faulty) {
                logger.severe("The pid string ${filter} is malformed.")
                foundFaulty = true
            }
        }
        if (foundFaulty) {
            logger.severe("The dataset list you provided contains errors, Roddy will not start jobs.")
            return false
        }
        return true
    }

    private List<DataSet> collectDataSetsForCohort(String[] datasetFilters, Analysis analysis, List<DataSet> listOfDataSets) {
        boolean error = false
        List<DataSet> dList = []

        datasetFilters.collect { String _filter ->
            if (!_filter)
                return;
            List<DataSet> res = selectDatasetsFromPattern(analysis, [_filter], listOfDataSets, true);
            if (_filter.contains("*") || _filter.contains("?")) {
                if (!res) {
                    logger.severe("Could not find a match for cohort part: ${_filter}")
                    error = true;
                }
            } else if (res.size() != 1) {
                logger.severe("Only one match is allowed for cohort part: ${_filter}")
                error = true;
            }
            return res;
        }.flatten()
        if (error) return null
        return dList as ArrayList<DataSet>
    }

    List<DataSet> selectDatasetsFromPattern(Analysis analysis, List<String> pidFilters, List<DataSet> listOfDataSets, boolean suppressInfo) {

        List<DataSet> selectedDatasets = new LinkedList<>();
        WildcardFileFilter wff = new WildcardFileFilter(pidFilters);
        for (DataSet ds : listOfDataSets) {
            File inputFolder = ds.getInputFolderForAnalysis(analysis);
            if (!wff.accept(inputFolder))
                continue;
            if (!suppressInfo) logger.info(String.format("Selected dataset %s for processing.", ds.getId()));
            selectedDatasets.add(ds);
        }

        if (selectedDatasets.size() == 0)
            logger.postAlwaysInfo("There were no available datasets for the provided pattern.");
        return selectedDatasets;
    }

    ExecutionContext readInExecutionContext(AnalysisProcessingInformation api) {
        return new ExecutionContextReaderAndWriter(this).readInExecutionContext(api)
    }

    Map<String, JobState> readInJobStateLogFile(ExecutionContext context) {
        return new ExecutionContextReaderAndWriter(this).readInJobStateLogFile(context)
    }

    List<Job> readJobInfoFile(ExecutionContext context) {
        return new ExecutionContextReaderAndWriter(this).readJobInfoFile(context)
    }

    String writeJobInfoFile(ExecutionContext context) {
        return new ExecutionContextReaderAndWriter(this).writeJobInfoFile(context)
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
        return path;
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

        String dirPath = "${outPath}${sep}roddyExecutionStore${sep}${ConfigurationConstants.RODDY_EXEC_DIR_PREFIX}${context.getTimestampString()}_${context.getExecutingUser()}_${context.getAnalysis().getName()}"
        if (context.getExecutionContextLevel() == ExecutionContextLevel.CLEANUP)
            dirPath += "_cleanup"
        return new File(dirPath);
    }

    public File getCommonExecutionDirectory(ExecutionContext context) {
        try {

            def values = context.getConfiguration().getConfigurationValues()
            if (values.hasValue(RODDY_CENTRAL_EXECUTION_DIRECTORY)) {
                File configuredPath = values.get(RODDY_CENTRAL_EXECUTION_DIRECTORY, null)?.toFile(context);
                if (configuredPath)
                    return configuredPath;
            }
        } catch (Exception ex) {
            logger.severe("There was an error in toFile for cvalue ${RODDY_CENTRAL_EXECUTION_DIRECTORY} in RuntimeService:getCommonExecutionDirectory()")
        }
        return new File(getOutputFolderForProject(context).getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + DIRECTORY_RODDY_COMMON_EXECUTION);
    }

    public File getAnalysedMD5OverviewFile(ExecutionContext context) {
        return new File(getCommonExecutionDirectory(context).getAbsolutePath(), FILENAME_ANALYSES_MD5_OVERVIEW);
    }

    public File getLoggingDirectory(ExecutionContext context) {
        return getExecutionDirectory(context);
    }

    public File getLockFilesDirectory(ExecutionContext context) {
        File f = new File(getTemporaryDirectory(context), "lockfiles");
        return f;
    }

    public File getTemporaryDirectory(ExecutionContext run) {
        return new File(getExecutionDirFilePrefixString(run) + "temp");
    }

    public Date extractDateFromExecutionDirectory(File dir) {
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

    /** Only the first matching ${dataSet} or ${pid} will be returned. ${dataSet} has precedence over ${pid}.
     *  No checks are done that there is a unique solution.
     * @param path
     * @param analysis
     * @return
     */
    public String extractDataSetIDFromPath(File path, Analysis analysis) {
        String pattern = analysis.getConfiguration().getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY).toFile(analysis).getAbsolutePath()
        RoddyIOHelperMethods.getPatternVariableFromPath(pattern, "dataSet", path.getAbsolutePath()).
                orElse(RoddyIOHelperMethods.getPatternVariableFromPath(pattern, "pid", path.getAbsolutePath()).
                        orElse(Constants.UNKNOWN))
    }

    /**
     * Looks in the projects output directory if the cache file exists. If it does not exist it is created using the find command.
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
                String dataSetID
                try {
                    dataSetID = analysis.getRuntimeService().extractDataSetIDFromPath(path, analysis);
                } catch (RuntimeException e) {
                    throw new RuntimeException(e.message + " If you moved the .roddyExecCache.txt, please delete it and restart Roddy.")
                }
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
        File f = new File(job.context.getExecutionDirectory(), Roddy.getJobManager().getLogFileName(job));
    }

    public File getLogFileForCommand(Command command) {
        //Nearly the same as for the job but with a process id
        File f = new File(getExecutionDirectory(command.getTag(Constants.COMMAND_TAG_EXECUTION_CONTEXT) as ExecutionContext), Roddy.getJobManager().getLogFileName(command));
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

    public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
        def fs = context.getRuntimeService();
        //File cf = fs..createTemporaryConfigurationFile(executionContext);
        String pid = context.getDataSet().toString()
        Map<String, Object> parameters = [
                pid         : (Object) pid,
                PID         : pid,
                CONFIG_FILE : fs.getNameOfConfigurationFile(context).getAbsolutePath(),
                ANALYSIS_DIR: context.getOutputDirectory().getParentFile().getParent()
        ]
        return parameters;
    }

    public String createJobName(ExecutionContext executionContext, BaseFile bf, String TOOLID, boolean reduceLevel) {
        return _createJobName(executionContext, bf, TOOLID, reduceLevel)
    }

    public static String _createJobName(ExecutionContext executionContext, BaseFile bf, String TOOLID, boolean reduceLevel) {
        ExecutionContext rp = bf.getExecutionContext();
        String runtime = rp.getTimestampString();
        String pid = rp.getDataSet().getId()
        StringBuilder sb = new StringBuilder();
        sb.append("r").append(runtime).append("_").append(pid).append("_").append(TOOLID);
        return sb.toString();
    }

    /**
     * Checks if a folder is valid
     *
     * A folder is valid if:
     * <ul>
     *   <li>its parents are valid</li>
     *   <li>it was not created recently (within this context)</li>
     *   <li>it exists</li>
     *   <li>it can be validated (i.e. by its size or files, but not with a lengthy operation!)</li>
     * </ul>
     */
    public boolean isFileValid(BaseFile baseFile) {

        //Parents valid?
        boolean parentsValid = true;
        for (BaseFile bf in baseFile.parentFiles) {
            if (bf.isTemporaryFile()) continue; //We do not check the existence of parent files which are temporary.
            if (bf.isSourceFile()) continue;
            if (!bf.isFileValid()) {
                return false;
            }
        }

        boolean result = true;

        //Source files should be marked as such and checked in a different way. They are assumed to be valid.
        if (baseFile.isSourceFile())
            return true;

        //Temporary files are also considered as valid.
        if (baseFile.isTemporaryFile())
            return true;

        try {
            //Was freshly created?
            if (baseFile.creatingJobsResult != null && baseFile.creatingJobsResult.wasExecuted) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Does it exist and is it readable?
            if (result && !baseFile.isFileReadable()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Can it be validated?
            //TODO basefiles are always validated!
            if (result && !baseFile.checkFileValidity()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        // TODO? If the file is not valid then also temporary parent files should be invalidated! Or at least checked.
        if (!result) {
            // Something is missing here! Michael?
        }

        return result;
    }

    @Deprecated
    void releaseCache() {}

    @Deprecated
    boolean initialize() {}

    @Deprecated
    public void destroy() {
    }

    String calculateAutoCheckpointFilename(ToolEntry toolEntry, List<Object> parameters) {
        "AUTOCHECKPOINT_${toolEntry.id}_${(parameters.collect { it.toString().hashCode() }.join("") + "SAFEGUARDFOREMTPYLIST").hashCode()}"
    }
}
