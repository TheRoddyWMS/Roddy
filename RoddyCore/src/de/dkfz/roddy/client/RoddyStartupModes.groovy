/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client

import de.dkfz.roddy.Constants
import groovy.transform.CompileStatic

import static de.dkfz.roddy.client.RoddyStartupModeScopes.*
import static de.dkfz.roddy.client.RoddyStartupOptions.*

/**
 * Contains the possible startup modes for Roddy.
 */
@CompileStatic
enum RoddyStartupModes {

    importWorkflow(SCOPE_REDUCED),

    help(SCOPE_CLI),

    showReadme(SCOPE_REDUCED),

    showFeaturetoggles(SCOPE_REDUCED),

    printAppConfig(SCOPE_REDUCED, [config]),

    showConfigPaths(SCOPE_REDUCED, [config,
                                    verbosityLevel]),

    prepareProjectConfig(SCOPE_REDUCED),

    printPluginReadme(SCOPE_CLI, [config]),

    printAnalysisXml(SCOPE_CLI, [config]),

    pluginInfo(SCOPE_REDUCED, [config]),

    compilePlugin(SCOPE_CLI, [config]),

    updatePlugin(SCOPE_CLI, [config]),

    validateConfig(SCOPE_REDUCED, [config]),

    printRuntimeConfig(SCOPE_FULL, [config,
                                    showEntrySources,
                                    extendedList]),

    printIdLessRuntimeConfig(SCOPE_REDUCED, [config]),

    listWorkflows(SCOPE_REDUCED, [config,
                                  shortList]),

    listDatasets(SCOPE_FULL, [config]),

    autoselect(SCOPE_REDUCED, [config]),

    run(SCOPE_FULL_WITHJOBMANAGER, [test,
                                    config,
                                    verbosityLevel,
                                    debugOptions,
                                    waitForJobs,
                                    ioDir,
                                    disableTrackOnlyUserJobs,
                                    trackOnlyStartedJobs,
                                    resubmitJobOnError,
                                    autoSubmit,
                                    autoCleanup,
                                    run,
                                    dontRun] as List<RoddyStartupOptions>),

    rerun(SCOPE_FULL_WITHJOBMANAGER, [test,
                                      run,
                                      dontRun,
                                      config,
                                      verbosityLevel,
                                      debugOptions,
                                      waitForJobs,
                                      ioDir,
                                      disableTrackOnlyUserJobs,
                                      trackOnlyStartedJobs,
                                      resubmitJobOnError,
                                      autoSubmit,
                                      autoCleanup] as List<RoddyStartupOptions>),

    testRun(SCOPE_FULL, [config,
                         verbosityLevel,
                         debugOptions,
                         waitForJobs,
                         ioDir,
                         disableTrackOnlyUserJobs,
                         trackOnlyStartedJobs,
                         resubmitJobOnError,
                         autoSubmit,
                         autoCleanup,
                         run,
                         dontRun] as List<RoddyStartupOptions>),

    testRerun(SCOPE_FULL, [config,
                           verbosityLevel,
                           debugOptions,
                           waitForJobs,
                           ioDir,
                           disableTrackOnlyUserJobs,
                           trackOnlyStartedJobs,
                           resubmitJobOnError,
                           autoSubmit,
                           autoCleanup,
                           run,
                           dontRun] as List<RoddyStartupOptions>),

    rerunStep(SCOPE_FULL_WITHJOBMANAGER, [config,
                                          verbosityLevel,
                                          debugOptions,
                                          waitForJobs,
                                          ioDir,
                                          resubmitJobOnError] as List<RoddyStartupOptions>), // rerun a single step of an executed dataset.

    checkWorkflowStatus(SCOPE_FULL_WITHJOBMANAGER, [config,
                                                    verbosityLevel,
                                                    detailed] as List<RoddyStartupOptions>), // Show the (last) status of an executed dataset.

    cleanup(SCOPE_FULL_WITHJOBMANAGER, [config,
                                        verbosityLevel]),

    abort(SCOPE_FULL_WITHJOBMANAGER, [config,
                                      verbosityLevel]),

    rmi(SCOPE_FULL, [config]),

    compile(SCOPE_CLI),

    pack(SCOPE_CLI),

    packPlugin(SCOPE_CLI, [config]),

    showConfig(SCOPE_REDUCED, [config]),

    createWorkflow(SCOPE_REDUCED, [config])

    //    setup(SCOPE_CLI)

    ;


    public final RoddyStartupModeScopes scope

    /**
     * A list of valid startup options for this startup mode.
     */
    private List<RoddyStartupOptions> validOptions

    RoddyStartupModes(RoddyStartupModeScopes scope, List<RoddyStartupOptions> validOptions = []) {
        this.validOptions = validOptions
        this.scope = scope
    }

    static Optional<RoddyStartupModes> fromString(String name) {
        Map<String, RoddyStartupModes> startupModes =
                RoddyStartupModes.values().collectEntries { [it.name().toLowerCase(), it] }
        return Optional.ofNullable(startupModes.get(name.toLowerCase(), null))
    }

    private static void printCommand(RoddyStartupModes option, String parameters) {
        System.out.println(String.format("  %s %s " + Constants.ENV_LINESEPARATOR, option.toString(), parameters))
    }

    private static void printCommand(RoddyStartupModes option, String parameters, String... description) {
        StringBuilder formattedDescription = new StringBuilder()
        if (description.length > 0) {
            formattedDescription.append(description[0])
            for (int i = 1; i < description.length; i++) {
                formattedDescription.append(Constants.ENV_LINESEPARATOR).append("\t").append(description[i])
            }
        }
        System.out.println(String.format("  %s %s " + Constants.ENV_LINESEPARATOR + "\t%s" + Constants.ENV_LINESEPARATOR, option.toString(), parameters, formattedDescription.toString()))
    }

    static void printCommands(List<List<Object>> commands) {
        commands.each { List<Object> it ->
            printCommand(it[0] as RoddyStartupModes, it[1] as String, it[2..-1] as String[])
        }
    }

    static void printCommandLineOptions() {
        System.out.println(
            [
            Constants.ENV_LINESEPARATOR
            , "Roddy is supposed to be a rapid development and management platform for cluster based workflows."
            , "The current supported ways of execution are:"
            , " - job submission using qsub with PBS or SGE"
            , " - monolithic, direct execution of jobs via Roddy"
            , " - submission or execution on the local machine or via SSH"
            , Constants.ENV_LINESEPARATOR
            , "  To support you with your workflows, Roddy offers you several options:"
           ].join(Constants.ENV_LINESEPARATOR))
        System.out.println()

        System.out.println("The following startup modes are all case-insensitive.")
        printCommands([
                [help,
                 "",
                 "Shows a list of available configuration files in all configured paths."],
                [printAppConfig,
                 "[--config={file}]",
                 "Prints the currently loaded application properties ini file."],
                [showConfigPaths,
                 "[--config={file}]",
                 "Shows a list of available configuration files in all configured paths."],
                [showFeaturetoggles,
                 "[--config={file}]",
                 "Shows a list of available feature toggles."],
                [prepareProjectConfig,
                 "[--config={file}]",
                 "Create or update a project xml file and an application properties ini file."],
                [pluginInfo,
                 "[--config={file}]",
                 "Shows details about the available plugins."],
                [printPluginReadme,
                 "(configuration@analysis) [--config={file}]",
                 "Prints the readme file of the currently selected workflow."],
                [printAnalysisXml,
                 "(configuration@analysis) [--config={file}]",
                 "Prints the analysis xml file of the currently selected workflow."],
                [validateConfig,
                 "(configuration@analysis) [--config={file}]",
                 "Tries to find errors in the specified configuration and shows them."],
                [listWorkflows,
                 "[filter word] [--shortList] [--config={file}]",
                 "Shows a list of available configurations and analyses.",
                 "If a filter word is specified, then the whole configuration tree is only printed", "if at least one configuration id in the tree contains the word."],
                [listDatasets,
                 "(configuration@analysis) [--config={file}]",
                 "Lists the available datasets for a configuration."],
                [printRuntimeConfig,
                 "(configuration@analysis) [pid_0,..,pid_n] [--config={file}] [--extendedList] [--showEntrySources]",
                 "Basically calls testrun but prints out the converted / prepared runtime configuration script content.",
                 "--extendedList shows all stored values (also e.g. tool entries. Works only in combination with --showEntrySources",
                 "--showEntrySources shows the source file of the entry in addition to the value."],
                [printIdLessRuntimeConfig,
                 "(configuration@analysis) [--config={file}] [--extendedList] [--showEntrySources]",
                 "Like printRuntimeConfig, but no dataset ID needs to be provided. All ID-related fields will be left empty."],
                [testRun,
                 "(configuration@analysis) [pid_0,..,pid_n] [--config={file}]",
                 "Displays the current workflow status for the given datasets."],
                [testRerun,
                 "(configuration@analysis) [pid_0,..,pid_n] [--config={file}]",
                 "Displays the current workflow status for the given datasets."],
                [run,
                 "(configuration@analysis) [pid_0,..,pid_n] [--waitForJobs] [--config={file}]",
                 "Runs a workflow with the configured JobFactory.",
                 "Does not check if the workflow is already running on the cluster."],
                [rerun,
                 "(configuration@analysis) [pid_0,..,pid_n] [--waitForJobs] [--config={file}]",
                 "Reruns a workflow starting only the parts which did not produce valid files.",
                 "Does not check if the workflow is already running on the cluster."],
                [cleanup,
                 "(configuration@analysis) [pid_0,..,pid_n] [--config={file}]",
                 "Calls a workflows cleanup method or a setup cleanup script to clean (i.e. remove or set to file size zero) output files."],
                [abort,
                 "(configuration@analysis) [pid_0,..,pid_n] [--config={file}]",
                 "Aborts the running jobs of a workflow for a pid."],
                [checkWorkflowStatus,
                 "(configuration@analysis) [pid_0,..,pid_n] [--detailed] [--config={file}]",
                 "Shows a generic overview about all datasets for a configuration",
                 "If some datasets are selected, a more detailed output is generated.",
                 "If detailed is set, information about all started jobs and their status is shown."],
//                [RoddyStartupModes.setup, "[--config={file}]", "Sets up Roddy for command line execution."],
//                [RoddyStartupModes.ui, "[--config={file}]", "Open Roddy's graphical user interface."]
        ])

        println("== Advanced developer options ==\n")
        printCommands([
                [compile,
                 "",
                 "Compiles the roddy library / application and updates the dist/bin/develop directory."],
                [pack,
                 "",
                 "Creates a copy of the development version and puts the version number to the file name."],
                [compilePlugin,
                 "(plugin ID) [--config={file}]", "Compiles a plugin."],
                [packPlugin,
                 "(plugin ID) [--config={file}]",
                 "Packages the compiled plugin in dist/plugins and creates a version number for it.",
                 "Please note that you can indeed override contents of a zip file if you do not update / compile the plugin jar!"]
        ])

        println("================================")
        System.out.println(Constants.ENV_LINESEPARATOR +
                           Constants.ENV_LINESEPARATOR +
                           "Common additional options. Option names are case-insensitive." +
                            Constants.ENV_LINESEPARATOR)
        System.out.println([
                "    --useconfig={file}              - Use {file} as the application configuration.",  // config
                "    --c={file}                         The order is: full path, .roddy folder, Roddy directory.",
                "    --verbositylevel={1,3,5}        - Set how much Roddy will print to the console, 1 is default, 3 is more, 5 is a lot.",   // verbosityLevel
                "    --v                                Set verbosity to 3.",
                "    --vv                               Set verbosity to 5.",
                "    --useiodir=[fileIn],{fileOut}   - Use fileIn/fileOut as the base input and output directories for your project.",  // ioDir
                "                                       If fileOut is not specified, fileIn is used for that as well.",
                "                                       format can be: tsv, csv or excel",
                "    --usemetadatatable={file},[format]",  // metadataTable
                "                                    - Tell Roddy to use an input table to load metadata and input data and available datasets.",
                "                                      Format can be 'tsv', 'csv' or 'excel'. By default 'tsv' is assumed.",
                "    --waitforjobs                   - Let Roddy wait for all submitted jobs to finish.",  // waitForJobs
                "    --disabletrackonlyuserjobs      - By default, Roddy will only track jobs of the current user. The switch tells Roddy to track all jobs.",   // disableTrackOnlyUserJobs
                "    --disablestrictfilechecks       - Tell Roddy to ignore missing files. By default, Roddy checks if all necessary files exist.",  // disableStrictFilechecks
                "    --ignoreconfigurationerrors     - Tell Roddy to ignore configuration errors. By default, Roddy will exit if configuration errors are detected.",  // ignoreConfigurationErrors
                "    --ignorecvalueduplicates        - Tell Roddy to ignore duplicate configuration values within the same configuration value block.",  // ignoreCValueDuplicates
                "                                       errors. By default, Roddy will exit if duplicates are found.",
                "    --forcenativepluginconversion   - Tell Roddy to override any existing converted Native plugin. By default Roddy will prevent this.",  // forceNativePluginConversion
                "    --forcekeepexecutiondirectory   - Tell Roddy to keep execution directories. By default Roddy will delete them, if no jobs were executed in a run.",  // forceKeepExecutionDirectory
                "    --useRoddyVersion=(version no)  - Use a specific roddy version.",  // roddyVersion
                "    --usefeaturetoggleconfig={file} - Some development and backward-compatibility options can be set here. See showFeatureToggles mode.",
                "    --rv=(version no)",
                "    --usePluginVersion=(...,...)     - Supply a list of used plugins and versions.",  // pluginVersion
                "    --configurationDirectories={path},... ",
                "                                    - Supply a list of configuration directories.",  // configurationDirectories
                "    --pluginDirectories={path},...  - Supply a list of plugin directories.",  // pluginDirectories
                "    --resourcessize={s,m,l,xl}      - Override the used resources size configured in the plugin- or project configuration.",  // resourcesSize
        ].join(Constants.ENV_LINESEPARATOR)
        )

        System.out.println(Constants.ENV_LINESEPARATOR +
                           Constants.ENV_LINESEPARATOR +
                           "Roddy version " +
                           Constants.APP_CURRENT_VERSION_STRING +
                           " build at " +
                           Constants.APP_CURRENT_VERSION_BUILD_DATE)
    }

    boolean needsJobManager() {
        return scope.needsJobManager
    }

    @Override
    String toString() {
        return this.name().replace("_", "")
    }
}
