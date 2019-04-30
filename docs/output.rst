Output
======

There is not much to say about the standard output and error of Roddy, except that the information on which jobs are submitted is printed to standard output, while all other information is printed to standard error. This simplifies the parsing of the submission results.

More interesting is the execution metadata. Note that the ``$outputAnalysisBaseDirectory`` is configurable and by default the same as ``$outputBaseDirectory/$dataSet`` (defined in the DefaultPlugin configuration). For many workflows it is in a subdirectory of ``$outputBaseDirectory``.

``$outputAnalysisBaseDirectory/roddyExecutionStore``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Roddy execution store contains the most important metadata required for debugging and reproduction. The directory contains one ``exec_*`` subdirectory for each Roddy run for the data output directory in which the execution store is located.

Specifically the following files are contained:

``runtimeConfig.sh``
    In Roddy 2 this file was the way how job configuration data was provided to the jobs. The file contains Bash code to set up the environment from which the top-level job script is called (this is done in the wrapInScript.sh contained in the DefaultPlugin). Since version 3 this file is not used anymore, but kept (for now) as reference. Be aware that job-specific parameters may be set to wrong values or not at all in this file. Roddy 3 uses the .parameter files.
``*.parameter``
    Bash script to set up the environment for the top-level job script (since Roddy 3). This file contains all configuration values as used for the job, unless they have been changed in the environment setup script or in the job itself.
``versionInfo.txt``
    List of plugins and versions used.
``jobStateLogFile.txt``
    This file is extended by the ``wrapInScript.sh`` provided by the DefaultPlugin. Each row consists of four colon-separated columns with (1) the cluster job ID as used by the batch processing system, (2) a status indicator -- usually ``STARTED`` or the job's exit code, (3) a timestamp in seconds since epoch, (4) and the name of the cluster job. You can convert the timestamp with the date command: ``date -date="@$secondsSinceEpoch"``
``.o%J`` files
    Combined standard output and error of the job as run on the cluster.

``$outputBaseDirectory/.roddyExecCache.txt``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A small CSV file containing information about execution stored, workflow analysis ID, and user names that ran the analysis.

``$outputBaseDirectory/.roddyExecutionStore/``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This hidden directory contains the scripts from the plugins as transferred from the host on which you executed Roddy. For different versions of your plugins different subdirectories are created, each with a version and timestamp tag. If you develop a workflow and change scripts multiple such directories for the same plugin version may coexist, but each with a different timestamp. The ``roddyExecutionStore/exec_*/analysisTools`` directory contains symlinks to these unpacked plugin tools directories that were used for the specific job execution.

$HOME/.roddy/logs
~~~~~~~~~~~~~~~~~

More extensive log files of Roddy containing information about the execution of the Roddy core workflow management system, including possible exception stacktraces, standard output and error, etc. After each run the corresponding log file name is reported on the standard error at the end of the execution. Note, however, that because these files can get quite big, only 30 of them are kept in this directory!

