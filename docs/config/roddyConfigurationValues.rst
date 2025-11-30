Roddy Configuration Values
==========================

The following variables are used by Roddy itself:

Path Configuration
------------------

The following variables describe paths and may contain metadata-variable references such as '${dataset}' that are filled in during the identification
of input data during start up or filled with metadata values during submission.

* `inputBaseDirectory`:  This option is also set by the `--useiodir=$outputBaseDirectory,$inputBaseDirectory` command-line option.
* `outputBaseDirectory`: This option is also set by the `--useiodir=$outputBaseDirectory[,$inputBaseDirectory]` command-line option. Usually, this directory contains
    * one subdirectory per dataset (e.g. patient).
    * the "execution cache file" (`.roddyExecCache.txt`) containing the list of executed runs
    * the global "execution store" (`.roddyExecutionStore`) with the decompressed analysis tools from the plugins
* `outputDirectory`: Usually set to `$outputBaseDirectory/${dataset}`.
* `outputAnalysisBaseDirectory`: This is usually a subdirectory of the `outputDirectory` and contains the actual output of the workflow. Usually, the filename patterns in the plugins use the `outputAnalysisBaseDirectory` as base directory for the full paths to output files. This directory also contains the "roddyExecutionStore" with the logs of the workflow execution.



Debugging Options
-----------------

* `debugWrapInScript`: Turn on extended debugging of the wrap in script that sets up the stage for the actual top-level job/tool script.
* `disableDebugOptionsForToolscript`: Mainly for internal usage. Turn off all debugging for the top-level job/tool script.

The following options set specific debugging options for Bash

* `debugWrapInScript`: Toggle debugging of the wrapper script (DefaultPlugin).
* `debugOptionsUsePipefail`: `set -o pipefail`
* `debugOptionsUseVerboseOutput`: `set -v`
* `debugOptionsUseExecuteOutput`: `set -x`
* `debugOptionsUseUndefinedVariableBreak`: `set -u`
* `debugOptionsUseExitOnError`: `set -e`
* `debugOptionsParseScripts`: `set -n`
* `debugOptionsUseExtendedExecuteOutput`: This turns on additional debugging in Bash by setting the `PS4` variable. Each line starts with information about the executed script, the line-number in the script, and the function calls. `export PS4='+(\${BASH_SOURCE}:\${LINENO}): \${FUNCNAME[0]: +\$ { FUNCNAME[0] }():}'"`

Exposed to Jobs
---------------

The following variables are exposed to plugins by default and are set to by Roddy to the specific values applicable to the job.

* `RODDY_SCRATCH`: Affected by `baseScratchDirectory` applicationProperties.ini option.
* `RODDY_JOBID`: The job-identifier (`PBS_JOBID`, `LSB_JOBID`, etc.)
* `RODDY_JOBNAME`: The job-name (`PBS_JOBNAME`, etc.)
* `RODDY_QUEUE`: The queue to which the job runs (`PBS_QUEUE`, etc.)

Additionally, all configuration variables are reached through from the command-line (`--cvalues`) and configuration files into the jobs. Dependent on the plugin code additional variables may be set (or overridden) specifically for each job.

Access Rights
-------------

Note that we suggest you manage the access rights for output files not via Roddy, but by setting proper access rights on the directory path to Roddy's output files.

* `processOptionsSetUserGroup`
* `processOptionsSetUserMask`
* `processOptionsQueryEnv`
* `processOptionsQueryID`
* `outputAllowAccessRightsModification`: Whether or not access rights are are managed by Roddy. Some environments may not allow this and produce errors for every access rights modification attempt.
* `outputFileGroup`: Group ownership of output files.
* `outputAccessRights`: Exact access rights, output files should have.
* `outputAccessRightsForDirectories`: Exact access rights, output directories should have.
* `outputUMask`: Umask for output files

Other Variables
---------------

* `usedResourcesSize`: Five resource sets can be defined and with this variable. Use the following abbreviations: `t`(iny/est), `s`(mall), `m`(edium), `l`(arge) `xl` (extra large). The resource sets are defined in the plugin XML, but can also be overridden by XMLs provided via `additionalImports` CLI parameter.
* `accountingName`: A name used to associate every job with a accounting information in the cluster system, if this is possible.
* `maxFileObjectAppearanceRetries` and `fileObjectAppearanceRetryWaitMs` configure the waiting time and retries for delayed file object appearance configurable. This is an internal parameter related to concurrent update of objects and has nothing to do with waiting for files to appear in the file system. Increasing these values will make Roddy less chatty ("Taking a short nap") and more stable on submission hosts under heavy load, but will also increase the time needed for submission. Note that these parameters are used for waiting for all file objects required per job. Therefore, the impact on the submission runtime will be higher, for workflows with many jobs.
