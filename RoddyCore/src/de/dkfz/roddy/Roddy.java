package de.dkfz.roddy;

import com.btr.proxy.search.ProxySearch;
import de.dkfz.roddy.client.RoddyStartupModes;
import de.dkfz.roddy.client.RoddyStartupOptions;
import de.dkfz.roddy.client.cliclient.RoddyCLIClient;
import de.dkfz.roddy.config.ConfigurationFactory;
import de.dkfz.roddy.core.Initializable;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
import de.dkfz.roddy.execution.jobs.Command;
import de.dkfz.roddy.execution.jobs.CommandFactory;
import de.dkfz.roddy.client.fxuiclient.RoddyUIController;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.plugins.LibrariesFactory;
import de.dkfz.roddy.tools.LoggerWrapper;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
//import sun.misc.Signal;
//import sun.misc.SignalHandler;
//import sun.tools.jar.CommandLine;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.*;

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

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Roddy.class.getName());
    private static final Properties applicationProperties = new Properties();

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

    private static RoddyCLIClient.CommandLineCall commandLineCall;

    /**
     * An option specifically for listworkflows.
     * Prints a list in this way:
     * ICGCeval@genome
     * ICGCeval@exome
     * ICGCeval.dbg@genome
     * ICGCeval.dbg@exome
     */
    private static boolean displayShortWorkflowList;

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

    /**
     * Main method which calls startup and wraps it into a try catch block.
     * This is done for bug finding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            exit();
        }
    }

    private static void startup(String[] args) {

        LoggerWrapper.setup();

        RoddyCLIClient.CommandLineCall clc = new RoddyCLIClient.CommandLineCall(args);
        commandLineCall = clc;

        performInitialSetup(args, clc.startupMode);

        parseAdditionalStartupOptions(clc);

        loadPropertiesFile();

        initializeServices(clc.startupMode.needsFullInit());

        parseRoddyStartupModeAndRun(clc);

        performCLIExit(clc.startupMode);
    }

    public static void performInitialSetup(String[] args, RoddyStartupModes option) {

        runMode = option == RoddyStartupModes.ui ? RunMode.UI : RunMode.CLI;
        trackUserJobsOnly = runMode == RunMode.CLI ? true : false; //Auto enable or disable trackuserjobs

        for (int i = 0; i < args.length; i++) {
            logger.postRareInfo(String.format("[%d] - %s", i, args[i]));
        }
    }

    protected static void parseAdditionalStartupOptions(RoddyCLIClient.CommandLineCall clc) {

        //Parse different options out of the args back from behind
        displayShortWorkflowList = clc.isOptionSet(RoddyStartupOptions.shortlist);
        if (clc.isOptionSet(RoddyStartupOptions.useconfig))
            customPropertiesFile = clc.getOptionValue(RoddyStartupOptions.useconfig);

        for (RoddyStartupOptions startupOption : clc.getOptionList()) {

            if (startupOption == (RoddyStartupOptions.verbositylevel)) {
                int level = RoddyConversionHelperMethods.toInt(clc.getOptionValue(startupOption), 5);
                LoggerWrapper.setVerbosityLevel(level);
            }

            //Enable the setup of all debug options via the command line.
            if (startupOption == (RoddyStartupOptions.debugOptions)) {
                String[] options = clc.getOptionList(startupOption).toArray(new String[0]);

            }

            if (runMode == RunMode.CLI) {
                // Instead of terminating, Roddy waits for all submitted jobs to finish.
                if (startupOption == (RoddyStartupOptions.waitforjobs)) {
                    waitForJobsToFinish = true;
                }

                if (startupOption == (RoddyStartupOptions.useiodir)) {
                    useCustomIODirectories = true;

                    String[] directories = clc.getOptionValue(startupOption).split(StringConstants.SPLIT_COMMA);
                    if (directories.length == 0 || directories.length > 2) {
                        throw new RuntimeException("Arguments for useasiodir are wrong");
                    }

                    useSingleIODirectory = directories.length == 1;
                    baseInputDirectory = directories[0];
                    baseOutputDirectory = useSingleIODirectory ? baseInputDirectory : directories[1];
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
//        return newArguments.toArray(new String[0]);
    }

    /**
     * Initializes basic Roddy services
     * The execution service has two configuration settings, one for command line interface and one for the ui based instance.
     */
    public static final boolean initializeServices(boolean fullSetup) {
        try {
//            LibrariesFactory.initializeFactory();
//
//            ConfigurationFactory.getInstance();

            // Configure a proxy for internet connection. Used i.e. for codemirror
            initializeProxySettings();

            FileSystemInfoProvider.initializeProvider(fullSetup);

            //Do not touch the calling order, execution service must be set before commandfactory.
            ExecutionService.initializeService(fullSetup);

            CommandFactory.initializeFactory(fullSetup);

            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
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

//                proxySearch.addStrategy(ProxySearch.Strategy.JAVA);
                proxySearch.addStrategy(ProxySearch.Strategy.BROWSER);
                ProxySelector proxySelector = proxySearch.getProxySelector();
//                l = proxySelector.select(new URI("http://codemirror.net"));
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

    private static void parseRoddyStartupModeAndRun(RoddyCLIClient.CommandLineCall clc) {
        if (clc.startupMode == RoddyStartupModes.ui)
            RoddyUIController.App.main(clc.getArguments().toArray(new String[0]));
        else
            RoddyCLIClient.parseStartupMode(clc);
    }

    private static void performCLIExit(RoddyStartupModes option) {
        if (option == RoddyStartupModes.ui)
            return;

        if (!option.needsFullInit())
            return;

        int exitCode = 0;
        if (!CommandFactory.getInstance().executesWithoutJobSystem() && waitForJobsToFinish) {
            exitCode = performWaitforJobs();
        } else {
            List<Command> listOfCreatedCommands = CommandFactory.getInstance().getListOfCreatedCommands();
            for (Command command : listOfCreatedCommands) {
                if (command.getJob().getJobState() == JobState.FAILED) exitCode++;
            }
        }
        exit(exitCode);
    }

    private static int performWaitforJobs() {
        try {
            Thread.sleep(15000); //Sleep at least 15 seconds to let any job scheduler handle things...
            return CommandFactory.getInstance().waitForJobsToFinish();
        } catch (Exception ex) {
            return 250;
        }

    }

    public static void exit() {
        exit(0);
    }

    public static void exit(int ecode) {
        writePropertiesFile();
        Initializable.destroyAll();
        System.exit(ecode <= 250 ? ecode : 250); //Exit codes should be in range from 0 .. 255
    }

    public static void loadPropertiesFile() {
        try {
            File file = getPropertiesFilePath();
            logger.postSometimesInfo("Loading properties file: " + file.getAbsolutePath());
            if (!file.exists()) file.createNewFile();
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
            applicationProperties.load(stream);

            getApplicationProperty("useRoddyVersion", "current"); // Load some default properties
            getApplicationProperty("usePluginVersion");
            getApplicationProperty("pluginDirectories");
            getApplicationProperty("configurationDirectories");

            if (LoggerWrapper.getVerbosityLevel() >= LoggerWrapper.VERBOSITY_HIGH) {
                for (Object o : applicationProperties.keySet()) {
                    boolean isSet = applicationProperties.get(o).toString().length() > 0;
                    if (!isSet) logger.postRareInfo("Property " + o.toString() + " is not set");
                }
            }
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void writePropertiesFile() {
        try {
            File file = getPropertiesFilePath();
            if (!file.exists()) file.createNewFile();
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
            final Map<RunMode, String> tempPasswords = new LinkedHashMap<>();
            for (RunMode mode : RunMode.values()) {
                boolean storePWD = false; //This will now always be set to false as a security request! Either use keyfiles or type it in.
                //Roddy.getApplicationProperty(mode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, Boolean.FALSE.toString()).equals(Boolean.TRUE.toString());
                tempPasswords.put(mode, getApplicationProperty(mode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD));
                if (!storePWD)
                    setApplicationProperty(mode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, "");
            }
            applicationProperties.store(stream, "No comment");
            stream.close();
            for (RunMode mode : RunMode.values()) {
                boolean storePWD = false;// Roddy.getApplicationProperty(mode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, Boolean.FALSE.toString()).equals(Boolean.TRUE.toString());
                if (!storePWD)
                    setApplicationProperty(mode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, tempPasswords.get(runMode));
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
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

        if (!applicationProperties.containsKey(pName))
            applicationProperties.setProperty(pName, defaultValue);
        return applicationProperties.getProperty(pName, defaultValue);
    }

    public static RunMode getRunMode() {
        return runMode;
    }

    public static void setApplicationProperty(RunMode runMode, String appPropertyExecutionServiceClass, String text) {
        setApplicationProperty(runMode.name() + "." + appPropertyExecutionServiceClass, text);
    }

    public static void setApplicationProperty(String pName, String value) {
        applicationProperties.setProperty(pName, value);
    }

    public static File getPropertiesFilePath() {
        if (customPropertiesFile == null) customPropertiesFile = Constants.APP_PROPERTIES_FILENAME;
        File _customPropertiesFile = new File("" + customPropertiesFile);
        if (_customPropertiesFile.exists())
            return _customPropertiesFile;
        _customPropertiesFile = new File(getSettingsDirectory(), _customPropertiesFile.getName());
        if (_customPropertiesFile.exists()) {
            return _customPropertiesFile;
        }
        _customPropertiesFile = new File(getApplicationDirectory(), _customPropertiesFile.getName());
        if (_customPropertiesFile.exists()) {
            return _customPropertiesFile;
        }
        logger.postAlwaysInfo("The configuration file " + customPropertiesFile + " does not exist in any known location and will not be loaded. A default configuration will be created and used.");
        return new File(getSettingsDirectory().getAbsolutePath() + File.separator + Constants.APP_PROPERTIES_FILENAME);
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
        File settingsDirectory = new File(home, ".roddy");
        if (!settingsDirectory.exists()) {
            if (!settingsDirectory.mkdir()) {
                throw new IllegalStateException(settingsDirectory.toString());
            }
        }
        return settingsDirectory;
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

    public static List<File> getPluginDirectories() {
        String[] split;
        if(getCommandLineCall().isOptionSet(RoddyStartupOptions.pluginDirectories))
            split = getCommandLineCall().getOptionValue(RoddyStartupOptions.pluginDirectories).split(StringConstants.SPLIT_COMMA);
        else
            split = Roddy.getApplicationProperty(Constants.APP_PROPERTY_PLUGIN_DIRECTORIES, "").split(StringConstants.SPLIT_COLON);

        List<File> folders = new LinkedList<>();
        for (String s : split) {
            File f = new File(s);
            if (f.exists())
                folders.add(f);
        }
        File pFolder = RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "plugins");
        File pFolder2 = RoddyIOHelperMethods.assembleLocalPath(getApplicationDirectory(), "dist", "plugins");
        if (pFolder.exists()) folders.add(pFolder);
        if (pFolder2.exists()) folders.add(pFolder2);
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

    public static String getUsedRoddyVersion() { return getApplicationProperty("useRoddyVersion", "current"); }

    public static String getUsedPluginVersion(String pluginID) {

        String[] pluginVersionEntries;
        if(getCommandLineCall().isOptionSet(RoddyStartupOptions.usePluginVersion))
            pluginVersionEntries = getCommandLineCall().getOptionValue(RoddyStartupOptions.usePluginVersion).split(StringConstants.SPLIT_COMMA);
        else
            pluginVersionEntries = getApplicationProperty(RoddyStartupOptions.usePluginVersion.name()).split(StringConstants.SPLIT_COMMA);
        Map<String, String> pluginVersions = new LinkedHashMap<>();
        for (String pluginVersionEntry : pluginVersionEntries) {
            if(RoddyConversionHelperMethods.isNullOrEmpty(pluginVersionEntry)) continue;
            String[] versionString = pluginVersionEntry.split(StringConstants.SPLIT_COLON);
            pluginVersions.put(versionString[0], versionString[1]);
        }
        if(pluginVersions.containsKey(pluginID))
            return pluginVersions.get(pluginID);
        return "current";
    }

    public static RoddyCLIClient.CommandLineCall getCommandLineCall() { return commandLineCall; }

    public enum RunMode {
        UI,
        CLI
    }

}