package de.dkfz.roddy.config;

/**
 * @Michael: What is the difference between CFG_, RODDY_, CVALUE_ prefixes?
 */
public class ConfigurationConstants {
    @ConfigurationConstant
    public static final String CFG_INPUT_BASE_DIRECTORY = "inputBaseDirectory";
    @ConfigurationConstant
    public static final String CFG_OUTPUT_BASE_DIRECTORY = "outputBaseDirectory";
    @ConfigurationConstant
    public static final String CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY = "outputAnalysisBaseDirectory";
    @ConfigurationConstant
    //    public static final String CFG_SEQUENCER_PROTOCOL = "sequencerProtocol";
    public static final String RODDY_EXEC_DIR_PREFIX = "exec_";
    public static final String RODDY_EXEC_CACHE_FILE = ".roddyExecCache.txt";
    public static final String RODDY_JOBSTATE_LOGFILE = "jobStateLogfile.txt";

    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_USE_PIPEFAIL = "debugOptionsUsePipefail";
    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_USE_VERBOSE_OUTPUT = "debugOptionsUseVerboseOutput";
    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_USE_EXECUTE_OUTPUT = "debugOptionsUseExecuteOutput";
    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_USE_UNDEFINED_VARIABLE_BREAK = "debugOptionsUseUndefinedVariableBreak";
    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_USE_EXIT_ON_ERROR = "debugOptionsUseExitOnError";
    @ConfigurationConstant
    public static final String DEBUG_OPTIONS_PARSE_SCRIPTS = "debugOptionsParseScripts";

    public static final String CVALUE_PLACEHOLDER_EXECUTION_DIRECTORY = "$PWD";
    public static final String CVALUE_PLACEHOLDER_RODDY_JOBID = "${RODDY_JOBID}";
    public static final String CVALUE_PLACEHOLDER_RODDY_JOBARRAYINDEX = "${RODDY_JOBARRAYINDEX}";
    public static final String CVALUE_TYPE_BASH_ARRAY = "bashArray";
    public static final String CVALUE_TYPE_PATH = "path";
    public static final String CVALUE_TYPE_STRING = "string";
    public static final String CVALUE_TYPE_BOOLEAN = "boolean";

    public static final String CVALUE_PREFIX_BASEPATH = "BASEPATH_";
    public static final String CVALUE_PREFIX_TOOL = "TOOL_";
    public static final String CVALUE_SUFFIX_BINARY = "_BINARY";
    public static final String CVALUE_SUFFIX_BINARY_SHORT = "_BIN";

    @ConfigurationConstant
    public static final String CVALUE_PROCESS_OPTIONS_SETUSERGROUP = "processOptionsSetUserGroup";
    @ConfigurationConstant
    public static final String CVALUE_PROCESS_OPTIONS_SETUSERMASK = "processOptionsSetUserMask";
    @ConfigurationConstant
    public static final String CVALUE_PROCESS_OPTIONS_QUERY_ENV = "processOptionsQueryEnv";
    @ConfigurationConstant
    public static final String CVALUE_PROCESS_OPTIONS_QUERY_ID = "processOptionsQueryID";

    public static final String CFG_ALLOW_ACCESS_RIGHTS_MODIFICATION = "outputAllowAccessRightsModification";
    public static final String CFG_OUTPUT_ACCESS_RIGHTS_FOR_DIRECTORIES = "outputAccessRightsForDirectories";
    public static final String CFG_OUTPUT_ACCESS_RIGHTS = "outputAccessRights";
    public static final String CFG_OUTPUT_FILE_GROUP = "outputFileGroup";

    public static final String CFG_PREVENT_JOB_EXECUTION = "preventJobExecution";
    public static final String CFG_USE_CENTRAL_ANALYSIS_ARCHIVE = "useCentralAnalysisArchive";
    public static final String CFG_OUTPUT_UMASK = "outputUMask";


}
