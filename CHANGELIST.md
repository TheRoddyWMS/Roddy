# Work in progress

* 3.5.0

  - Fixed non-quoting of variables with spaces. Now they are quoted and thus correctly interpreted as string variables instead of Bash array variables that are not correctly exported due to the Bash bug.
  
  - Added escaping of not already quoted variables containing double quotes as quoting these could yield wrong results (e.g. in the context of named Bash bug).  
  
  - Allow for "selectionTag" in filename patterns and output file tags
  
  - Correctly processes selectionTag for onScriptParameter filename pattern matching
  
  - Added feature toggle `StrictOnScriptParameterSelectionTag` with default `false`; with `false` default selection tag values always match; with `true`, which is planned to be the Roddy 4, "default" selection tag values in output file declarations only match "default" selection tags in filename patterns
  
  - Improved parsing of `--cvalues` command-line argument solving issues with colons ':' in configuration variables
  
  - Removed ModifiedVariablePassing feature toggle
  
  - Fixed a new regression with not quoting bash/map variables in the job-parameter files
  
  - Lots of minor improvements of (error) output and behaviour
  
  - Updated BatchEuphoria dependency
  
  - Added `testRunVariableWidth` application property to change the width of the printing of job-parameter names. Defaults to old values 25.
  
  - Update to Gradle 5.1.1 
  
* 3.4.2

  - Fix a bug which made CValue.toFile() output wrong paths
  
* 3.4.1

  - Add JSON configuration file loader and tests
  
* 3.4.0

  - Improved validation of configuration values.

  - ForbidSubmissionToRunning was re-enabled and turned on by default.
  
  - Created ExitReasons class for usage within Roddy and plugins. The class stores different exit codes and messages.
  
  - Bugfixes for local job execution and cluster job submission
  
  - Updated to BatchEuphoria 0.0.6.
  
  - Follow symbolic links when listing files in directories, e.g. collecting dataset names.
  
  - Added a base class for Spock tests: RoddyTestSpec.
  
  - Updated copyright notices.
  
  - Improved test coverage.

* 3.3.3

  - Further improved error reporting
  
  - Increased default JVM memory size to 1G
  
  - Improved robustness

* 3.3.2

  - Improved directory checks
  
  - Improved error reporting

* 3.3.1

  - Updated to RoddyToolLib 2.0.0
  
  - Updated to BatchEuphoria 0.0.5
  
  - Fixed autofilename bug

* 3.3.0 (misreports itself as 3.2.3)

  - Bugfixes to get Sophia 2.0.0 running
  
  - Added more (longer) tuple classes for jobs with more inputs/outputs
  
  - Mis-tagged this release as 3.2.3!

* 3.2.2

  - Fixed context warnings that were mis-represented as errors
  
  - Bugfixes to get BamToFastqPlugin running
  
  - Fixed FailOnAutoFilenames feature toggle usage
   
  - Improved error reporting (e.g. report .roddy/ logfile, configuration errors)
  
  - Fixed bug in remote file-existence checks
  
  - Sphinxs documentation with plantuml plugin support

* 3.2.1

  - FileOnErroneousDryRuns feature toggle
  
  - Extended plugin API
  
  - Fixed rare MD5 file check bug during tool compression: locally modified plugins could result in always uploading known plugin versions
  
  - Improved error reporting

* 3.2.0

  - Improved loading of cohorts and supercohorts
  
  - Improved cyclic dependency detection in configuration variables
  
  - testrerun shows all configuration again
  
  - Stricter conversion of Sphinx documentation
  
  - Allow FileGroup in tool tuple output
  
  - Allow selectiontag in FileGroup

* 3.1.0

  - Improved cohort handling
  
  - Supercohorts
   
  - RoddyToolLib 1.0.0
  
  - ContextResource for simpler test writing
  
  - No JDK version check for plugins anymore
  
  - Improved unknow libdir problem during roddy.sh compile

* 3.0.11

  - Fix printidlessruntimeconfig
  
  - Backported ContextResource from 3.1.0 version bump
  
  - IO-dir checks also in other modes but run

* 3.0.10

  - Backported tool-compression bug related to MD5 sum creation from 3.2.1 

* 3.0.9

  - Update to RoddyToolLib 2.0.0

* 3.0.8

  - Boolean configuration value `1` was incorrectly interpreted as `false`. Fixed to be interpreted as `true`. Added warnings that on the long run only "true" and "false" will be allowed values for Booleans.

  - Grid Engine Support restored

  - Bugfixes
  
  - Start-up script reads `INCREASE_BUILD_NUMBER` (true, false) variable defaulting to true, to allow turning off automatic build-number increase.
  
  - Basic implementation of a new Brawl DSL based on Groovy. 
  
  - Removed `preventJobExecution` configuration variable

* 3.0.1-3.0.7

  - Bugfix releases
  
  - TheRoddyWMS organization

* 3.0.4 

* 3.0.0

  - Many bugfixes and error message improvements

  - Split-off BatchEuphoria and RoddyToolLib from RoddyCore. BatchEuphoria has LSF support

  - Sphinx-based documentation (for ReadTheDocs.org) 

  - building with Gradle
  
  - continuous integration with Travis and deployment to Github Releases
  
  - improved structure and content of output (show plugin directories and loaded XMLs)

* 2.3.187

  - Many bugfixes
  
  - Improved error messages

* 2.3.133

This section contains features which are currently in development. Testable / active features
are in the changelist.
Entries here can be marked with (PLANNED) or (WIP). (TEST) is more for the Changelist.

- (PLANNED) check defined binaries for existence. Isn't this already done in the validation part?

- (PLANNED) enable and disable runFlags with --run=... and --dontrun=...

- (WIP) Roddy accepts a lot more parameters which can otherwise be configured with the
  application properties file:
    - usePluginVersion,
    - commandFactoryClass,
    - executionServiceClass, executionServiceAuth, executionServiceHost, executionServiceUser
    
    Those parameters will all override the settings in the application properties file

- (WIP) Integrate SLURM as a cluster backend

# Changelist

* 2.3.97b

Beta release.
- Working version of new runtime class location implementation.

- Add new printruntimeconfig modes. There are be the two additional modes
    --showsourcenetries : shows values with their file.
    --extendedlist      : as an addition to showsourceentries, shows the
                          full list of values including their predecessors.
- API version checks (Java, Groovy, Roddy, Plugins) 
- Unified portable IntelliJ Idea configs
- Improved logging in ProjectFactory
- Metadata table support
- Colored output

* 2.2.112

* Version update

- Change the behaviour of file access rights setting:
   
    - Only output one error message instead of many
    - Test on startup if access right setting is possible and disable the feature by setting outputAllowAccessRightsModification to false
    - Roddy will check, if the project output folder exists. If it does not exist, it will refuse to run a dataset. This way, the user is forced to create this directory explicitely.

  - Setting the location of .roddyExecutionDirectory is possible. Set RODDY_EXECUTION_DIRECTORY to a (valid) path of your choice in your configuration files. The normal behaviour stays, which is to setup this directory in the project output folder. 
  
  - More checkups will occur, when Roddy starts up. E.g. Roddy will check the accessibility of plugin and configuration directories.

  - We introduce a new branching scheme from now on. There will be the master branch and an additional development branch. The master branch should only contain stable and tested releases of Roddy. development will be there for everything else.

  - (WIP) Roddy will now support plugin revisions and compatibility tracks. This is useful, if you have a plugin which depends on other plugins and which you do not want to recompile/ repack, just because another plugin was extended.
  
    - Plugins can be extended in two ways 
        - horicontal for e.g. hotfixes / revisions and 
        - vertical, for extensions. 
  
    - Vertical Plugin compatibility is done for hotfixes via the directory name:
        - Workflow-1.0.23 (original)
        - Workflow-1.0.23-r1 (revision 1)
    
    - Horizontal Plugin compatibility is done via the buildinfo.txt file:
        - Workflow-1.0.24/buildinfo.txt
            - extendsversion=1.0.23
      
  ![Dependency graph]("documentation/readme_images/Roddy Plan_0.jpg")

* 2.2.87

    - Fix: Fix some bugs regarding brawl workflows.

    - Fix: Fix a bug in testrerun where too much jobs were displayed. This should be fixed now.

    - Update prepareprojectconfig. create and update still exist but you only need to provide the
  target project folder. Roddy will take care of the rest. Both update and create will update
  the configurationDirestories part in the ini file to contain/change the newly created directory

* 2.2.81

    - Fix: Output directories will now always be set to type "path". This will prevent errors for
      output files, if users forget to set this particular variables to the proper type.
    
    - (TEST) Roddy can now create auto filenames, if there was no filename pattern for a file.
      This is currently enabled by default! Auto filenames are placed in the output directory and
      contain the job name, the parameter name, the job id and the suffix .auto.
    
    - (WIP) add the createworkflow runmode to Roddy.
    
    - (TEST) A working and version of Roddys built in Brawl language. Brawl is be a very basic
      language to implement simple workflows. Support is e.g. given for if and a lot of typing can be skipped.
      brawl scripts will be auto converted and compiled to Groovy workflows upon startup. This way it is guaranteed
      that those scripts are written properly. Still missing is the for loop syntax.

* 2.2.66

    - (TEST) Implement the "showreadme" command to show the Roddy README.md
    
    - (TEST) Implement the "printpluginreadme" command to show the readme of a workflow.
             Also put in the "printanalysisxml" to show the analysis xml.
    
    - (TEST) Allow selection of nodes via command line for PBS and SLURM. SGE is currently not supported.
      The configuration value to set is "enforceSubmissionToNodes:<nodeid>;<nodeid>" followed by a semicolon separated list of nodes.
    
    - (TEST) Change output of job ids to use println.
    
    - (TEST) Introduce a killswitch to disable filename imports upon analysis import.

* 2.2.49

    - Add the --extendedlist option to support a very extended view for checkworkflostatus.
      In this view, all previous runs are shown.
    
    - (TEST) Tool entries allow override of old entries without resetting everything.
      A ToolEntry must be defined like this: (with overrideresourcesets="true"!)
              <tool name='fastqc' value='checkFastQC.sh' basepath='qcPipeline' overrideresourcesets="true">
                  <resourcesets>
                      <rset size="l" nodes="1" walltime="1000"/>
                  </resourcesets>
              </tool>
    
    - Move the configuration conversion (bash, xml) to different classes, cleansing the ConfigurationFactory a bit.
    
    - Add the boolean configuration parameter outputAllowAccessRightsModification to allow (default) or disallow access rights modification
      on the target file system.
    
    - queryworkflowstatus now gives a much leaner overview about all datasets of a project.
      There was also a bugfix which stated that some processes were still running though they were obviously crashed.
    
    - The Roddy wrapper script for called scripts contains various new checks which will lead to ABORTED, if the conditions cannot be met.
    
    - The feature toggle file can be selected with --usefeaturetoggleconfig=<file>.
      Single feature toggles can be enabled or disabled with --enabletoggle=<toggle>,<toggle> or --disabletoggle
      The list of possibly available toggle can be queried with the "showfeaturetoggles" start mode
    
    - (TEST) Unzip of plugins is disabled by default now. Can be enabled via feature toggle. (Feature toggle id: UnzipZippedPlugins[def:false])
    
    - (TEST) Pass parameters in a parameter file instead of the normal cli parameter passing. (Feature toggle id: ModifiedVariablePassing[def:false])

* 2.2.8

    - (TEST) Break submission, if an error occurs. If a job cannot be started on a cluster environment, the overall
      job submission will be skipped. Running / Started processes are not affected. (Feature toggle id: BreakSubmissionOnError[def:false])
    
    - (TEST) New app ini loader, does not store files. This part was remove for now.
    
    - (TEST) XML validation will take place when XML files are loaded. (Feature toggle id: XMLValidation[def:true])
    
    - (TEST) Roddy binaries, scripts and libraries will be stored in a different directory.
      Insided dist, there will be several bin/{version} folders like 2.1.49. Inside those, the binary and necessary
      dependencies are store like i.e.:
            dist/bin/2.1.49/Roddy.jar
                           /helperScripts
                           /lib
      Regarding Runtime libraries handling like groovy and the JDK there is no good solution yet.

* 2.1.49

    - Roddy will perform a lot more checks before starting a workflow. These checks include file system
      checks, file availability and some more. 
    
    - Roddy will delete an execution directory if no jobs were submitted. This is not working
      with the SSH library SFTP client for unknown reasons.
    
    - The way how libraries / plugins are loaded is changed. The used plugins / version can
      be specified (descending priority):
        - A project configuration analysis import:
          <analysis id='snvCalling' configuration='snvCallingAnalysis' useplugin="COWorkflows:1.0.114,..."/>
        - On the command line with --usePluginVersion=...
        - The application ini file: usePluginVersion=COWorkflows:1.0.114,...
        Roddy will try to load plugins which are linked to the set plugins. If a plugin is missing, this
        might lead to problems.
      
        If the used plugins are not set, Roddy will try to load all plugins in
        their latest version.

* 2.1.28

* 2.1.27

    - Bugfix: There was an error in some rare cases with the job wrapper script. In those rare
  cases, the code to query the last jobstates failed and the script was not started. This only
  occurred when using SGE. 

* 2.1.26

    - Roddy tries to create all files with the proper group rights and settings. This
  is mostly tested for PBS based execution. The change of settings includes files in
  .roddyExecutionDirectory as well as those in the roddyExecutionStore
  File settings are set recursively where this is necessary.

* 2.1.24

    - (TEST) You can use Roddy to create new workflows. Use the createworkflow option for that:
      bash roddy.sh createworkflow (pluginID) [native:](workflowID)
    
    - (TEST)The application properties ini readout is changed in such a way, that the users user
  name will be used if USERNAME or "" is supplied. This will also be the default parameter.

* 2.1.14

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

* 2.1.8