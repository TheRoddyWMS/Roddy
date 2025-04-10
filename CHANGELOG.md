# Versioning Scheme

Roddy uses semantic versioning at least since version 3. Earlier versions use the same `x.y.z` format, but the version increases were not done according to the rules defined for semantic versioning. 

## Semantic Versioning of Roddy

As a workflow management system, Roddy has the following APIs:

* Interface with the workflows (plugin XMLs, Groovy API, etc.)
* Interface with the caller (user)

The focus for deciding on the correct version level to increase is on the user-interface. The following changes trigger the respective bump:

1. **Major**: Any change that causes users to take action to continue with their work.
    * Parameter name
    * Parameter value interpretation
    * Output change related to job name reporting (required for OTP)
    * Plugin API change causing incompatibility of old plugins
    * Names of files in the `roddyExecutionStore` as far as produced by Roddy itself (`jobStateLogfile.txt`, `*.parameter`, `*.o%j`)
2. **Minor**: Any change that adds a functionality but does not require an action by the user.
    * Plugin API extensions that do not affect old plugins
    * New parameters or features
3. **Patch**: Any change that is functionally neutral. This includes documentation updates, most error reporting changes, refactorings, and stability-improving bugfixes.
    * Safety measures, such as parameter value validation (e.g. parsing)
    * Stability fixes
    * Improved error messages and reporting (except for the job-name reporting mentioned above)
    * Refactorings

## Semantic Versioning of Roddy Workflows

Workflows have the following APIs:

  * Interface of the workflow with the workflow management system
  * Interface with the user

The focus for workflows is on the interface with the user. Workflow system updates are rare, and version increases are complicated by the need to maintain multiple long-term support versions on version branches. 
Therefore, in past cases, special rules were applied.

1. **Major**: Any change that causes users to take action to continue with their work.
    * Workflow parameter name or removed parameters
    * Certain default configuration values
    * Changes in the semantics of a configuration parameter or the interpretation of inputs, e.g. because of algorithm changes
    * Renamed columns in output TSVs
    * Reordering of columns in a TSV file with header or adding columns between existing columns
    * Remove/rename output files
2. **Minor**: Any change that adds a functionality but does not require an action by the user.
    * Safety measures, such as parameter value validation (e.g. parsing)
    * New parameters or features
3. **Patch**: Any change that is functionally neutral. This includes documentation updates, most error reporting changes, refactorings, and stability-improving bugfixes.
    * Stability and bug fixes that allow running the workflow on data on which it previously failed
    * Improved error messages and reporting (except for the job-name reporting mentioned above)
    * Refactorings

Note, that some changes may break fragile user code and thus potentially lead to wrong analysis results. 
This is, e.g., the case if the data user cuts out columns of a TSV without checking the header.


# Changelist

* 3.8.0
  * **Minor**: Singularity support. Jobs can now be run in singularity containers. Currently, only a single image for a whole workflow is supported.
    > This is only implemented and tested for LSF and SLURM, not tested for PBS and SGE, and not implemented at all for the REST-based submission to LSF and the direct execution job manager.
  * **Minor**: Added `group-config.py` script that allows to compile version information reports (JSON) from execution stores.
  * **Minor**: Stricter parsing of `bashArray`. Enclosing characters must now be true `(` and `)` parenthesis. Previously, any character was allow! This is not an API-breaking change, because if you used anything but parentheses, it would not have been a `bashArray`. Roddy now just complains correctly.
  * **Patch**: Change reported error for pattern that cannot be matched to file into warning.
  * **Patch**: Fix problem with parameter-list interpretation during Roddy startup due to incorrect Bash expression.
  * **Patch**: Security-related bumps of some related libraries (org.bouncycastle, org.slf4j).
  * **Patch**: Added `listConfigurations`, `allBoms`, and a `...Bom` task for every Gradle configuration set. The `allBoms` and `...Bom` tasks generate JSON CycloneDX SBOMs in `gradleBuild/reports/cyclonedx`.
  * **Patch**: Some refactorings.
    * Renamed internal names of many CLI options, to get away from options that are difficult to read, because all concatenated lower-case words, or that start with the filler-word "use". Could not change on the external interface though, because of backward compatibility.
    * Code layout changes, to reduce endless long lines.
    * Replaced many `getX` calls, probably originating from old Java code, into Groovy-idiomatic property accessor calls.
    * Lazy error handling sucks
      * Added `org.jetbrains.annotations.NotNull` annotations. But I could not get the runtime checks to work, likely because of too old Groovy version.
      * Added `Precondition` checks to still get some runtime checks (at the cost of additional code, though).
    * Some problems with the start-up scripts were fixed.

* 3.7.3
  * **Patch**: Pure maintenance release
  * **Patch**: Fix reported release number
  * **Patch**: Fix deprecation warnings in `build.gradle`
  * **Patch**: Allow release with optional subpatch-level suffixes matching the pattern `-test\d+`. This is obviously for testing only.
  * **Patch**: Updated PR-template with separated PR and release sections

* 3.7.2
  * **Patch**: Bumped BatchEuphoria version to 0.1.1: A newer LSF release is pickier on the position of the `-o` parameter in `bjobs`. Adapted to this upstream change. Probably, code will still work with older LSF versions.

* 3.7.1
  * **Patch**: Fixed printidlessruntimeconfig target

* 3.7.0
  * **Minor**: Updated to BatchEuphoria 0.1.0, which includes SLURM support. This means, Roddy now supports SLURM.
  * **Minor**: Bumped some dependencies, many of them because of security vulnerabilities. com.thoughtworks.xstream:xstream:1.4.19 has vulnerabilities, but is not used anymore and therefore removed from the dependencies. The SSH libraries and old BouncyCastle encryption libraries have some security issues, but an update is beyond the scope of this release. The best long-term solution would probably be to switch to Apache Mina. 

* 3.6.1
  * **Patch**: Users still seem to use the misleading and long-deprecated `runtimeConfig.sh` and `runtimeConfig.xml` files from the execution store. These files are not written anymore. 
    > You should always use the job-specific `.parameter` files!
  * **Patch**: Bugfix: Failure to pass the job resuming step (e.g. via `bresume`), with jobs that cannot be resumed (e.g. submitted via the DirectExecutionJobManager, like in the Bam2FastqPlugin).
  * **Patch**: The command in `roddyCall.sh` is properly escaped and suited for direct copy-paste to the command-line
  * **Patch**: Bugfix: Did not handle subsequent variable references in cvalue validation correctly.
  * **Patch**: Better error reporting for submission errors
  * **Patch**: Bumped BatchEuphoria to 0.0.13
  * **Patch**: Bumped RoddyToolLib to 2.3.0
  * **Patch**: Deprecated all `Job` constructors except for the main constructor, which is now used throughout the Roddy core code.  

* 3.6.0
  * **Minor**: Added `accountingName` to allow project accounting in cluster (for BE 0.0.12) 
  * **Patch**: Bumped to use BatchEuphoria 0.0.12

* 3.5.11
    * **Patch**: Pure maintenance release
    * **Patch**: Fix reported release number
    * **Patch**: Allow release with optional subpatch-level suffixes matching the pattern `-test\d+`. This is obviously for testing only.
    * **Patch**: Updated PR-template with separated PR and release sections

* ReleaseBranch_3.5.10
  * **Patch**: Added `listConfigurations`, `allBoms`, and a `...Bom` task for every Gradle configuration set. The `allBoms` and `...Bom` tasks generate JSON CycloneDX SBOMs in `gradleBuild/reports/cyclonedx`.

* 3.5.10
  * **Patch**: Bumped BatchEuphoria version to 0.0.7-1: A newer LSF release is pickier on the position of the `-o` parameter in `bjobs`. Adapted to this upstream change. Probably code will still work with older LSF versions.

* 3.5.9
  * **Patch**: The LocalExecutionService ignored errors of asynchronously executed commands. Now, errors Roddy detects errors and reports their standard and error output.
  * **Patch**: LocalExecutionService always executes commands via `bash -c` (before it did so only if the process was synchronously executed)
  * **Patch**: Update of RoddyToolLib to fix error handling in asynchronous execution and with multi-threading and command-output processing (StringBuilder->StringBuffer)

* 3.5.8
  * **Patch**: Bugfix: Project XML validation didn't exit != 0 in strict mode
  * **Patch**: Refactorings and more tests
  * **Patch**: Reduced verbosity of some file access error messages
  * **Patch**: CI now uses Java 8.0.252-open (sdkman) and Groovy 2.4.19 for building

* 3.5.7
  * **Patch**: Refactorings and few more tests

* 3.5.6
  * **Patch**: Bugfix: Delete submitted but suspended jobs upon exception in plugin
  * **Patch**: Updated Travis CI config to use Java and Groovy version via SDKMAN! 

* 3.5.5
  * **Patch**: Improved error messages for nested variable errors (more information to diagnose the cause)
  * **Patch**: Bugfix: nested variable error with job manager without queues (e.g. DirectSynchronousExecutionJobManager)

* 3.5.4
  * **Patch**: Bugfix: real and repeatable job calls files did not contain command but object reference

* 3.5.3
  * **Patch**: Bugfix: Quote selectionTag in string to visualize also empty selection tags.
  * **Patch**: Bugfix: Fail on auto-filename check
  * **Patch**: Upgrade from BatchEuphoria 0.0.6 to 0.0.7
    * Fixes date parsing issues in LSF
    * Reduced memory consumption with LSF, if many jobs ran in the monitored time interval

* 3.5.2
  * **Patch**: Bugfix: Fixed multi-threading bug for parallel execution of jobs in older SNV workflow plugins. This bug may have caused missed processing of chromosomes.
  * **Patch**: Bugfix: Use the job creation counter as additional information to make the auto filenames unique (development feature)
  * **Patch**: Bugfix: Exit on "derivedfrom" filename pattern despite matched pattern
   
* 3.5.1
  * **Patch**: Fixed variable reference for RODDY_ variables responsible for a sporadic bug due to incorrectly sorted parameter file

* 3.5.0
  * **Patch**: Fixed non-quoting of variables with spaces. Now, they are quoted and thus correctly interpreted as string variables instead of Bash array variables that are not correctly exported due to the Bash bug.
  * **Patch**: Added escaping of not already quoted variables containing double quotes as quoting these could yield wrong results (e.g. in the context of named Bash bug).
  * **Minor**: Allow for "selectionTag" in filename patterns and output file tags
  * **Patch**: Correctly processes selectionTag for onScriptParameter filename pattern matching
  * **Minor**: Added feature toggle `StrictOnScriptParameterSelectionTag` with default `false`; with `false` default selection tag values always match; with `true`, which is planned to be the Roddy 4, "default" selection tag values in output file declarations only match "default" selection tags in filename patterns
  * **Minor**: Improved parsing of `--cvalues` command-line argument solving issues with colons ':' in configuration variables
  * **Minor**: Removed ModifiedVariablePassing feature toggle
  * **Patch**: Fixed a new regression with not quoting bash/map variables in the job-parameter files
  * **Patch**: Numerous minor improvements of (error) output and behaviour
  * **Patch**: Updated BatchEuphoria dependency
  * **Minor**: Added `testRunVariableWidth` application property to change the width of the printing of job-parameter names. Defaults to old values 25.
  * **Patch**: Update to Gradle 5.1.1
  * **Patch**: Update to RoddyToolLib 2.1.1 
  
* 3.4.2
  * **Patch**: Fix a bug which made CValue.toFile() output wrong paths
  
* 3.4.1
  * **Patch**: Add JSON configuration file loader and tests
  
* 3.4.0
  * **Minor**: Improved validation of configuration values.
  * **Minor**: ForbidSubmissionOnRunning was re-enabled and turned on by default.
  * **Patch**: Created ExitReasons class for usage within Roddy and plugins. The class stores different exit codes and messages.
  * **Patch**: Bugfixes for local job execution and cluster job submission
  * **Patch**: Updated to BatchEuphoria 0.0.6.
  * **Minor**: Follow symbolic links when listing files in directories, e.g. collecting dataset names.
  * **Patch**: Added a base class for Spock tests: RoddyTestSpec.
  * **Patch**: Updated copyright notices.
  * **Patch**: Improved test coverage.

* 3.3.4 (ReleaseBranch_3.3)
  * **Patch**: Fixed variable reference for RODDY_ variables responsible for a sporadic bug due to incorrectly sorted parameter file

* 3.3.3
  * **Patch**: Further improved error reporting
  * **Patch**: Increased default JVM memory size to 1G
  * **Patch**: Improved robustness

* 3.3.2
  * **Patch**: Improved directory checks
  * **Patch**: Improved error reporting

* 3.3.1
  * **Patch**: Updated to RoddyToolLib 2.0.0
  * **Patch**: Updated to BatchEuphoria 0.0.5
  * **Patch**: Fixed autofilename bug

* 3.3.0 (misreports itself as 3.2.3)
  * **Patch**: Bugfixes to get Sophia 2.0.0 running
  * **Patch**: Added more (longer) tuple classes for jobs with more inputs/outputs
  * **Patch**: Mis-tagged this release as 3.2.3!

* 3.2.2
  * **Patch**: Fixed context warnings that were mis-represented as errors
  * **Patch**: Bugfixes to get BamToFastqPlugin running
  * **Patch**: Fixed FailOnAutoFilenames feature toggle usage
  * **Patch**: Improved error reporting (e.g. report .roddy/ logfile, configuration errors)
  * **Patch**: Fixed bug in remote file-existence checks
  * **Patch**: Sphinxs documentation with plantuml plugin support

* 3.2.1
  * **Patch**: FileOnErroneousDryRuns feature toggle
  * **Patch**: Extended plugin API
  * **Patch**: Fixed rare MD5 file check bug during tool compression: locally modified plugins could result in always uploading known plugin versions
  * **Patch**: Improved error reporting

* 3.2.0
  * **Patch**: Improved loading of cohorts and supercohorts
  * **Patch**: Improved cyclic dependency detection in configuration variables
  * **Patch**: testrerun shows all configuration again
  * **Patch**: Stricter conversion of Sphinx documentation
  * **Minor**: Allow FileGroup in tool tuple output
  * **Minor**: Allow selectiontag in FileGroup

* 3.1.0
  * **Minor**: Improved cohort handling
  * **Minor**: Supercohorts
  * **Patch**: RoddyToolLib 1.0.0
  * **Patch**: ContextResource for simpler test writing
  * **Patch**: No JDK version check for plugins anymore
  * **Patch**: Improved unknown libdir problem during roddy.sh compile

* 3.0.11
  * **Patch**: Fix printidlessruntimeconfig
  * **Patch**: Backported ContextResource from 3.1.0 version bump
  * **Patch**: IO-dir checks also in other modes but run

* 3.0.10
  * **Patch**: Backported tool-compression bug related to MD5 sum creation from 3.2.1 

* 3.0.9
  * **Patch**: Update to RoddyToolLib 2.0.0

* 3.0.8
  * **Patch**: Boolean configuration value `1` was incorrectly interpreted as `false`. Fixed to be interpreted as `true`. Added warnings that on the long run only "true" and "false" will be allowed values for Booleans.
  * **Patch**: Grid Engine Support restored
  * **Patch**: Bugfixes
  * **Patch**: Start-up script reads `INCREASE_BUILD_VERSION` (true, false) variable defaulting to true, to allow turning off automatic build-number increase.
  * **Patch**: Basic implementation of a new Brawl DSL based on Groovy. 
  * **Patch**: Removed `preventJobExecution` configuration variable

* 3.0.1-3.0.7
  * **Patch**: Bugfix releases
  * **Patch**: TheRoddyWMS organization

* 3.0.4 

* 3.0.0
  * Many bugfixes and error message improvements
  * Split-off BatchEuphoria and RoddyToolLib from RoddyCore. BatchEuphoria has LSF support
  * Sphinx-based documentation (for ReadTheDocs.org) 
  * building with Gradle
  * continuous integration with Travis and deployment to Github Releases
  * improved structure and content of output (show plugin directories and loaded XMLs)

* 2.3.187
  * Many bugfixes
  * Improved error messages

* 2.3.133

This section contains features which are currently in development. Testable / active features
are in the changelist.
Entries here can be marked with (PLANNED) or (WIP). (TEST) is more for the Changelist.

* (PLANNED) check defined binaries for existence. Isn't this already done in the validation part?

* (PLANNED) enable and disable runFlags with --run=... and --dontrun=...

* (WIP) Roddy accepts a lot more parameters which can otherwise be configured with the
  application properties file:
    * usePluginVersion,
    * commandFactoryClass,
    * executionServiceClass, executionServiceAuth, executionServiceHost, executionServiceUser
    
    Those parameters will all override the settings in the application properties file

* (WIP) Integrate SLURM as a cluster backend

* 2.3.97b

Beta release.
* Working version of new runtime class location implementation.

* Add new printruntimeconfig modes. There are be the two additional modes
    --showsourcenetries : shows values with their file.
    --extendedlist      : as an addition to showsourceentries, shows the
                          full list of values including their predecessors.
* API version checks (Java, Groovy, Roddy, Plugins)
* Unified portable IntelliJ Idea configs
* Improved logging in ProjectFactory
* Metadata table support
* Colored output

* 2.2.112

* Version update

* Change the behaviour of file access rights setting:
   
    * Only output one error message instead of many
    * Test on startup if access right setting is possible and disable the feature by setting outputAllowAccessRightsModification to false
    * Roddy will check, if the project output folder exists. If it does not exist, it will refuse to run a dataset. This way, the user is forced to create this directory explicitely.

  * Setting the location of .roddyExecutionDirectory is possible. Set RODDY_EXECUTION_DIRECTORY to a (valid) path of your choice in your configuration files. The normal behaviour stays, which is to setup this directory in the project output folder. 
  
  * More checkups will occur, when Roddy starts up. E.g. Roddy will check the accessibility of plugin and configuration directories.

  * We introduce a new branching scheme from now on. There will be the master branch and an additional development branch. The master branch should only contain stable and tested releases of Roddy. development will be there for everything else.

  * (WIP) Roddy will now support plugin revisions and compatibility tracks. This is useful, if you have a plugin which depends on other plugins and which you do not want to recompile/ repack, just because another plugin was extended.
  
    * Plugins can be extended in two ways
        * horicontal for e.g. hotfixes / revisions and
        * vertical, for extensions.
  
    * Vertical Plugin compatibility is done for hotfixes via the directory name:
        * Workflow-1.0.23 (original)
        * Workflow-1.0.23-r1 (revision 1)
    
    * Horizontal Plugin compatibility is done via the buildinfo.txt file:
        * Workflow-1.0.24/buildinfo.txt
            * extendsversion=1.0.23
      
  ![Dependency graph]("documentation/readme_images/Roddy Plan_0.jpg")

* 2.2.87

    * Fix: Fix some bugs regarding brawl workflows.

    * Fix: Fix a bug in testrerun where too much jobs were displayed. This should be fixed now.

    * Update prepareprojectconfig. create and update still exist but you only need to provide the
  target project folder. Roddy will take care of the rest. Both update and create will update
  the configurationDirestories part in the ini file to contain/change the newly created directory

* 2.2.81

    * Fix: Output directories will now always be set to type "path". This will prevent errors for
      output files, if users forget to set this particular variables to the proper type.
    
    * (TEST) Roddy can now create auto filenames, if there was no filename pattern for a file.
      This is currently enabled by default! Auto filenames are placed in the output directory and
      contain the job name, the parameter name, the job id and the suffix .auto.
    
    * (WIP) add the createworkflow runmode to Roddy.
    
    * (TEST) A working and version of Roddys built in Brawl language. Brawl is be a very basic
      language to implement simple workflows. Support is e.g. given for if and a lot of typing can be skipped.
      brawl scripts will be auto converted and compiled to Groovy workflows upon startup. This way it is guaranteed
      that those scripts are written properly. Still missing is the for loop syntax.

* 2.2.66

    * (TEST) Implement the "showreadme" command to show the Roddy README.md
    
    * (TEST) Implement the "printpluginreadme" command to show the readme of a workflow.
             Also put in the "printanalysisxml" to show the analysis xml.
    
    * (TEST) Allow selection of nodes via command line for PBS and SLURM. SGE is currently not supported.
      The configuration value to set is "enforceSubmissionToNodes:<nodeid>;<nodeid>" followed by a semicolon separated list of nodes.
    
    * (TEST) Change output of job ids to use println.
    
    * (TEST) Introduce a killswitch to disable filename imports upon analysis import.

* 2.2.49

    * Add the --extendedlist option to support a very extended view for checkworkflostatus.
      In this view, all previous runs are shown.
    
    * (TEST) Tool entries allow override of old entries without resetting everything.
      A ToolEntry must be defined like this: (with overrideresourcesets="true"!)
              <tool name='fastqc' value='checkFastQC.sh' basepath='qcPipeline' overrideresourcesets="true">
                  <resourcesets>
                      <rset size="l" nodes="1" walltime="1000"/>
                  </resourcesets>
              </tool>
    
    * Move the configuration conversion (bash, xml) to different classes, cleansing the ConfigurationFactory a bit.
    
    * Add the boolean configuration parameter outputAllowAccessRightsModification to allow (default) or disallow access rights modification
      on the target file system.
    
    * queryworkflowstatus now gives a much leaner overview about all datasets of a project.
      There was also a bugfix which stated that some processes were still running though they were obviously crashed.
    
    * The Roddy wrapper script for called scripts contains various new checks which will lead to ABORTED, if the conditions cannot be met.
    
    * The feature toggle file can be selected with --usefeaturetoggleconfig=<file>.
      Single feature toggles can be enabled or disabled with --enabletoggle=<toggle>,<toggle> or --disabletoggle
      The list of possibly available toggle can be queried with the "showfeaturetoggles" start mode
    
    * (TEST) Unzip of plugins is disabled by default now. Can be enabled via feature toggle. (Feature toggle id: UnzipZippedPlugins[def:false])
    
    * (TEST) Pass parameters in a parameter file instead of the normal cli parameter passing. (Feature toggle id: ModifiedVariablePassing[def:false])

* 2.2.8

    * (TEST) Break submission, if an error occurs. If a job cannot be started on a cluster environment, the overall
      job submission will be skipped. Running / Started processes are not affected. (Feature toggle id: BreakSubmissionOnError[def:false])
    
    * (TEST) New app ini loader, does not store files. This part was remove for now.
    
    * (TEST) XML validation will take place when XML files are loaded. (Feature toggle id: XMLValidation[def:true])
    
    * (TEST) Roddy binaries, scripts and libraries will be stored in a different directory.
      Insided dist, there will be several bin/{version} folders like 2.1.49. Inside those, the binary and necessary
      dependencies are store like i.e.:
            dist/bin/2.1.49/Roddy.jar
                           /helperScripts
                           /lib
      Regarding Runtime libraries handling like groovy and the JDK there is no good solution yet.

* 2.1.49

    * Roddy will perform a lot more checks before starting a workflow. These checks include file system
      checks, file availability and some more. 
    
    * Roddy will delete an execution directory if no jobs were submitted. This is not working
      with the SSH library SFTP client for unknown reasons.
    
    * The way how libraries / plugins are loaded is changed. The used plugins / version can
      be specified (descending priority):
        * A project configuration analysis import:
          <analysis id='snvCalling' configuration='snvCallingAnalysis' useplugin="COWorkflows:1.0.114,..."/>
        * On the command line with --usePluginVersion=...
        * The application ini file: usePluginVersion=COWorkflows:1.0.114,...
        Roddy will try to load plugins which are linked to the set plugins. If a plugin is missing, this
        might lead to problems.
      
        If the used plugins are not set, Roddy will try to load all plugins in
        their latest version.

* 2.1.28

* 2.1.27

    * Bugfix: There was an error in some rare cases with the job wrapper script. In those rare
  cases, the code to query the last jobstates failed and the script was not started. This only
  occurred when using SGE. 

* 2.1.26

    * Roddy tries to create all files with the proper group rights and settings. This
  is mostly tested for PBS based execution. The change of settings includes files in
  .roddyExecutionDirectory as well as those in the roddyExecutionStore
  File settings are set recursively where this is necessary.

* 2.1.24

    * (TEST) You can use Roddy to create new workflows. Use the createworkflow option for that:
      bash roddy.sh createworkflow (pluginID) [native:](workflowID)
    
    * (TEST)The application properties ini readout is changed in such a way, that the users user
  name will be used if USERNAME or "" is supplied. This will also be the default parameter.

* 2.1.14

    * (TEST) There is now a basic support for native workflows. Though the workflow scripts still
      need to be adapted somehow, the new mechanism could make it very easy to integrate
      non java workflows.
      Create an analysis configuration with the following header content:
        listOfUsedTools="nativeWorkflow"
        nativeWorkflowTool="nativeWorkflow"
        targetCommandFactory="de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommandFactory"
      Do not forget to put in the workflow script!
      This is basically working, however parameters might not be passed yet and the tool is only taken
      from the job name. This might confuse people!
    
    * Roddy now supports the parameter --useRoddyVersion= from the command line, it overrides
  the settings in the application properties ini file

* 2.1.8
