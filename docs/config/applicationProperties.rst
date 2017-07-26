Application properties files
============================

To successfully manage a workflow, Roddy needs to know about several things:

- The Batch system you're running on.

- The user credentials for e.g. SSH and connection settings.

- The directories for configuration files and plugins.

- And, if you want, some debug settings.

Let's have a brief look at it:

.. code-block:: Bash

    [COMMON]
    useRoddyVersion=current                     # Use the most current version for tests

    [DIRECTORIES]
    configurationDirectories=[FOLDER_WITH_CONFIGURATION_FILES]
    pluginDirectories=[FOLDER_WITH_PLUGINS]

    [JOB_PROCESSING]
    jobManagerClass=de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutionJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.sge.SGEJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.slurm.SlurmJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.lsf.rest.LSFRestJobManager
    commandFactoryUpdateInterval=300
    commandLogTruncate=80                       # Truncate logged commands to this length. If <= 0, then no truncation.

    [COMMANDLINE]
    executionServiceUser=USERNAME
    executionServiceClass=de.dkfz.roddy.execution.io.LocalExecutionService
    #executionServiceClass=de.dkfz.roddy.execution.io.SSHExecutionService
    executionServiceHost=[YOURHOST]
    executionServiceAuth=keyfile
    #executionServiceAuth=password
    executionServicePasswd=
    executionServiceStorePassword=false
    executionServiceUseCompression=false
    fileSystemInfoProviderClass=de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider

The file is divided into several sections, but this is mainly to keep a
better order, you can have the file setup like you want it. Briefly explained, the

-  **COMMON** is for setting up general things
-  **DIRECTORIES**
-  **COMMANDS**
-  **COMMANDLINE** is to set up the command line interface

We try to keep every possible option in the ini file, so you should
basically be able to just select what you need and to fill in the
missing parts.

Usually, you just need to change the following settings:

-  jobManagerClass - Selects the cluster system backend
-  CLI.executionServiceClass - Selects, if you want to access your
   system via SSH or directly
-  CLI.executionServiceAuth - keyfile or password?
-  CLI.executionServiceHost - The host, if you select SSH
-  CLI.executionServicePasswd - The password for your system, if using
   SSH and no keyfiles
-  CLI.executionServiceStorePassword - If you want to store the
   password, put in true, however, the password is stored in plain-text!

You might remember or store away the above options for future usage
as its likely, that they won't change too often. For you the more important
settings might be:

-  configurationDirectories - Put in a comma separated list of
   directories, where you keep your project XML files
-  pluginDirectories - Put in a comma separated list of the directories,
   where your plugins are stored. Note, that the folder dist/plugins in
   the Roddy base directory, which contains the PluginBase and
   DefaultPlugin, will always be imported. You do not need to set this
   one.

You can either copy the content from above or you can also use Roddy to
help you with the setup. This will be explained later on.

