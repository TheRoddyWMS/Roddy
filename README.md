== Overview



== Contents



== Directory Structure

RoddyCore   The core Roddy project with the include CLI client.

RoddyGUI    Roddys graphical user interface (Currently not in use)

dist 	    A ready to use version of roddy (without jre or groovy)
            The folder also contains Roddy jar files with version tags.

dist/plugins
            The folder contains basic Roddy plugins
            It also might contain zip files with different version tags.

dist/plugins/PluginBase
	        A basic project from which plugins must be derived./dist/plugins/Template
	        A template plugin which can be used for your own development.

dist/plugins/Template
	        A template plugin which you can use to create your own plugins or workflows.
	        This also gets used if you call the createnewworkflow option.

== Available configuration flags for project configuration files

ID                                      Default Description
debugOptionsUsePipefail                 true    Enable process termination for pipes
debugOptionsUseVerboseOutput            true    Print a lot, like "set -v" in bash
debugOptionsUseExecuteOutput            true    Print executed lines, like "set -x" in bash
debugOptionsUseUndefinedVariableBreak   false   Fail, if a variable in a script is missing, like "set -u" in bash
debugOptionsUseExitOnError              false   Fail, if a script throws an error, like "set -e" in bash
debugOptionsParseScripts                false

processOptionsSetUserGroup              true    Overrides the users default group on the target system
processOptionsSetUserMask               true    Overrides the users default usermask on the target system
processOptionsQueryEnv                  false   Call something like env (in bash) on the target system
processOptionsQueryID                   false

== Work in progress

This section contains features which are currently in development. Testable / active features
are in the changelist.

- (WIP) Roddy binaries, scripts and libraries will be stored in a different directory.
  Insided dist, there will be several bin/{version} folders like 2.1.49. Inside those, the binary and necessary 
  dependencies are store like i.e.:
	dist/bin/2.1.49/Roddy.jar
                       /helperScripts
                       /lib
  Regarding Runtime libraries handling like groovy and the JDK there is no good solution yet.

- (WIP) Roddy accepts a lot more parameters which might otherwise be configured with the
  application properties file:
    useRoddyVersion, usePluginVersion,
    pluginDirectories, configurationDirectories
    commandFactoryClass,
    executionServiceClass, executionServiceAuth, executionServiceHost, executionServiceUser
  Those parameters all override the settings in the application properties file

== Changelist

* Version update to 2.1.61

* Version update to 2.1.49

- (TEST) Roddy will perform a lot more checks before starting a workflow. These checks include file system
  checks, file availability and some more. 

- (TEST) Roddy will delete an execution directory if no jobs were submitted. This is not working
  with the SSH library SFTP client for unknown reasons.

- (TEST) The way how libraries / plugins are loaded is changed. The used plugins / version can
  be specified (descending priority):
  - A project configuration analysis import:
    <analysis id='snvCalling' configuration='snvCallingAnalysis' useplugin="COWorkflows:1.0.114,..."/>
  - On the command line with --usePluginVersion=...
  - The application ini file: usePluginVersion=COWorkflows:1.0.114,...
  Roddy will try to load plugins which are linked to the set plugins. If a plugin is missing, this
  might lead to problems.

  If the used plugins are not set, Roddy will try to load all plugins in
  their latest version.

* Version update to 2.1.28

* Version update to 2.1.27

- Bugfix: There was an error in some rare cases with the job wrapper script. In those rare
  cases, the code to query the last jobstates failed and the script was not started. This only
  occurred when using SGE. 

* Version update to 2.1.26

- (TEST) Roddy tries to create all files with the proper group rights and settings. This
  is mostly tested for PBS based execution. The change of settings includes files in
  .roddyExecutionDirectory as well as those in the roddyExecutionStore
  File settings are set recursively where this is necessary.

* Version update to 2.1.24

- (TEST) You can use Roddy to create new workflows. Use the createworkflow option for that:
  bash roddy.sh createworkflow (pluginID) [native:](workflowID)

- (TEST)The application properties ini readout is changed in such a way, that the users user
  name will be used if USERNAME or "" is supplied. This will also be the default parameter.

* Version update to 2.1.14

- (TEST) There is now a basic support for native workflows. Though the workflow scripts still
  need to be adapted somehow, the new mechanism could make it very easy to integrate
  non java workflows.
  Create an analysis configuration with the following header content:
    listOfUsedTools="nativeWorkflow"
    nativeWorkflowTool="nativeWorkflow"
    targetCommandFactory="de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommandFactory"
  Do not forget to put in the workflow script!
  This is basically working, however parameters might not be passed yet and the tool is only taken
  from the job name. This might confuse people!

- Roddy now supports the parameter --useRoddyVersion= from the command line, it overrides
  the settings in the application properties ini file

* Version update to 2.1.8