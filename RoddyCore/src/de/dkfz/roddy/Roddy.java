/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy;

import com.btr.proxy.search.ProxySearch;
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize;
import de.dkfz.eilslabs.batcheuphoria.execution.cluster.pbs.PBSJobManager;
import de.dkfz.eilslabs.batcheuphoria.jobs.*;
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManager;
import de.dkfz.roddy.client.RoddyStartupModes;
import de.dkfz.roddy.client.RoddyStartupOptions;
import de.dkfz.roddy.client.cliclient.CommandLineCall;
import de.dkfz.roddy.client.cliclient.RoddyCLIClient;
import de.dkfz.roddy.client.rmiclient.RoddyRMIServer;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationConstants;
import de.dkfz.roddy.config.ConfigurationValue;
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues;
import de.dkfz.roddy.execution.jobs.*;
import de.dkfz.roddy.execution.io.ExecutionHelper;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.tools.AppConfig;
import de.dkfz.roddy.core.Initializable;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.BashCommandSet;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.io.fs.ShellCommandSet;
import de.dkfz.roddy.client.fxuiclient.RoddyUIController;
import de.dkfz.roddy.plugins.LibrariesFactory;
import de.dkfz.roddy.tools.LoggerWrapper;
import groovy.lang.GroovyClassLoader;
import groovy.transform.CompileStatic;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static de.dkfz.roddy.RunMode.CLI;
import static de.dkfz.roddy.StringConstants.FALSE;

/**
 * This is the main class for the Roddy command line application.
 * <p>
 * The class has several purposes:
 * - It initializes all service classes and the client interface (command line or gui).
 * - It is responsible to initialize proxy settings and to keep track of settings and settings files.
 * - It parses any special parameter passed to roddy on the command line like i.e. --useconfig.
 * - It provides access to application folders.
 *
 * @author michael
 */
public class Roddy {

    private static String SETTINGS_DIRECTORY_NAME = ".roddy";

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Roddy.class.getSimpleName());
    private static AppConfig applicationProperties;

    private static RunMode runMode;
    private static boolean mainStarted = false;
    /**
     * This option is for command line execution only.
     * If set, Roddy will call the command factory to wait for all created jobs to finish.
     */
    private static boolean waitForJobsToFinish = false;
    private static String customPropertiesFile = null;

    /**
     * If you start roddy with autosubmit[=n], then roddy starts one dataset after another
     * until the maxim of n running datasets is reached. If one dataset is finished it starts
     * the next one. So it is some sort of controlled submit mode where you can i.e. leave
     * roddy lalone over night.
     * <p>
     * This feature was planned and might still be of interest. However, most users created
     * their own scripts and it might be more feasible to use such a script than a permanent
     * running Roddy instance.
     */
    private static boolean autosubmitMode = false;
    private static int autosubmitMaxBatchCount = 4;

    private static boolean repeatJobSubmission = false;
    private static int repeatJobSubmissionAmount = -1;
    private static int repeatJobSubmissionWait = 10;

    /**
     * Enable this, if you want Roddy to keep track only of the current users jobs. This is automatically enabled
     * for command line and disabled for GUI
     */
    private static boolean trackUserJobsOnly;
    private static boolean trackOnlyStartedJobs;

    private static boolean useCustomIODirectories;
    private static boolean useSingleIODirectory;
    private static String baseInputDirectory;
    private static String baseOutputDirectory;

    private static final File applicationDirectory = new File("").getAbsoluteFile();
    private static final File applicationBundleDirectory = new File(applicationDirectory, "bundledFiles");

    private static CommandLineCall commandLineCall;

    /**
     * An option specifically for listworkflows.
     * Prints a list in this way:
     * ICGCeval@genome
     * ICGCeval@exome
     * ICGCeval.dbg@genome
     * ICGCeval.dbg@exome
     */
    private static boolean displayShortWorkflowList;

    /**
     * For test reasons, the user can disable this value. Then Roddy won't call System.exit().
     */
    private static boolean exitAllowed = true;

    private static AppConfig featureToggleConfig;

    public static boolean isStrictModeEnabled() {
        return getFeatureToggleValue(AvailableFeatureToggles.StrictMode);
    }

    public static int getRepeatSubmissionAmount() {
        if (!repeatJobSubmission)
            return 1;
        return repeatJobSubmissionAmount <= 0 ? Integer.MAX_VALUE : repeatJobSubmissionAmount;
    }

    public static int getRepeatSubmissionWait() {
        return repeatJobSubmissionWait <= 1 ? 2 : repeatJobSubmissionWait;
    }

    public static boolean isTrackingOfUserJobsEnabled() {
        return trackUserJobsOnly;
    }

    public static boolean queryOnlyStartedJobs() {
        return trackOnlyStartedJobs;
    }

    public static boolean useCustomIODirectories() {
        return useCustomIODirectories;
    }

    public static boolean useSingleIODirectory() {
        return useSingleIODirectory;
    }

    public static boolean displayShortWorkflowList() {
        return displayShortWorkflowList;
    }

    public static String getCustomBaseInputDirectory() {
        return baseInputDirectory;
    }

    public static String getCustomBaseOutputDirectory() {
        return baseOutputDirectory;
    }


    public static ResourceSetSize getUsedResourcesSize() {
        // Do not set, when null!
        if (commandLineCall == null) {
            return null;
        }

        // Return null, when it is not available
        if (!commandLineCall.isOptionSet(RoddyStartupOptions.usedresourcessize)) {
            return null;
        }

        // Return the parsed value or null.
        try {
            return ResourceSetSize.valueOf(commandLineCall.getOptionValue(RoddyStartupOptions.usedresourcessize));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isMetadataCLOptionSet() {
        return getCommandLineCall().isOptionSet(RoddyStartupOptions.usemetadatatable);
    }

    /**
     * Main method which calls startup and wraps it into a try catch block.
     * This is done for bug finding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
//            Thread.sleep(3000);
            //Check if Roddy is called from the right directory.
            //TODO Think about a better way to get Roddys base directory.
            String[] list = applicationDirectory.list((dir, name) -> name.equals("roddy.sh"));
            if (list == null || list.length != 1) {
                System.out.println("You must call roddy from the right location! (Base roddy folder where roddy.sh resides)");
                System.exit(255);
            }

            if (args.length == 0) {
                args = new String[]{RoddyStartupModes.help.toString()};
            }

            if (mainStarted == true)
                return;
            mainStarted = true;
            startup(args);
        } catch (Exception e) {
            e.printStackTrace();
            exit(1);
        }
    }

    public static void resetMainStarted() {
        mainStarted = false;
    }

    private static long t1 = 0;
    private static long t2 = 0;

    private static void time(String info) {
        t2 = System.nanoTime();
        if (info != null) logger.postSometimesInfo(RoddyIOHelperMethods.printTimingInfo(info, t1, t2));
        t1 = t2;
    }


    private static void startup(String[] args) {

        time(null);

        List<String> list = Arrays.asList(args);
        CommandLineCall clc = new CommandLineCall(list);
        commandLineCall = clc;
        // Initialize the logger with an initial setup. At this point we don't know about things like the logger settings
        // or the used app ini file. However, all the following methods rely on an existing valid logger setup.
        LoggerWrapper.setup(getApplicationLogDirectory());

        logger.postAlwaysInfo("Roddy version " + Constants.APP_CURRENT_VERSION_STRING);

        if(!performInitialCheck())
            exit(1);

        time("ftoggleini");
        time("clc .1");


        time("setup");
        initializeFeatureToggles();
        time("clc");
        performInitialSetup(args, clc.startupMode);
        time("initial");
        parseAdditionalStartupOptions(clc);
        time("parseopt");
        loadPropertiesFile();
        time("loadprop");
        LoggerWrapper.setup(applicationProperties);

        if (initializeServices(clc.startupMode.needsFullInit())) {
            time("initserv");
            parseRoddyStartupModeAndRun(clc);
            time("parsemode");
            performCLIExit(clc.startupMode);
        } else {
            logger.postAlwaysInfo("Could not initialize services. Roddy will not execute jobs.");
            performCLIExit(clc.startupMode, 1);
        }
        time("exit");
    }

    public static boolean performInitialCheck() {
        List<String> errors = new LinkedList<>();
        errors.add("Roddy cannot run:");
        if(!ExecutionHelper.executeCommandWithExtendedResult("which jar").successful)
            errors.add("\tTool jar not found.");
        if(!ExecutionHelper.executeCommandWithExtendedResult("which zip").successful)
            errors.add("\tTool zip not found.");
        if(!ExecutionHelper.executeCommandWithExtendedResult("which unzip").successful)
            errors.add("\tTool unzip not found.");

        if(errors.size() == 1) return true;

        logger.severe(RoddyIOHelperMethods.joinArray(errors.toArray(new String[0]), "\n"));
        return false;
    }

    public static void performInitialSetup(String[] args, RoddyStartupModes option) {

        runMode = CLI;
        if (option == RoddyStartupModes.ui) runMode = RunMode.UI;
        if (option == RoddyStartupModes.rmi) runMode = RunMode.RMI;

        trackUserJobsOnly = runMode == CLI ? true : false; //Auto enable or disable trackuserjobs

        for (int i = 0; i < args.length; i++) {
            logger.postRareInfo(String.format("[%d] - %s", i, args[i]));
        }
    }

    protected static void parseAdditionalStartupOptions(CommandLineCall clc) {

        //Parse different options out of the args back from behind
        displayShortWorkflowList = clc.isOptionSet(RoddyStartupOptions.shortlist);
        if (clc.isOptionSet(RoddyStartupOptions.useconfig))
            customPropertiesFile = clc.getOptionValue(RoddyStartupOptions.useconfig);
        else if (clc.isOptionSet(RoddyStartupOptions.c))
            customPropertiesFile = clc.getOptionValue(RoddyStartupOptions.c);

        for (RoddyStartupOptions startupOption : clc.getOptionList()) {

            if (startupOption == (RoddyStartupOptions.v)) {
                LoggerWrapper.setVerbosityLevel(LoggerWrapper.VERBOSITY_MEDIUM);
            }

            if (startupOption == (RoddyStartupOptions.vv)) {
                LoggerWrapper.setVerbosityLevel(LoggerWrapper.VERBOSITY_HIGH);
            }

            if (startupOption == (RoddyStartupOptions.verbositylevel)) {
                int level = RoddyConversionHelperMethods.toInt(clc.getOptionValue(startupOption), 5);
                LoggerWrapper.setVerbosityLevel(level);
            }

            //Enable the setup of all debug options via the command line.
            if (startupOption == (RoddyStartupOptions.debugOptions)) {
                String[] options = clc.getOptionList(startupOption).toArray(new String[0]);

            }

            if (runMode.isCommandLineMode()) {
                // Instead of terminating, Roddy waits for all submitted jobs to finish.
                if (startupOption == (RoddyStartupOptions.waitforjobs)) {
                    waitForJobsToFinish = true;
                }

                if (startupOption == (RoddyStartupOptions.useiodir)) {
                    useCustomIODirectories = true;

                    List<String> directories = clc.getOptionList(startupOption);
                    if (directories.size() == 0 || directories.size() > 2) {
                        throw new RuntimeException("Arguments for useasiodir are wrong");
                    }

                    useSingleIODirectory = directories.size() == 1;
                    baseInputDirectory = directories.get(0);
                    baseOutputDirectory = useSingleIODirectory ? baseInputDirectory : directories.get(1);
                }

                if (startupOption == (RoddyStartupOptions.disabletrackonlyuserjobs)) {
                    trackUserJobsOnly = false;
                }

                if (startupOption == (RoddyStartupOptions.trackonlystartedjobs)) {
                    trackOnlyStartedJobs = true;
                }

                // When a job is not submitted successfully, Roddy will wait try to do it again
                if (startupOption == (RoddyStartupOptions.resubmitjobonerror)) {
                    repeatJobSubmission = true;
                    List<String> options = clc.getOptionList(startupOption);
                    if (options.size() > 0)
                        repeatJobSubmissionAmount = RoddyConversionHelperMethods.toInt(options.get(0));
                    if (options.size() > 1)
                        repeatJobSubmissionWait = RoddyConversionHelperMethods.toInt(options.get(1));
                }

                if (startupOption == (RoddyStartupOptions.autosubmit)) {
                    autosubmitMode = true;
                    if (clc.getOptionValue(startupOption) != null)
                        autosubmitMaxBatchCount = RoddyConversionHelperMethods.toInt(clc.getOptionValue(startupOption));
                }

                //Enable setup of workflow run flags from the command line
                if (startupOption == (RoddyStartupOptions.run)) {
                    String[] flags = clc.getOptionList(startupOption).toArray(new String[0]);
                }

                if (startupOption == (RoddyStartupOptions.dontrun)) {
                    String[] flags = clc.getOptionList(startupOption).toArray(new String[0]);
                }
            }
        }
    }

    private static void initializeFeatureToggles() {
        File toggleIni = null;
        if (commandLineCall.isOptionSet(RoddyStartupOptions.usefeaturetoggleconfig)) {
            toggleIni = new File(commandLineCall.getOptionValue(RoddyStartupOptions.usefeaturetoggleconfig));
            logger.postSometimesInfo("Trying to load alternative feature toggles file: " + toggleIni);
            if (toggleIni == null || !toggleIni.exists()) {
                logger.postAlwaysInfo("Cannot find requested toggle file.");
                exit(1);
            }

        } else {
            toggleIni = new File(getSettingsDirectory(), "featureToggles.ini");
        }
        if (toggleIni != null && toggleIni.exists()) { // Use default values, if the file is not available
            logger.postAlwaysInfo("Loading a feature toggle file.");
            AppConfig appConfig = new AppConfig(toggleIni);
            featureToggleConfig = appConfig;
        } else {
            featureToggleConfig = new AppConfig();
            Arrays.asList(AvailableFeatureToggles.values()).forEach(toggle -> {
                featureToggleConfig.setProperty(toggle.name(), "" + toggle.defaultValue);
            });
        }
        //Override toggles in ini
        if (commandLineCall.isOptionSet(RoddyStartupOptions.enabletoggles)) {
            List<String> enabledToggles = commandLineCall.getOptionList(RoddyStartupOptions.enabletoggles);
            enabledToggles.forEach(toggle -> {
                if (AvailableFeatureToggles.valueOf(toggle) != null) featureToggleConfig.setProperty(toggle, "true");
            });
        }

        if (commandLineCall.isOptionSet(RoddyStartupOptions.disabletoggles)) {
            List<String> enabledToggles = commandLineCall.getOptionList(RoddyStartupOptions.enabletoggles);
            enabledToggles.forEach(toggle -> {
                if (AvailableFeatureToggles.valueOf(toggle) != null) featureToggleConfig.setProperty(toggle, "false");
            });
        }
    }

    public static boolean getFeatureToggleValue(AvailableFeatureToggles toggle) {
        if (featureToggleConfig == null) return toggle.defaultValue;
        return RoddyConversionHelperMethods.toBoolean(featureToggleConfig.getProperty(toggle.name(), null), toggle.defaultValue);
    }

    /**
     * Initializes basic Roddy services
     * The execution service has two configuration settings, one for command line interface and one for the ui based instance.
     */
    public static final boolean initializeServices(boolean fullSetup) {
        String currentStep = "Initialize proxy settings";
        try {

            // Configure a proxy for internet connection. Used i.e. for codemirror
            initializeProxySettings();
            time("init proxy");

            currentStep = "Initialize file system access provider";
            FileSystemAccessProvider.initializeProvider(fullSetup);
            time("init fsap");

            //Do not touch the calling order, execution service must be set before JobManager.
            currentStep = "Initialize execution service";
            ExecutionService.initializeService(fullSetup);
            time("init execserv");

            currentStep = "Initialize command factory";
            initializeJobManager(fullSetup);
            time("init cmd fac");
            return true;
        } catch (Exception ex) {
            logger.severe("initializeServices failed with an unhandled error. The step in which the error occurred was: " + currentStep + "\nSee the following stacktrace for more details.", ex);
            return false;
        }
    }

    /**
     * Auto detect and select a proxy provider.
     * TODO Does this work from Windows?
     * This uses the proxy vole library.
     */
    private static void initializeProxySettings() {
        boolean useProxyForInternetConnection = Boolean.parseBoolean(Roddy.getApplicationProperty(Constants.APP_PROPERTY_NET_USEPROXY, FALSE));
        if (useProxyForInternetConnection) {

            System.setProperty("java.net.useSystemProxies", "true");
            System.out.println("detecting proxies");
            List l = null;
            try {
                ProxySearch proxySearch = new ProxySearch();
                proxySearch.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
                proxySearch.addStrategy(ProxySearch.Strategy.ENV_VAR);
                proxySearch.addStrategy(ProxySearch.Strategy.KDE);

                proxySearch.addStrategy(ProxySearch.Strategy.BROWSER);
                ProxySelector proxySelector = proxySearch.getProxySelector();
                ProxySelector.setDefault(proxySelector);
                l = ProxySelector.getDefault().select(new URI("http://codemirror.net"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (l != null) {
                for (Iterator iter = l.iterator(); iter.hasNext(); ) {
                    java.net.Proxy proxy = (java.net.Proxy) iter.next();
                    System.out.println("proxy hostname : " + proxy.type());

                    InetSocketAddress addr = (InetSocketAddress) proxy.address();

                    if (addr == null) {
                        System.out.println("No Proxy");
                    } else {
                        System.out.println("proxy hostname : " + addr.getHostName());
                        System.setProperty("http.proxyHost", addr.getHostName());
                        System.out.println("proxy port : " + addr.getPort());
                        System.setProperty("http.proxyPort", Integer.toString(addr.getPort()));
                    }
                }
            }
        }
    }


    private static Configuration applicationSpecificConfiguration = null;
    public static Configuration getApplicationSpecificConfiguration() {
        if(applicationSpecificConfiguration == null) {
            applicationSpecificConfiguration = new Configuration(null);
            RecursiveOverridableMapContainerForConfigurationValues configurationValues = applicationSpecificConfiguration.getConfigurationValues();
            JobManager jobManager = Roddy.getJobManager();
            Map<String, String> specificEnvironmentSettings = jobManager.getSpecificEnvironmentSettings();
            for (String k : specificEnvironmentSettings.keySet()) {
                logger.postSometimesInfo("Add job manager value " + k + "=" + specificEnvironmentSettings.get(k) + " to context configuration");
                configurationValues.add(new ConfigurationValue(k, specificEnvironmentSettings.get(k)));
            }
        }
        return applicationSpecificConfiguration;
    }

    @CompileStatic
    public static void initializeJobManager(boolean fullSetup) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        logger.postSometimesInfo("public static void initializeFactory(boolean fullSetup)");
        if (!fullSetup)
            return;

        ClassLoader classLoader;
        String jobManagerClassID;
        Class jobManagerClass;

        classLoader = LibrariesFactory.getGroovyClassLoader();
        jobManagerClassID = Roddy.getApplicationProperty(Constants.APP_PROPERTY_COMMAND_FACTORY_CLASS, PBSJobManager.class.getName());
        jobManagerClass = classLoader.loadClass(jobManagerClassID);

        /** Get the constructor which comes with no parameters */
        Constructor first = jobManagerClass.getDeclaredConstructor(de.dkfz.eilslabs.batcheuphoria.execution.ExecutionService.class, JobManagerCreationParameters.class);
        jobManager = (JobManager) first.newInstance(ExecutionService.getInstance()
                , new JobManagerCreationParametersBuilder()
                .setCreateDaemon(true)
                .setTrackUserJobsOnly(trackUserJobsOnly)
                .setTrackOnlyStartedJobs(trackOnlyStartedJobs)
                .setUserIdForJobQueries(FileSystemAccessProvider.getInstance().callWhoAmI())
                .setJobIDIdentifier(ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_JOBID_RAW)
                .setJobArrayIDIdentifier(ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_JOBARRAYINDEX_RAW)
                .setJobScratchIdentifier(ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_SCRATCH_RAW).build());


// There are many values which need to be extracted from the xml (context, project?)
//        configuration.getProperty("PBS_AccountName", "")
//        configuration.getProperty("email")
//        configuration.getProperty("outputFileGroup", null)
//        configuration.getProperty("umask", "")

        // Was in Command
//        new File(configuration.getProperty("loggingDirectory", "/"))
    }

    private static JobManager jobManager;

    public static JobManager getJobManager() {
        return jobManager;
    }

    private static void parseRoddyStartupModeAndRun(CommandLineCall clc) {
        if (clc.startupMode == RoddyStartupModes.ui)
            RoddyUIController.App.main(clc.getArguments().toArray(new String[0]));
        else if (clc.startupMode == RoddyStartupModes.rmi)
            RoddyRMIServer.startServer(clc);
        else
            RoddyCLIClient.parseStartupMode(clc);
    }

    private static void performCLIExit(RoddyStartupModes option) {
        performCLIExit(option, 0);
    }

    private static void performCLIExit(RoddyStartupModes option, int exitCode) {
        if (commandLineCall.getOptionList().contains(RoddyStartupOptions.disallowexit))
            return;

        if (option == RoddyStartupModes.ui)
            return;

        if (!option.needsFullInit())
            return;

        if (jobManager.executesWithoutJobSystem() && waitForJobsToFinish) {
            exitCode = performWaitforJobs();
        } else {
            List<Command> listOfCreatedCommands = jobManager.getListOfCreatedCommands();
            for (Command command : listOfCreatedCommands) {
                if (command.getJob().getJobState() == JobState.FAILED) exitCode++;
            }
        }
        exit(exitCode);
    }

    private static int performWaitforJobs() {
        try {
            Thread.sleep(15000); //Sleep at least 15 seconds to let any job scheduler handle things...
            return jobManager.waitForJobsToFinish();
        } catch (Exception ex) {
            return 250;
        }

    }

    public static void exit() {
        exit(0);
    }

    public static void exit(int ecode) {
//        writePropertiesFile();
        Initializable.destroyAll();
        System.exit(ecode <= 250 ? ecode : 250); //Exit codes should be in range from 0 .. 255
    }

    public static void loadPropertiesFile() {
        File file = getPropertiesFilePath();
        logger.postSometimesInfo("Loading properties file: " + file.getAbsolutePath());
        if (!file.exists()) {
            // Skip and exit!
            logger.postAlwaysInfo("Could not load the application properties file: Exitting");
            exit(1);
        }

        applicationProperties = new AppConfig(file);

        getApplicationProperty("useRoddyVersion", LibrariesFactory.PLUGIN_VERSION_CURRENT); // Load some default properties
        getApplicationProperty("usePluginVersion");
        getApplicationProperty("pluginDirectories");
        getApplicationProperty("configurationDirectories");

    }

    public static AppConfig getApplicationConfiguration() {
        return applicationProperties;
    }

    public static String getApplicationProperty(String pName) {
        return getApplicationProperty(pName, "");
    }

    public static String getApplicationProperty(RunMode runMode, String pName) {
        return getApplicationProperty(runMode, pName, "");
    }

    public static String getApplicationProperty(RunMode runMode, String pName, String defaultValue) {
        return getApplicationProperty(runMode.name() + "." + pName, defaultValue);
    }

    public static String getApplicationProperty(String pName, String defaultValue) {

        //TODO Check, if the behaviour is right (more unit tests...)
        if (applicationProperties == null)
            return defaultValue;

        if (!applicationProperties.containsKey(pName))
            applicationProperties.setProperty(pName, defaultValue);

        return applicationProperties.getProperty(pName, defaultValue);
    }

    public static RunMode getRunMode() {
        return CLI;// runMode;
    }

    public static void setApplicationProperty(RunMode runMode, String appPropertyExecutionServiceClass, String text) {
        setApplicationProperty(runMode.name() + "." + appPropertyExecutionServiceClass, text);
    }

    public static void setApplicationProperty(String pName, String value) {
        applicationProperties.setProperty(pName, value);
    }

    /**
     * Returns the path to Roddys application settings directory.
     * This directory contains the application properties file and subfolders like i.e. file caches.
     *
     * @return
     */
    public static File getSettingsDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("user.home==null");
        }
        File home = new File(userHome);
        File settingsDirectory = new File(home, SETTINGS_DIRECTORY_NAME);
        if (!settingsDirectory.exists()) {
            if (!settingsDirectory.mkdir()) {
                throw new IllegalStateException(settingsDirectory.toString());
            }
        }
        return settingsDirectory;
    }

    public static File getApplicationLogDirectory() {
        File logDir = new File(getSettingsDirectory(), "logs");
        if (!logDir.exists())
            logDir.mkdir();
        return logDir;
    }

    public static File getPropertiesFilePath() {
        if (customPropertiesFile == null) customPropertiesFile = Constants.APP_PROPERTIES_FILENAME;
        File _customPropertiesFile = new File("" + customPropertiesFile);
        String customPropertiesFileName = _customPropertiesFile.getName();

        List<File> files = Arrays.asList(_customPropertiesFile, new File(getSettingsDirectory(), customPropertiesFileName), new File(getApplicationDirectory(), customPropertiesFileName));
        for (File cpf : files)
            if (cpf.exists())
                return cpf;

        logger.postAlwaysInfo("The configuration file " + customPropertiesFile + " does not exist in any known location and will not be loaded. A default configuration will be created and used.");
        return new File(getSettingsDirectory(), Constants.APP_PROPERTIES_FILENAME);
    }

    public static File getCompressedAnalysisToolsDirectory() {
        return new File(getSettingsDirectory(), "compressedAnalysisTools");
    }

    public static File getNewCompressedAnalysisToolsFile() {
        return new File(getCompressedAnalysisToolsDirectory(), String.format("roddyCompressedAnalysisToolsArchive_%d", System.currentTimeMillis()));
    }

    public static File getCompressedAnalysisToolsOverviewFile() {
        return new File(getCompressedAnalysisToolsDirectory(), "listOfCompressedFiles.txt");
    }

    private static List<File> loadFolderListFromConfiguration(RoddyStartupOptions option, String configurationConstant) {
        String[] split;
        if (getCommandLineCall().isOptionSet(option))
            split = getCommandLineCall().getOptionList(option).toArray(new String[0]);
        else
            split = Roddy.getApplicationProperty(configurationConstant, "").split("[,:]");

        List<File> folders = new LinkedList<>();
        for (String s : split) {
            File f = new File(s);
            if (f.exists())
                folders.add(f);
        }
        return folders;
    }

    public static List<File> getConfigurationDirectories() {
        return loadFolderListFromConfiguration(RoddyStartupOptions.configurationDirectories, Constants.APP_PROPERTY_CONFIGURATION_DIRECTORIES);
    }

    public static List<File> getPluginDirectories() {
        List<File> folders = loadFolderListFromConfiguration(RoddyStartupOptions.pluginDirectories, Constants.APP_PROPERTY_PLUGIN_DIRECTORIES);

        // TODO For backward compatibility the following plugin folders will be used in an hardcoded way
        for (File folder : Arrays.asList(
                RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "plugins"),
                RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "dist", "plugins"),
                RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "dist", "plugins_2.49plus"),
                RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "dist", "plugins_R2.3")
        )) {
            if (folder.exists() && !folders.contains(folder)) folders.add(folder);
        }
        return folders;
    }

    public static File getFileCacheDirectory() {
        File dir = getSettingsDirectory();
        File newDir = new File(dir.getAbsolutePath() + File.separator + "caches" + File.separator + "filecache");
        if (!newDir.exists())
            newDir.mkdirs();
        return newDir;
    }

    public static File getApplicationDirectory() {
        return applicationDirectory;
    }

    public static File getBundledFilesDirectory() {
        return applicationBundleDirectory;
    }

    public static String getUsedRoddyVersion() {
        String result = "";
        CommandLineCall clc = getCommandLineCall();
        for (RoddyStartupOptions opt : Arrays.asList(
                RoddyStartupOptions.useRoddyVersion,
                RoddyStartupOptions.useroddyversion,
                RoddyStartupOptions.rv)) {
            if (clc.isOptionSet(opt))
                result = clc.getOptionValue(opt);
        }
        if (!RoddyConversionHelperMethods.isNullOrEmpty(result)) return result;
        return getApplicationProperty("useRoddyVersion", LibrariesFactory.PLUGIN_VERSION_CURRENT);
    }

    public static File getRoddyBinaryFolder() {
        String roddyVersion = getUsedRoddyVersion();
        return RoddyIOHelperMethods.assembleLocalPath(applicationDirectory, "dist", "bin", roddyVersion);
    }

    public static String[] getPluginVersionEntries() {
        String[] pluginVersionEntries;
        if (getCommandLineCall().isOptionSet(RoddyStartupOptions.usePluginVersion))
            pluginVersionEntries = getCommandLineCall().getOptionValue(RoddyStartupOptions.usePluginVersion).split(StringConstants.SPLIT_COMMA);
        else
            pluginVersionEntries = getApplicationProperty(RoddyStartupOptions.usePluginVersion.name()).split(StringConstants.SPLIT_COMMA);
        return pluginVersionEntries;
    }

    public static String getUsedPluginVersion(String pluginID) {

        Map<String, String> pluginVersions = new LinkedHashMap<>();
        for (String pluginVersionEntry : getPluginVersionEntries()) {
            if (RoddyConversionHelperMethods.isNullOrEmpty(pluginVersionEntry)) continue;
            String[] versionString = pluginVersionEntry.split(StringConstants.SPLIT_COLON);
            pluginVersions.put(versionString[0], versionString[1]);
        }
        if (pluginVersions.containsKey(pluginID))
            return pluginVersions.get(pluginID);
        return LibrariesFactory.PLUGIN_VERSION_CURRENT;
    }

    public static FileSystemAccessProvider getLocalFileSystemInfoProvider() {
        return new FileSystemAccessProvider();
    }

    public static ShellCommandSet getLocalCommandSet() {
        String localCommandSet = getApplicationProperty("localCommandSet");

        // Handle default case, the local commandset ist not set.
        if (RoddyConversionHelperMethods.isNullOrEmpty(localCommandSet)) {
            logger.postSometimesInfo("Using BashCommandSet, localCommandSet ist not set.");
            BashCommandSet bcs = new BashCommandSet();
            bcs.validate();
            return bcs;
        }

        Class<ShellCommandSet> cls = null;
        try {
            cls = (Class<ShellCommandSet>) LibrariesFactory.getGroovyClassLoader().loadClass(localCommandSet);
            ShellCommandSet shellCommandSet = cls.newInstance();
            if (shellCommandSet.validate())
                return shellCommandSet;
            throw new RuntimeException("The selected ShellCommandSet '${localCommandSet}' could not validate.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find localCommandSet class '${localCommandSet}'. Check you ini file.");
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not create object of type: " + cls.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access default constructor for class: " + cls.getName());
        }
    }

    public static CommandLineCall getCommandLineCall() {
        if (commandLineCall == null)
            return new CommandLineCall(new LinkedList<>());
        return commandLineCall;
    }

}
