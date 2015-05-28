package de.dkfz.roddy.client;

import de.dkfz.roddy.Constants;
import static RoddyStartupOptions.*;
import static RoddyStartupModeScopes.*;

/**
 * Contains the possible startup modes for Roddy.
 */
@groovy.transform.CompileStatic
public enum RoddyStartupModes {

    importworkflow(SCOPE_REDUCED),

    help(SCOPE_CLI),

    showfeaturetoggles(SCOPE_REDUCED),

    printappconfig(SCOPE_REDUCED, [useconfig]),

    showconfigpaths(SCOPE_REDUCED, [useconfig, verbositylevel]),

    prepareprojectconfig(SCOPE_REDUCED),

    compile(SCOPE_CLI),

    pack(SCOPE_CLI),

    plugininfo(SCOPE_REDUCED, [useconfig]),

    compileplugin(SCOPE_CLI, [useconfig]),

    updateplugin(SCOPE_CLI, [useconfig]),

    packplugin(SCOPE_CLI, [useconfig]),

    showconfig(SCOPE_REDUCED, [useconfig]),

    validateconfig(SCOPE_REDUCED, [useconfig]),

    printruntimeconfig(SCOPE_FULL, [useconfig]),

    listworkflows(SCOPE_REDUCED, [useconfig, shortlist]),

    listdatasets(SCOPE_FULL, [useconfig]),

    run(SCOPE_FULL, [test, useconfig, verbositylevel, debugOptions, waitforjobs, useiodir, disabletrackonlyuserjobs, trackonlystartedjobs, resubmitjobonerror, autosubmit, autocleanup, run, dontrun]),

    rerun(SCOPE_FULL, [test, run, dontrun, useconfig, verbositylevel, debugOptions, waitforjobs, useiodir, disabletrackonlyuserjobs, trackonlystartedjobs, resubmitjobonerror, autosubmit, autocleanup] as List<RoddyStartupOptions>),

    testrun(SCOPE_FULL, [useconfig, verbositylevel, debugOptions, waitforjobs, useiodir, disabletrackonlyuserjobs, trackonlystartedjobs, resubmitjobonerror, autosubmit, autocleanup, run, dontrun] as List<RoddyStartupOptions>),

    testrerun(SCOPE_FULL, [useconfig, verbositylevel, debugOptions, waitforjobs, useiodir, disabletrackonlyuserjobs, trackonlystartedjobs, resubmitjobonerror, autosubmit, autocleanup, run, dontrun] as List<RoddyStartupOptions>),

    rerunstep(SCOPE_FULL, [useconfig, verbositylevel, debugOptions, waitforjobs, useiodir, resubmitjobonerror] as List<RoddyStartupOptions>), // rerun a single step of an executed dataset.

    checkworkflowstatus(SCOPE_FULL, [useconfig, verbositylevel, detailed] as List<RoddyStartupOptions>), // Show the (last) status of an executed dataset.

    cleanup(SCOPE_FULL, [useconfig, verbositylevel]),

    abort(SCOPE_FULL, [useconfig, verbositylevel]),

    ui(SCOPE_FULL, [useconfig, verbositylevel]),

    setup(SCOPE_CLI);
    //    networksubmissionserver(false),

    //    createtestdata(true),

    //    showstatus(true),

    public final int scope;

    /**
     * A list of valid startup options for this startup mode.
     */
    private List<RoddyStartupOptions> validOptions

    RoddyStartupModes(int scope, List<RoddyStartupOptions> validOptions = []) {
        this.validOptions = validOptions
        this.scope = scope;
    }

    private static void printCommand(RoddyStartupModes option, String parameters) {
        System.out.println(String.format("  %s %s " + Constants.ENV_LINESEPARATOR, option.toString(), parameters));
    }

    private static void printCommand(RoddyStartupModes option, String parameters, String... description) {
        StringBuilder formattedDescription = new StringBuilder();
        if (description.length > 0) {
            formattedDescription.append(description[0]);
            for (int i = 1; i < description.length; i++) {
                formattedDescription.append(Constants.ENV_LINESEPARATOR).append("\t").append(description[i]);
            }
        }
        System.out.println(String.format("  %s %s " + Constants.ENV_LINESEPARATOR + "\t%s" + Constants.ENV_LINESEPARATOR, option.toString(), parameters, formattedDescription.toString()));
    }

    public static void printCommandLineOptions() {
        System.out.println(Constants.ENV_LINESEPARATOR
                + Constants.ENV_LINESEPARATOR + "Roddy is supposed to be a rapid development and management platform for cluster based workflows."
                + Constants.ENV_LINESEPARATOR + "The current supported ways of execution are:"
                + Constants.ENV_LINESEPARATOR + " - job submission using qsub with PBS or SGE"
                + Constants.ENV_LINESEPARATOR + " - monolithic, direct execution of jobs via Roddy"
                + Constants.ENV_LINESEPARATOR + " - submission or execution on the local machine or via SSH"
                + Constants.ENV_LINESEPARATOR + Constants.ENV_LINESEPARATOR + "  To support you with your workflows, Roddy offers you several options:" + Constants.ENV_LINESEPARATOR);

        printCommand(RoddyStartupModes.help, "", "Shows a list of available configuration files in all configured paths.");
        printCommand(RoddyStartupModes.showconfigpaths, "[--useconfig={file}]", "Shows a list of available configuration files in all configured paths.");
        printCommand(RoddyStartupModes.prepareprojectconfig, "", "Create or update a project xml file and an application properties ini file.");
        printCommand(RoddyStartupModes.plugininfo, "[--useconfig={file}]", "Shows details about the available plugins.");
//        printCommand(RoddyStartupModes.showconfig, "[--useconfig={file}]", "Shows a list of available configuration files in all configured paths.");
        printCommand(RoddyStartupModes.validateconfig, "(configuration@analysis) [--useconfig={file}]", "Tries to find errors in the specified configuration and shows them.");
        printCommand(RoddyStartupModes.listworkflows, "[filter word] [--shortlist] [--useconfig={file}]", "Shows a list of available configurations and analyses.", "If a filter word is specified, then the whole configuration tree is only printed", "if at least one configuration id in the tree contains the word.");
        printCommand(RoddyStartupModes.listdatasets, "(configuration@analysis) [--useconfig={file}]", "Lists the available datasets for a configuration.");
        printCommand(RoddyStartupModes.printruntimeconfig, "(configuration@analysis) [--useconfig={file}]", "Basically calls testrun but prints out the converted / prepared runtime configuration script content.");
        printCommand(RoddyStartupModes.testrun, "(configuration@analysis) [pid_0,..,pid_n] [--useconfig={file}]", "Displays the current workflow status for the given datasets.");
        printCommand(RoddyStartupModes.testrerun, "(configuration@analysis) [pid_0,..,pid_n] [--useconfig={file}]", "Displays the current workflow status for the given datasets.");
        printCommand(RoddyStartupModes.run, "(configuration@analysis) [pid_0,..,pid_n] [--waitforjobs] [--useconfig={file}]", "Runs a workflow with the configured Jobfactory.", "Does not check if the workflow is already running on the cluster.");
        printCommand(RoddyStartupModes.rerun, "(configuration@analysis) [pid_0,..,pid_n] [--waitforjobs] [--useconfig={file}]", "Reruns a workflow starting only the parts which did not produce valid files.", "Does not check if the workflow is already running on the cluster.");
        printCommand(RoddyStartupModes.cleanup, "(configuration@analysis) [pid_0,..,pid_n] [--useconfig={file}]", "Calls a workflows cleanup method or a setup cleanup script to clean (i.e. remove or set to file size zero) output files.");
        printCommand(RoddyStartupModes.abort, "(configuration@analysis) [pid_0,..,pid_n] [--useconfig={file}]", "Aborts the running jobs of a workflow for a pid.");

//        printCommand(RoddyStartupModes.rerunstep, "(configuration@analysis) [pid] [--waitforjobs] [--useconfig={file}]", "Reruns a single step of the last execution context.", "The job will be rerun without any dependencies.", "There are no checks if the job is already running.");
        printCommand(RoddyStartupModes.checkworkflowstatus, "(configuration@analysis) [pid_0,..,pid_n] [--detailed] [--useconfig={file}]", "Shows a generic overview about all datasets for a configuration", "If some datasets are selected, a more detailed output is generated.", "If detailed is set, information about all started jobs and their status is shown.");
        printCommand(RoddyStartupModes.setup, "[--useconfig={file}]", "Sets up Roddy for command line execution.");
        printCommand(RoddyStartupModes.ui, "[--useconfig={file}]", "Open Roddys graphical user interface.");
        println("== Advanced developer options ==\n");
        printCommand(RoddyStartupModes.compile, "", "Compiles the roddy library / application.");
        printCommand(RoddyStartupModes.pack, "", "Creates a copy of the current version and puts the version number to the file name.");
        printCommand(RoddyStartupModes.compileplugin, "(plugin ID) [--useconfig={file}]", "Compiles a plugin .");
        printCommand(RoddyStartupModes.packplugin, "(plugin ID) [--useconfig={file}]", "Packages the compiled plugin in dist/plugins and creates a version number for it.", "Please note that you can indeed override contents of a zip file if you do not update / compile the plugin jar!");
        println("================================")
        System.out.println(Constants.ENV_LINESEPARATOR + Constants.ENV_LINESEPARATOR + "Common additional options");
        System.out.println("    --useconfig={file}              - Use {file} as the application configuration.\n" +
                "                                      The order is: full path, .roddy folder, Roddy directory.");
        System.out.println("    --useiodir=[fileIn],{fileOut}   - Use fileIn/fileOut as the base input and output directories for your project.\n" +
                "                                      If fileOut is not specified, fileIn is used for that as well.");
        System.out.println("    --waitforjobs                   - Let Roddy wait for all submitted jobs to finish.");
        System.out.println("    --disabletrackonlyuserjobs      - Default for command line mode is that Roddy only tracks user jobs. This can be changed with this switch.");
        System.out.println("    --useRoddyVersion=(version no)  - Use a specific roddy version.");
        System.out.println("    --usePluginVersion=(...,...)    - Supply a list of used plugins and versions.");
        System.out.println(Constants.ENV_LINESEPARATOR + Constants.ENV_LINESEPARATOR + "Roddy version " + Constants.APP_CURRENT_VERSION_STRING + " build at " + Constants.APP_CURRENT_VERSION_BUILD_DATE);
    }

    public boolean needsFullInit() {
        return scope == SCOPE_FULL;
    }

    @Override
    public String toString() {
        return this.name().replace("_", "");
    }
}
