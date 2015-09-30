package de.dkfz.roddy.client.cliclient

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.client.cliclient.clioutput.*
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.config.validation.ValidationError
import de.dkfz.roddy.config.validation.WholeConfigurationValidator
import de.dkfz.roddy.core.*
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.SSHExecutionService
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.ScannerWrapper

import static de.dkfz.roddy.Constants.ENV_LINESEPARATOR as NEWLINE
import static de.dkfz.roddy.StringConstants.SPLIT_COMMA
import static de.dkfz.roddy.client.RoddyStartupModes.*;

/**
 * Command line client for roddy.
 * Offers methods for all the command line options (i.e. listworkflows, checkstatus)
 */
@groovy.transform.CompileStatic
public class RoddyCLIClient {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(RoddyCLIClient.class.name);

    private static class ProjectTreeItem {

        final List<ProjectTreeItem> children = [];
        final InformationalConfigurationContent icc;

        ProjectTreeItem(InformationalConfigurationContent icc, List<ProjectTreeItem> children) {
            this.icc = icc
            this.children += children;
        }
    }

    private static ConsoleStringFormatter _formatter = null;

    private static ConsoleStringFormatter getFormatter() {
        if (_formatter == null) {
            //Decide which formatter can be used.
            def env = System.getenv()
            boolean isLinux = env.get("OSTYPE") == "linux";
            boolean isBash = env.get("SHELL")?.contains("bash");

//        if (isLinux && isBash) { //Simple check if we are using linux and a color code aware console
//            if (System.console() != null)
//                return new BashFormatter();
//        }
            _formatter = new NoColorsFormatter();
        }
        return _formatter;
    }

    /**
     * Simple method to count input parameters
     *
     * @param args
     * @param parmCount
     * @throws RuntimeException
     */
    private static void checkParameterCount(String[] args, int parmCount) throws RuntimeException {
        if (args.length < parmCount) {
            RoddyStartupModes.printCommandLineOptions();
            throw new RuntimeException(Constants.ERR_MSG_WRONG_PARAMETER_COUNT + args.length);
        }
    }


    public static void parseStartupMode(CommandLineCall clc) {
        //TODO Convert to CommandLineCall
        String[] args = clc.getArguments();
        switch (clc.startupMode) {
            case showreadme:
                showReadme();
                break;
            case showfeaturetoggles:
                showFeatureToggles();
                break;
            case printappconfig:
                printApplicationConfiguration();
                break;
            case showconfigpaths:
                showConfigurationPaths();
                break;
            case plugininfo:
                showPluginInfo(clc);
                break;
            case printpluginreadme:
                printPluginReadme(clc);
                break;
            case printanalysisxml:
                printAnalysisXML(clc);
                break;
            case validateconfig:
                validateConfiguration(args[1]);
                break;
            case printruntimeconfig:
                printRuntimeConfiguration(clc);
                break;
            case listworkflows:
                listWorkflows(clc);
                break;
            case listdatasets:
                listDatasets(args);
                break;
            case run:
                RoddyCLIClient.run(clc);
                break;
            case rerun:
                RoddyCLIClient.rerun(clc);
                break;
            case testrun:
                RoddyCLIClient.testrun(clc);
                break;
            case testrerun:
                RoddyCLIClient.testrerun(clc);
                break;
            case rerunstep:
                break;
            case checkworkflowstatus:
                checkWorkflowStatus(clc);
                break;
            case cleanup:
                cleanupWorkflow(clc);
                break;
            case abort:
                abortWorkflow(clc);
                break;
            case help:
            default:
                RoddyStartupModes.printCommandLineOptions();
                System.exit(0);
        }
    }

    public static void showReadme() {
        println("Showing the Roddy readme file.");
        println(new File(RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "README.md").text));
    }

    public static void showFeatureToggles() {
        println("Available feature toggles and their default values.")
        println("Note, that not every toggle is active or usable!")
        AvailableFeatureToggles.values().each {
            AvailableFeatureToggles toggle ->
                println("\t${toggle.name()} [def:${toggle.defaultValue}]")
        }
    }

    public static void printApplicationConfiguration() {
        println(Roddy.getApplicationConfiguration());
    }

/**
 * List up configuration paths and the files in those.
 * @param args
 */
    public static void showConfigurationPaths() {
        Map<String, InformationalConfigurationContent> configObjects = ConfigurationFactory.getInstance().getAllAvailableConfigurations();
        def listOfFiles = configObjects.values().collect { InformationalConfigurationContent icc -> return icc.file; }
        Map<String, List<String>> filesInFolders = [:];
        listOfFiles.each { File f -> filesInFolders.get(f.parent, []) << f.name; }
        filesInFolders.sort();
        filesInFolders.each { String k, List<String> v ->
            println k;
            String previous = null;
            v.sort();
            v.each { String n -> if (previous != n) println "\t" + n; previous = n; }
        }
    }

    public static void showPluginInfo(CommandLineCall clc) {
//        Map<String, Map<String, PluginInfo>> listOfInfos = LibrariesFactory.getInstance().getLoadedPlugins();
//        listOfInfos.each {
//            String pluginID, Map<String, PluginInfo> pluginMap ->
//
//                StringBuilder sb = new StringBuilder();
//                sb << "Plugin: " << pluginID << NEWLINE;
        //TODO Update to work again.
//                sb << "Work directory: ".padLeft(25) << pluginInfo.directory << " [" << pluginInfo.prodVersion << "]" << NEWLINE;
//                sb << "Dev directory: ".padLeft(25) << pluginInfo.developmentDirectory << " [" << pluginInfo.devVersion << "]" << NEWLINE;
//                sb << "Used tools directories: ".padLeft(25) << NEWLINE;
//                pluginInfo.toolsDirectories.each {
//                    String k, File d ->
//                        sb << k.padLeft(30) << " = " << d.getAbsolutePath() << NEWLINE;
//                }
//                sb << NEWLINE;
//                System.out.println(sb.toString())
//            }
//        }
    }

    public static void printPluginReadme(CommandLineCall commandLineCall) {
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(commandLineCall.getArguments()[1]);

        def text = analysis.getReadmeFile()?.text
        if(text) {
            println("Print readme file for analysis ${analysis.getName()}: \n\t" + analysis.getReadmeFile());
            println(text);
        }
    }

    public static void printAnalysisXML(CommandLineCall commandLineCall) {
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(commandLineCall.getArguments()[1]);
        def content = analysis.getConfiguration().getInformationalConfigurationContent()

        System.out.println("Print analysis XML file for analysis ${analysis.getName()}: \n\t" + content.file);
        System.out.println(content.text);
    }

    /**
     * Validate a configuration in command line mode.
     * All errors found will be output in different colours.
     * @param id
     * @return
     */
    public static boolean validateConfiguration(String id) {
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(id)
        if (!analysis) return false;

        WholeConfigurationValidator wcv = new WholeConfigurationValidator(analysis.getConfiguration());
        wcv.validate();
        printValidationResult(id, analysis.getConfiguration(), wcv);
    }

    private static void printValidationResult(String id, Configuration configuration, WholeConfigurationValidator wcv) {
        final String separator = Constants.ENV_LINESEPARATOR;
        StringBuilder sb = new StringBuilder();

        sb << "#FWHITE#Validated configuration #FBLUE#${id}#CLEAR#" << separator;

        List<ValidationError> errors = wcv.getValidationErrors();

        int i = 0;

        for (ConfigurationLoadError error : configuration.getListOfLoadErrors()) {
            sb << "#FRED#" << "${i}: ".padLeft(6) << "Load error #CLEAR#" << separator;
            sb << "      ID:          " << error.id << separator;
            sb << "      Description: " << error.description << separator;
            if (error.exception)
                sb << "      #FWHITE##BRED#" << error.exception.toString() << "#CLEAR#" << separator;
            i++;
        }

        for (ValidationError error : errors) {
            sb << "#FRED#" << "${i}: ".padLeft(6) << "Validation error:#CLEAR#" << separator;
            sb << "      ID:          " << error.id << separator;
            sb << "      Description: " << error.description << separator;
            if (error.exception)
                sb << "      #FWHITE##BRED#" << error.exception.toString() << "#CLEAR#" << separator;
            i++;
        }

        System.out.println(getFormatter().formatAll(sb.toString()));
    }

    public static void printRuntimeConfiguration(CommandLineCall commandLineCall) {
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(commandLineCall.getArguments()[1]);
        if (!analysis) return;

        if (commandLineCall.getParameters().size() < 2) {
            logger.postAlwaysInfo("There must be a valid dataset id / pid.")
            return;
        }

        List<ExecutionContext> executionContexts = analysis.run(Arrays.asList(commandLineCall.getArguments()[2].split(SPLIT_COMMA)), ExecutionContextLevel.QUERY_STATUS);
        for (it in executionContexts) {
            System.out.println(ConfigurationConverter.convertAutomatically(it, it.getAnalysis().getConfiguration()));
        }
    }

    public static void listWorkflows(CommandLineCall clc) {
//        RoddyCLIClient.checkParameterCount(args, 1);

        String filter = clc.hasParameters() ? clc.getParameters().get(0) : "";

        final String separator = Constants.ENV_LINESEPARATOR;

        //TODO colours will only work on linux bash, so fix that or at least disable it on other environments
        List<InformationalConfigurationContent> availableProjectConfigurations = ConfigurationFactory.getInstance().getAvailableProjectConfigurations();

        availableProjectConfigurations.sort(new Comparator<InformationalConfigurationContent>() {
            @Override
            public int compare(InformationalConfigurationContent o1, InformationalConfigurationContent o2) {
                return o1.name.compareTo(o2.name);
            }
        });

        List<ProjectTreeItem> items = loadProjectsRec(availableProjectConfigurations);

        for (ProjectTreeItem pti in items) {

            //First check, if a filter must be applied.
            if (!projectIDContains(pti, filter)) {
                logger.postSometimesInfo(getFormatter().formatAll("#FRED#Configuration ${pti.icc.id} left out because of the specified filter.#CLEAR#"));
                continue;
            }
            logger.postAlwaysInfo(getFormatter().formatAll("${separator}#FWHITE##BGRED#Parsing configuration ${pti.icc.id}#CLEAR# : ${pti.icc.file}"));
            if (clc.isOptionSet(RoddyStartupOptions.shortlist)) {
                logger.postAlwaysInfo(getFormatter().formatAll(printRecursivelyShort(pti).toString()));
            } else {
                logger.postAlwaysInfo(getFormatter().formatAll(printRecursively(0, pti).toString())); ;
            }
        }
    }

    private static List<ProjectTreeItem> loadProjectsRec(List<InformationalConfigurationContent> availableProjectConfigurations) {
        List<ProjectTreeItem> lst = [];
        for (InformationalConfigurationContent icc : availableProjectConfigurations) {
            lst << new ProjectTreeItem(icc, loadProjectsRec(icc.getSubContent()));
        }
        return lst;
    }

    private static StringBuilder printRecursivelyShort(ProjectTreeItem pti) {
        final String separator = Constants.ENV_LINESEPARATOR;
        StringBuilder configText = new StringBuilder();
        Configuration cfg = ConfigurationFactory.getInstance().getConfiguration(pti.icc.id);
        for (String iccAna in pti.icc.listOfAnalyses) configText << cfg.getID() << "@" << iccAna.split("::")[-2] << separator
        for (ProjectTreeItem it in pti.children) configText << printRecursivelyShort(it);
//        configText << separator;
        return configText;
    }

    private static StringBuilder printRecursively(int depth, ProjectTreeItem pti) {
        final String separator = Constants.ENV_LINESEPARATOR;
        final int numberOfTabs = depth;
        final char[] arrayOfTabs = new char[numberOfTabs];
        final char tab = "\t";
        Arrays.fill(arrayOfTabs, tab);
        final String prefix = new String(arrayOfTabs);

        //Bash colours.
        //TODO Disable that for windows clients.
        StringBuilder configText = new StringBuilder();

        //Print info:
        Configuration cfg = ConfigurationFactory.getInstance().getConfiguration(pti.icc.id);

        configText << prefix << "#FWHITE##BGRED#ID:#CLEAR##FWHITE#       " << "${cfg.ID}#CLEAR#"
        if (cfg.description) configText << " - #FGREEN#${cfg.description}#CLEAR#";
        configText << separator;

        if (pti.icc.listOfAnalyses) {
            if (depth == 0) configText << prefix << " Class:   ${cfg.configuredClass}${separator}";
            if (pti.icc.imports) configText << prefix << " Imports: " << pti.icc.imports << separator;

            configText << prefix << " #FBLUE#Available analyses:#CLEAR#" << separator
            int i = 0;
            for (String iccAna in pti.icc.listOfAnalyses) {
                String[] split = iccAna.split("::");
                configText << prefix << "   " << i << ": ID=[" << split[-2] << "], Workflow=[" << split[-1] << "]" << separator;
                i++;
            }
        }

        if (pti.icc.subContent) {
            //Go into sub items
            configText << prefix << " #FYELLOW#Sub configurations:#CLEAR#" << separator
            for (ProjectTreeItem it in pti.children) {
                configText << printRecursively(depth + 1, it);
            }
        }
        configText << separator;
        return configText;
    }

    private static boolean projectIDContains(ProjectTreeItem pti, String filter) {
        if (!filter) return true;
        if (pti.icc.id.toLowerCase().contains(filter.toLowerCase())) return true;

        boolean found = false;
        if (pti.children.size() > 0)
            for (ProjectTreeItem child : pti.children) {
                if (projectIDContains(child, filter))
                    found = true;
            }

        return found;
    }

    public static void listDatasets(String[] args) {
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(args[1]);
        if (!analysis) return;

        def datasets = analysis.getListOfDataSets()
        for (DataSet ds : datasets) {
            System.out.println(String.format("\t%s", ds.getId()));
        }
    }

    public static Analysis checkAndLoadAnalysis(CommandLineCall clc) {
        if (clc.parameters.size() < 2) {
            logger.postAlwaysInfo("There were no dataset identifiers set, cannot run workflow."); return null;
        }
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(clc.parameters[0]);
        return analysis;
    }

    public static void run(CommandLineCall clc) {
        Analysis analysis = checkAndLoadAnalysis(clc);
        if (!analysis) return;

        analysis.run(Arrays.asList(clc.parameters[1].split(SPLIT_COMMA)), ExecutionContextLevel.RUN);
    }

    public static List<ExecutionContext> rerun(CommandLineCall clc) {
        Analysis analysis = checkAndLoadAnalysis(clc);
        if (!analysis) return;

        List<ExecutionContext> executionContexts = analysis.run(Arrays.asList(clc.parameters[1].split(SPLIT_COMMA)), ExecutionContextLevel.QUERY_STATUS);
        return analysis.rerun(executionContexts, false);
    }

    /**
     * Performs a dry run and prints out information about jobs and files which would normally be run or created.
     * @param args
     */
    public static void testrun(CommandLineCall clc, boolean testrerun = false) {
        Analysis analysis = checkAndLoadAnalysis(clc);
        if (!analysis) return;

        List<ExecutionContext> executionContexts = analysis.run(Arrays.asList(clc.parameters[1].split(SPLIT_COMMA)), ExecutionContextLevel.QUERY_STATUS);
        if (testrerun) executionContexts = analysis.rerun(executionContexts, true);

        outputRerunResult(executionContexts, false)
    }

    /**
     * Performs a dry rerun and prints out information about jobs and files which would normally be run or created.
     * @param args
     */
    public static void testrerun(CommandLineCall clc) {
        RoddyCLIClient.testrun(clc, true);
    }

    private static void outputRerunResult(List<ExecutionContext> executionContexts, boolean rerun) {
        final String separator = Constants.ENV_LINESEPARATOR;
        for (ExecutionContext ec : executionContexts) {
            Configuration configuration = ec.getConfiguration()
            ConfigurationConverter.convertAutomatically(ec, configuration);
            StringBuilder sb = new StringBuilder();

            sb << "#FWHITE##BGBLUE#Information about run test for dataset: " << ec.getDataSet().getId() << "#CLEAR#" << separator;

            sb << "  Input directory     : ${ec.getInputDirectory()}" << separator;
            sb << "  Output directory    : ${ec.getOutputDirectory()}" << separator;
            sb << "  Execution directory : ${ec.getExecutionDirectory()}" << separator;

            Collection<Job> collectedJobs = ec.getExecutedJobs().findAll { Job job -> job.getJobID() != null && (rerun ? job.getJobState() == JobState.UNSTARTED : true) && job.runResult?.wasExecuted; }
            sb << "  #FWHITE#List of jobs (${collectedJobs.size()}):#CLEAR#" << separator;
            for (Job job : collectedJobs) {

                String resources = "Unknown resource entry";

                try {
                    ToolEntry tool = configuration.getTools().getValue(job.getToolID());
                    ToolEntry.ResourceSet resourceSet = tool.getResourceSet(configuration);
                    ProcessingCommands convertResourceSet = CommandFactory.getInstance().convertResourceSet(configuration, resourceSet);
                    resources = convertResourceSet.toString();

                } catch (Exception ex) {
                }

                sb << "    #FYELLOW#${job.getJobID()}:#CLEAR# ${job.getToolID()} [${resources}]" << separator;

                for (k in job.parameters.keySet()) {
                    String _k = k;
                    if (k.length() > 25)
                        _k = k.substring(0, 23) + ".."
                    String parm = job.parameters.get(k, "").replace(ec.getExecutionDirectory().getAbsolutePath(), "[ exDir]");
                    parm = parm.replace(ec.getOutputDirectory().getAbsolutePath(), "[outDir]");
                    parm = parm.replace(ec.getInputDirectory().getAbsolutePath(), "[ inDir]");
                    if (parm.startsWith("parameterArray") && parm != "parameterArray=()") {
                        parm = parm.replace(" ", "\n" + " ".padRight(38));
                        parm = parm.replace("(", "(\n" + " ".padRight(38));
                        parm = parm.replace(")", "\n" + " ".padRight(34) + ")");
                    }
                    sb << "      #FYELLOW#${_k.padRight(26)}: ${parm}" << "#CLEAR#" << separator;
                }
            }

            ec.getExecutedJobs()

            sb << separator;
            sb << separator;

            println(getFormatter().formatAll(sb.toString()));
        }
    }

    /**
     * Read in information about datasets and i.e. number of running jobs etc.
     * Can be called in detailed mode to display information about the current execution context.
     * @param args
     */
    public static void checkWorkflowStatus(CommandLineCall clc) {
        final String separator = Constants.ENV_LINESEPARATOR;

        def analysisID = clc.getParameters().get(0)
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(analysisID);
        if (!analysis) return;
        List<String> dFilter = clc.getParameters().size() >= 2 ? clc.getParameters()[1].split(SPLIT_COMMA) : null;
        if (dFilter == null) {
            println("There were no valid pids specified.")
            return;
        }

        Map<DataSet, Boolean> dataSets = analysis.checkStatus(dFilter, true);

        String outputDirectory = analysis.getOutputBaseDirectory().getAbsolutePath()

        StringBuilder sb = new StringBuilder();
        sb << "#FWHITE##BGBLUE#Listing datasets for analysis ${analysisID}:#CLEAR#" << NEWLINE;
        sb << "Note, that only 'valid' information is processed and display. Empty execution folders and ";
        sb << "folders containing no job information will be skipped." << NEWLINE << NEWLINE << "[outDir]: " << outputDirectory << NEWLINE;

        if(!dataSets)
            return;

        //Get padding length for pid.
        int padSize = dataSets.keySet().max { DataSet ds -> ds.id.length(); }.id.length() + 2
        if (padSize < 10) padSize = 10;

        sb << "Dataset".padRight(padSize) << "  State     " << "#    " << "OK   " << "ERR  " << "User      " << "Folder / Message" << separator;

        for (DataSet ds in dataSets.keySet()) {
            boolean running = dataSets.get(ds);
            boolean datasetprinted = false;
            List<AnalysisProcessingInformation> listOfAPIsToPrint = [];
            if (clc.isOptionSet(RoddyStartupOptions.extendedlist)) {
                listOfAPIsToPrint = ds.getProcessingInformation(analysis);
            } else {
                listOfAPIsToPrint << ds.getLatestValidProcessingInformation(analysis);
            }
            for (AnalysisProcessingInformation information : listOfAPIsToPrint) {

                ExecutionContext context = null;
                if (information)
                    context = information.getDetailedProcessingInfo();

                if(!datasetprinted) {
                    sb << ds.getId().padRight(padSize) << "  ";
                    datasetprinted = true;
                } else {
                    sb << "".padRight(padSize) << "  ";
                }

                if (context == null) {
                    sb << "UNSTARTED 0    0    0".padRight(25) << "Not executed (or the Roddy log files were deleted).#CLEAR#" << separator;
                    continue;
                }

                def userID = context.getExecutingUser().padRight(10).substring(0, 9)
                def execFolder = context.getExecutionDirectory().getAbsolutePath()
                if (clc.isOptionSet(RoddyStartupOptions.shortlist)) execFolder = execFolder.replace(outputDirectory, "[outDir]")

                if (running) {
                    sb << "RUNNING".padRight(25) << userID << " " << execFolder << separator;
                } else {
                    //Check for errors in the last run.
                    //Check if there were errornous jobs
                    List<Job> listOfJobs = context.getExecutedJobs();
                    int failedJobs = 0;
                    Map<JobState, Integer> counter = [:];
                    for (it in listOfJobs) {
                        if (it.getJobState() == JobState.FAILED) {
                            failedJobs++;
                            counter[it.getJobState()] = counter.get(it.getJobState(), 0) + 1;
                        }
                    }
                    String state = failedJobs > 0 ? "FAILED" : "OK";

                    int startedJobCnt = listOfJobs.size()
                    sb << state.padRight(10) << ("" + startedJobCnt).padRight(5) << ("" + (startedJobCnt - failedJobs)).padRight(5) << ("" + failedJobs).padRight(5) << userID << " " << execFolder << separator;
                }
            }

        }
        sb << "#CLEAR#" << separator;
        println(getFormatter().formatAll(sb.toString()));

    }

    static void cleanupWorkflow(CommandLineCall clc) {
        def analysisID = clc.getParameters().get(0)
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(analysisID);
        if (!analysis) return;
        List<String> dFilter = clc.getParameters().size() >= 2 ? clc.getParameters()[1].split(SPLIT_COMMA) : null;
        if (dFilter == null) {
            println("There were no valid pids specified.")
            return;
        }
        analysis.cleanup(dFilter);
    }

    static void abortWorkflow(CommandLineCall clc) {
        def analysisID = clc.getParameters().get(0)
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(analysisID);
        if (!analysis) return;
        List<String> dFilter = clc.getParameters().size() >= 2 ? clc.getParameters()[1].split(SPLIT_COMMA) : null;
        if (dFilter == null) {
            println("There were no valid pids specified.")
            return;
        }

        Map<DataSet, Boolean> dataSets = analysis.checkStatus(dFilter);
        for (DataSet ds in dataSets.keySet()) {
            List<AnalysisProcessingInformation> information = ds.getProcessingInformation(analysis);
            ExecutionContext context = null;
            List<Job> listOfJobs = null;
            List<Job> listOfRunningJobs = [];
            if (information)
                context = information.first().getDetailedProcessingInfo();
            if (context && context.hasRunningJobs())
                listOfJobs = context.getExecutedJobs()

            if (listOfJobs)
                for (job in listOfJobs) {
                    if (job.isFakeJob()) continue;
                    if (job.getJobState().isPlannedOrRunning())
                        listOfRunningJobs << job;
                }
            if (listOfRunningJobs)
                CommandFactory.getInstance().queryJobAbortion(listOfRunningJobs)
        }
    }

    /**
     * Sets up Roddys property file from the command line.
     */
    public static void performCommandLineSetup() {
        try {
            ScannerWrapper sc = new ScannerWrapper();
            System.out.println("Setup roddy for command line processing.\n========================================\n");

            Roddy.RunMode runMode = Roddy.getRunMode();

            boolean useProxyForInternetConnection = Boolean.parseBoolean(Roddy.getApplicationProperty(Constants.APP_PROPERTY_NET_USEPROXY, false.toString()));
            useProxyForInternetConnection = sc.getBooleanYN("Does your internet connection require a proxy:", useProxyForInternetConnection);
            if (useProxyForInternetConnection) {
                String adr = sc.getString("Enter your proxy address: ", Roddy.getApplicationProperty(Constants.APP_PROPERTY_NET_PROXY_ADDRESS, StringConstants.EMPTY));
                String usr = sc.getString("Enter your proxy user id: ", Roddy.getApplicationProperty(Constants.APP_PROPERTY_NET_PROXY_USR, StringConstants.EMPTY));
                Roddy.setApplicationProperty(Constants.APP_PROPERTY_NET_PROXY_ADDRESS, adr);
                Roddy.setApplicationProperty(Constants.APP_PROPERTY_NET_PROXY_USR, usr);
            }
            Roddy.setApplicationProperty(Constants.APP_PROPERTY_NET_USEPROXY, useProxyForInternetConnection.toString());

            String selectedExecService = Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, LocalExecutionService.class.getName());
            String serviceQuery = "Chose an execution service:";
            List<String> availableServiceOptions = Arrays.asList("LocalExecutionService - Run everything locally (Data files, binaries and the command submission tool must be accessible!)", "SSHExecutionService - Run using an ssh connection.");
            List<String> availableServiceClasses = Arrays.asList(LocalExecutionService.class.getName(), SSHExecutionService.class.getName());
            String selected = null;
            if (selectedExecService.equals(LocalExecutionService.class.getName()))
                selected = availableServiceOptions.get(0);
            else if (selectedExecService.equals(SSHExecutionService.class.getName()))
                selected = availableServiceOptions.get(1);
            selectedExecService = sc.getChoiceAsObject(serviceQuery, availableServiceOptions, 1, availableServiceClasses, selected);

            if (selectedExecService.equals(availableServiceClasses.get(1))) {
                String sshHosts;
                System.out.print("Enter a list of ssh hosts: [" + Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS, "") + "] ");
                sshHosts = sc.getString();
                if (sshHosts.trim().length() == 0)
                    sshHosts = Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS, "");

//                System.out.println("Create a key pair for passwordless server access? (Roddy won't run without this!) [y/N] ");
//                boolean createKeyPair = sc.getBooleanYN();
                String selectedAuthenticationMethod = Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE);
                String authQuery = "Chose an authentication method:";
                List<String> authOptions = Arrays.asList("Authenticate using a pair of passwordless keyfiles.", "Authenticate using a user and a password.");
                List<String> authMethods = Arrays.asList(Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD);
                selected = null;
                if (selectedAuthenticationMethod.equals(Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE))
                    selected = authOptions.get(0);
                else if (selectedAuthenticationMethod.equals(Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD))
                    selected = authOptions.get(1);
                selectedAuthenticationMethod = sc.getChoiceAsObject(authQuery, authOptions, 1, authMethods, selected);

                String sshUser = "";
                String sshPassword = "";
                boolean storePassword = false;
                if (selectedAuthenticationMethod.equals(Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD)) {
                    System.out.print("Enter your ssh user id: [" + Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, "") + "] ");
                    sshUser = sc.getString();
                    System.out.print("Enter your ssh password: ");
                    sshPassword = sc.getPasswordString();
//                    System.out.print( [y/N] ");
                    storePassword = false;//sc.getBooleanYN("Do you want to store the password (This is done unencrypted!)?", false);
                }

                //TODO Put that back in?
//                System.out.println("Do you want to receive emails? [y/N]");
//                String email = "";
//                boolean wantsMail = sc.getBooleanYN();
//                if (wantsMail) {
//                    System.out.println("Enter your email address: ");
//                    email = sc.getString();
//                }

                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, selectedExecService);
                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS, sshHosts);
                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, selectedAuthenticationMethod);
                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, sshUser);
                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, ((Boolean) storePassword).toString());
                Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, sshPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void askForPassword() {
        ScannerWrapper sc = new ScannerWrapper();
        System.out.print("Enter your ssh password: ");
        String sshPassword = sc.getPasswordString();
        Roddy.setApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, sshPassword);
    }
}
