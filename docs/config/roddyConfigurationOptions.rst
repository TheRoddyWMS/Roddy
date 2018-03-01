Roddy Configuration Options
===========================

The following variables are used by Roddy itself:

Path Configuration
------------------

The following variables describe paths and may contain metadata-variable references such as '${dataset}' that are filled in during the identification
of input data during start up or filled with metadata values during submission.

* outputBaseDirectory: This option is also set by the --useiodir=$outputBaseDirectory[,$inputBaseDirectory] command-line option
* inputBaseDirectory:  This option is also set by the --useiodir=$outputBaseDirectory,$inputBaseDirectory command-line option

Debugging Options
-----------------

* disableDebugOptionsForToolscript
* debugWrapInScript: Turn on extended debugging of the wrap in script that calls the top-level job scripts.
* debugOptionsUsePipefail: Put `set -o pipefail`
* debugOptionsUseVerboseOutput: Put `set -v`
* debugOptionsUseExecuteOutput:
* debugOptionsUseExtendedExecuteOutput:
* debugOptionsUseUndefinedVariableBreak:
* debugOptionsUseExitOnError: Put `set -e`
* debugOptionsParseScripts:

Exposed to Jobs
---------------

The following variables are exposed to plugins by default. Specific plugins can add additional variables to configure the executed scripts.

* RODDY_SCRATCH: Affected by `baseScratchDirectory` applicationProperties.ini option.
* RODDY_JOBID: The job-identifier (PBS_JOBID, LSB_JOBID, etc.)
* RODDY_JOBNAME: The job-name (PBS_JOBNAME, etc.)
* RODDY_QUEUE: The queue to which the job runs (PBS_QUEUE, etc.)

Access Rights
-------------

* processOptionsSetUserGroup
* processOptionsSetUserMask
* processOptionsQueryEnv
* processOptionsQueryID
* outputAccessRightsForDirectories
* outputAllowAccessRightsModification";
* outputAccessRights
* outputFileGroup
* outputUMask

Other Variables
---------------

* preventJobExecution
* usedResourcesSize
