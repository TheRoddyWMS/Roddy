/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy;

/**
 * Several publically available constants for the Roddy framework
 * The build strings are automatically updated on command line compilation.
 */
public class Constants {

    /////////////////////////
    // Application constants
    /////////////////////////

    public static final String APP_CURRENT_VERSION_STRING = "3.5.1";
    public static final String APP_CURRENT_VERSION_BUILD_DATE = "Mon Feb 18 11:03:49 CET 2019";
    public static final String APP_PROPERTY_JOB_MANAGER_CLASS = "jobManagerClass";
    public static final String APP_PROPERTY_FILESYSTEM_ACCESS_MANAGER_CLASS = "fileSystemAccessManagerClass";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_CLASS = "executionServiceClass";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_USER = "executionServiceUser";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_HOSTS = "executionServiceHost";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_USE_COMPRESSION = "executionServiceUseCompression";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD = "executionServiceAuth";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD = "executionServicePasswd";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD = "executionServiceStorePassword";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD = "password";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_SSHAGENT = "sshagent";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE = "keyfile";
    public static final String APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE_LOCATION = "executionServiceKeyfileLocation";
    public static final String APP_PROPERTY_CONFIGURATION_DIRECTORIES = "configurationDirectories";
    public static final String APP_PROPERTY_PLUGIN_DIRECTORIES = "pluginDirectories";
    public static final String APP_PROPERTIES_FILENAME = "applicationProperties.ini";
    public static final String APP_PROPERTY_NET_USEPROXY = "netUseProxy";
    public static final String APP_PROPERTY_NET_PROXY_ADDRESS = "netProxyAddress";
    public static final String APP_PROPERTY_NET_PROXY_USR = "netProxyUser";
    public static final String APP_PROPERTY_SCRATCH_BASE_DIRECTORY = "scratchBaseDirectory";
    public static final String APP_PROPERTY_JOB_MANAGER_PASS_ENVIRONMENT = "jobManagerPassEnvironment";
    public static final String APP_PROPERTY_JOB_MANAGER_HOLDJOBS_ON_SUBMISSION = "jobManagerHoldJobsOnSubmission";
    public static final String APP_PROPERTY_JOB_MANAGER_UPDATE_INTERVAL = "jobManagerUpdateInterval";
    public static final String APP_PROPERTY_BASE_ENVIRONMENT_SCRIPT = "baseEnvironmentScript";
    public static final String APP_PROPERTY_TESTRUN_VARIABLE_WIDTH = "testRunVariableWidth";

    /////////////////////////
    // Error messages
    /////////////////////////

    public static final String ERR_MSG_ONLY_ONE_JOB_ALLOWED = "A job object is not allowed to run several times.";
    public static final String ERR_MSG_WRONG_PARAMETER_COUNT = "You did not provide proper parameters, args.length = ";

    /////////////////////////
    // Environment settings
    /////////////////////////

    public static final String ENV_LINESEPARATOR = SystemProperties.getLineSeparator();

    /////////////////////////
    // Other constants
    /////////////////////////

    public static final String UNKNOWN_USER = "UNKNOWN";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String NO_VALUE = "<NO_VALUE>";
    public static final String USERNAME = "USERNAME";
    public static final String USERGROUP = "USERGROUP";
    public static final String USERHOME = "USERHOME";
    public static final String DEFAULT = "default";

    public static final String RODDY_PARENT_JOBS = "RODDY_PARENT_JOBS";
    public static final String CONFIG_FILE = "CONFIG_FILE";
    public static final String PARAMETER_FILE = "PARAMETER_FILE";
    public static final String ANALYSIS_DIR = "ANALYSIS_DIR";
    public static final String PARAMETER_FILE_SUFFIX = ".parameters";
    public static final String PROJECT_NAME = "projectName";
    public static final String DATASET = "dataSet";
    public static final String DATASET_HR = "dataset";
    public static final String DATASET_CAP = "DATASET";
    @Deprecated
    public static final String PID_CAP = "PID";
    @Deprecated
    public static final String PID = "pid";

    public static final String AUTO_FILENAME_SUFFIX = ".auto";

    /////////////////////////
    // Bash configs
    /////////////////////////

    public static final String RODDY_CONFIGURATION_MAGICSTRING = "Roddy configuration";
}
