Application properties files
============================

To successfully manage a workflow, Roddy needs to know about several things:

- The Batch system you're running on.

- The user credentials for e.g. SSH and connection settings.

- The directories for configuration files and plugins.

- And, if you want, some debug settings.

When setting paths and referring to e.g. environment variables like "${USER}" braces "${...}" to avoid warning about variables without braces that Roddy generates (since version 3.5) to warn you about possibly unresolved variables.

Let's have a brief look at it:

.. code-block:: Bash

    [COMMON]
    useRoddyVersion=develop                     # Use the most development version for tests
    passEnvironment=false
    baseEnvironmentScript=[ENVIRONMENT_FILE]
    logFilesPrefix=default                      # Prefix of extended run logs
    maximumLogFilesPerPrefix=32                 #

    [DIRECTORIES]
    configurationDirectories=[FOLDER_WITH_CONFIGURATION_FILES]
    pluginDirectories=[FOLDER_WITH_PLUGINS]
    scratchBaseDirectory=[FOLDER_ON_EXECUTION_HOSTS]

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
    #executionServiceKeyfileLocation=[keyfile path]   # use $HOME/.ssh/id_rsa by default
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


By default the environment local to the submission host, on which the job
submission commands like qsub or bsub are executed -- i.e. not necessarily the
system on which Roddy is executed (!), is not passed to the execution hosts,
to ensure a defined environment for maximum reproducibility. If you want to pass
the local environment, you can set `passEnvironment` to `true`. The
`baseEnvironmentScript` variable can be used to ensure that e.g. /etc/profile is
sourced, because this may not be per se the case, depending on whether your
scheduling system uses interactive or login shells to execute job. Alternatively,
you may source such base environments in your ~/.profile or ~/.bashrc.

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

