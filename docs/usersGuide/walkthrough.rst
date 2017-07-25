Walkthrough
===========

This guide will show you how to setup Roddy, so that it is starting and
ready to run an analysis for a project. There is a sample NGS workflow
available, which will be used in the examples.

For a short overview about Roddy usage navigate to :doc:`cheatSheet`.

If you do not already have a running installation, please see :doc:`../installationGuide`
for instructions to install Roddy.

After installing Roddy, please head to the Roddy folder and run the
Roddy start script:

.. code-block:: bash

    bash roddy.sh

If everything is good, Roddy will start and print the help.

.. code-block:: bash

    Roddy is supposed to be a rapid development and management platform for cluster based workflows.
    The current supported ways of execution are:
     - job submission using qsub with PBS or SGE
     - monolithic, direct execution of jobs via Roddy
     - submission or execution on the local machine or via SSH

      To support you with your workflows, Roddy offers you several options:

      help
            Shows a list of available configuration files in all configured paths.

    [...]

        --usePluginVersion=(...,...)    - Supply a list of used plugins and versions.


    Roddy version 2.2.78 build at Fri Sep 18 13:55:26 CEST 2015

Now you can go on and prepare the configuration for your project.

Setup Roddy Configuration
-------------------------

You need two types of configurations:

1. Application configuration file (by default
   applicationProperties.ini): A ini formatted file that configures
   properties of the Roddy application, the batch processing system
   (PBS, SGE, etc.), default paths for configutations and plugins, etc.
2. An XML configuration file for your project with all parameters of the
   workflow that you want to use.

Application ini file
~~~~~~~~~~~~~~~~~~~~

Roddy uses an ini file to control the application behaviour. The ini
file define several things:

-  Which job system you use
-  How you connect to the processing system
-  Where Roddy shall search for plugins and configuration files

By default, Roddy will use the ini file located at
*$HOME/.roddy/applicationProperties.ini*, but you can select any other
file with the \_--useconfig\_\_ command-line option.

Please have a look at the following default application properties ini
file:

.. code-block:: ini

    [COMMON]
    useRoddyVersion=current                     # Use the most current version for tests

    [DIRECTORIES]
    configurationDirectories=[FOLDER_WITH_CONFIGURATION_FILES]
    pluginDirectories=[FOLDER_WITH_PLUGINS]

    [COMMANDS]
    jobManagerClass=de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutionJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.sge.SGEJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.slurm.SlurmJobManager
    #jobManagerClass=de.dkfz.roddy.execution.jobs.cluster.lsf.rest.LSFRestJobManager
    commandFactoryUpdateInterval=300
    commandLogTruncate=80                       # Truncate logged commands to this length. If <= 0, then no truncation.

    [COMMANDLINE]
    CLI.executionServiceUser=USERNAME
    CLI.executionServiceClass=de.dkfz.roddy.execution.io.LocalExecutionService
    #CLI.executionServiceClass=de.dkfz.roddy.execution.io.SSHExecutionService
    CLI.executionServiceHost=[YOURHOST]
    CLI.executionServiceAuth=keyfile
    #CLI.executionServiceAuth=password
    CLI.executionServicePasswd=
    CLI.executionServiceStorePassword=false
    CLI.executionServiceUseCompression=false
    CLI.fileSystemInfoProviderClass=de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider

The file is divided into several sections, but this is mainly to keep a
better order:

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

You might just remember or store away the above options for the future
as they most likely won't change too often. For you the more important
settings might be:

-  configurationDirectories - Put in a comma separated list of
   directories, where you keep your project xml files
-  pluginDirectories - Put in a comma separated list of the directories,
   where your plugins are stored. Note, that the folder dist/plugins in
   the Roddy base directory, which contains the PluginBase and
   DefaultPlugin, will always be imported. You do not need to set this
   one.

You can either copy the content from above or you can also use Roddy to
help you with the setup. This will be explained later on.

Project configuration files
~~~~~~~~~~~~~~~~~~~~~~~~~~~

All workflow-specific settings are stored in XML files.

The configuration files are multi-level, which means, you can - Import
configuration files into other configuration files - Define several
level of configurations and subconfigurations in one file

.. code-block:: xml

    <configuration configurationType='project'
             name='TestProject'
             description='A very small project configuration for some workflow tests.'
             imports="baseProject"
             usedresourcessize="m">
        <availableAnalyses>
            <analysis id='testWorkflow' configuration='TestAnalysis' useplugin="DefaultPlugin:current"/>
            <analysis id='qualityControl' configuration='QualityControlAnalysis' useplugin="QualityControlPlugin:1.0.10"/>
        </availableAnalyses>
        <configurationvalues>
            <cvalue name='inputBaseDirectory' value='$USERHOME/roddyTests/${projectName}/data' type='path'/>
            <cvalue name='outputBaseDirectory' value='$USERHOME/roddyTests/${projectName}/results' type='path'/>
        </configurationvalues>
        <subconfigurations>
            <configuration name="verysmall" usedresourcessize="xs" inheritAnalyses="true" />
        </subconfigurations>
    </configuration>

You as a user normally should only need to create a project specific
file like the one above. Roddy also offers a command for you to help you
to set this one up.

Configuration files contain several sections where Roddy lets you define
things like configuration values, tools and even filenames. But, you probably
won't need that now and we'll concentrate on a very basic project
configuration like the one above. You can find an in-detail guide here
:doc:`../config/xmlConfigurationFiles`. You might concentrate on the configuration
values part as this will be the part which you probably need most.

**//Uhhh, ok, so what is in the above example?//**

Good that you ask! First you'll find a standard xml format containing
the configuration header. If it is a project configuration file (you
could e.g. create a file which contains basic settings for your working
environment like e.g. commonly used binaries and reference files) then
your file must be named with the prefix "projects". Otherwise it will
not be recognized as a project configuration by Roddy.

.. code-block:: xml

    <configuration configurationType='project'
                         name='TestProject'
                         description='A very small project configuration for some workflow tests.'
                         imports="baseProject"
                         usedresourcessize="m">

The header of the configuration must contain the following: - The
configurationType (in this case "project") - A name which must not
contain "." and " "

It may contain:

-  A description
-  Imports for other configuration files. **import** can hold a comma
   separated list of other configuration id's / names
-  A switch for the size of the data you are dealing with. In the
   analysis configuration every tool can have different level of
   resources im memory, CPU, and walltime. This option in the project
   XML allows you to select a project-wide resource requirement level
   for the size of the input data expected in the project. The values t,
   xs, s, m, l, xl are allowed the and default is "l".

Directly after the header, you will find a list of the imported
workflows for your project.

.. code-block:: xml

        <availableAnalyses>
            <analysis id='testWorkflow' configuration='TestAnalysis' useplugin="DefaultPlugin:current"/>
            <analysis id='qualityControl' configuration='QualityControlAnalysis' useplugin="QualityControlPlugin:1.0.10"/>
        </availableAnalyses>

Each line can enable a workflow / analysis for your project. To make
such a line work, you need to set:

-  *id* an arbitrary name that identifies the workflow in your project.
   This name will be used to call the workflow from the command line.
-  *configuration* to identify the original analysis configuration id
   that is defined in the analysis XML in the plugin. You can also
   import an analysis several times with a different id value.
-  finally, useplugin is used to select the plugin and the plugins
   version, in which the analysis is searched. This parameter is
   optional.

The corresponding configuration files are automatically searched in your
plugins. The active plugins are retrieved from the plugin directories
set in you application ini file.

Next comes the part where you set the projects input and output folder.

.. code-block:: xml

        <configurationvalues>
            <cvalue name='inputBaseDirectory' value='$USERHOME/roddyTests/${projectName}/data' type='path'/>
            <cvalue name='outputBaseDirectory' value='$USERHOME/roddyTests/${projectName}/results' type='path'/>
        </configurationvalues>

In most cases, you should be done right now.

Analysis-specific configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Occasionally, you may want to set specific parameters only for selected
analyses. In this case you can add subconfigurations:

.. code-block:: xml

        <subconfigurations>
            <configuration name="verysmall" usedresourcessize="xs" inheritAnalyses="true" />
        </subconfigurations>

Subconfigurations are exactly defined like the main configuration. They
can contain the same sections. Each value, which is defined by you,
overrides a value of the parent configuration. Subconfigurations can be
nested and affect all ** tags that are nested within.

Built-in configuration creation / updates
-----------------------------------------

Use Roddy to create an initial project configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Roddy can help you to create an initial project configuration with one
command.

.. code-block:: bash

    bash roddy.sh prepareprojectconfig create [targetprojectfolder] --useRoddyVersion=current

The command will:

1. Create a target folder structure like
   [targetprojectfolder]/roddyProject/versions/version\_[current
   date]\_[current time]
2. Copy a default ini file to the target folder
   [targetprojectfolder]/applicationProperties.ini
3. Copy a default project xml to the target folder
   [targetprojectfolder]/project.xml

You can now update both the ini file and the xml file to your needs. Do
not forget to place the freshly create folder as a configuration folder
to the ini file! Please see the explanation above to decide which
settings are appropriate for your system.

To use the ini file, you can call Roddy in the following way:

.. code-block:: bash

    bash roddy.sh --useconfig=[targetprojectfolder]/applicationProperties.ini

Use Roddy to update an existing project configuration to a new version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sometimes it is helpful to keep several version for project
configuration files. This ensures, that you can always try to go back to
an old version of your config. To support this, you can call Roddy in
the following way:

.. code-block:: bash

    bash roddy.sh prepareprojectconfig update [targetprojectfolder]

Roddy will then search the latest existing project configuration version
and create a new folder with a copy in it.

So after you call Roddy, you'll find e.g.:

-  [targetprojectfolder]/roddyProject/versions/version\_20150719\_111328
   and
-  [targetprojectfolder]/roddyProject/versions/version\_20150925\_134527

The new folder will contain a copy of the contents of the old folder.
You can call Roddy afterwards with the new ini file.

    IMPORTANT: Roddy does not update the *configurationDirectories*
    option in the new *applicationProperties.ini*. As of now, you need
    to manually adapt the configuration directories in the ini file!

Check if things are set up properly
-----------------------------------

With configurations of complex workflows, it may become very tedious and
error prone to ensure that everything is configured correctly. If you
work with multiple projects, the first thing to check is the use of the
correct configuration files. To find out, if you did everything right,
Roddy offers you several options:

.. code-block:: bash

    bash roddy.sh showconfigpaths  --useconfig=[pathOfIniFile]

This will show you all available configuration files in your configured
paths. Note, that this won't list analysis XML files, as these are
loaded in a later stage, where Roddy has knowledge about loaded plugins.

With the following command you can check, whether you set the right
paths and if all your files are available:

.. code-block:: bash

    bash roddy.sh listdatasets [project]@[analysis] --useconfig=[pathOfIniFile]

.. NOTE:: Roddy supports parsing metadata such as dataset identifiers
    from paths but additionally has a MetadataTable facility that
    simplifies metadata input via a table. Some workflows may also be
    implemented to get the metadata from dedicated configuration values.
    Therefore, whether this command works may depend on the specific
    workflow and may require additional command-line parameters or
    configuration values. Still it can be extremely useful to get a list
    of all findable datasets.

If everything is properly set and you use the right configuration and
analysis, Roddy will be able to search the input and output folders in
your project configuration file. It will then display a list of all
found datasets. Roddy will search both folders and the result will be
combined, so you will not get doublettes. If you see the list of your
datasets, you can now run your analysis, but before you do this, you can
also try some more things before.

.. code-block:: bash

    bash roddy.sh printruntimeconfig [project]@[analysis] [pid] --useconfig=[pathOfIniFile]

If you run a workflow for the first time, it might make sense to check
the generated runtime configuration file before you start a process. The
above command will do that for the pid set by you. Is everything right?
Good, then you can go on and start a process. If not, you need to check
your configuration files.

Run a project
-------------

There is one more thing you can do before starting a process: You can
call Roddy with testrun:

.. code-block:: bash

    bash roddy.sh testrun [project]@[analysis] [pattern]/[ALL] --useconfig=[pathOfIniFile]

testrun will nearly do the same thing as run, except, that it does not
start cluster jobs. It will list all the jobs which will be executed.
Please take a close look at the output for all the jobs. testrun and all
the other run commands are all triggered with a dataset id pattern.
We'll explain that soon.

    Some explanation for the dataset patterns. Roddy selects and lists
    datasets like e.g. *ls*. This means, you can use all sorts of
    wildcards and patterns. Valid patterns are e.g. H063\ *, \*-A\*,
    ???3-* and so on. But! Keep in mind, that wildcards will may already
    be resolved by the shell (e.g. Bash is always good for surprises).
    testrun will help you find out, if the patterns you use are working.
    Also note, that a plain \* won't work at least for Bash. If you want
    to run all datasets, use the dataset selector [ALL].

Now let's look at an example for a job output:

.. code-block:: bash

        0x789C44FF73F: fastqc [ -l walltime=1000:00:00]
          pid                       : H006-1
          PID                       : H006-1
          CONFIG_FILE               : [ exDir]/runtimeConfig.sh
          ANALYSIS_DIR              : /home/heinold/temp/roddyLocalTest/testproject
          TOOLSDIR                  : [ exDir]/analysisTools/qcPipeline
          TOOL_ID                   : fastqc
          RAW_SEQ                   : [ inDir]/control/paired/run120918_SN7001149_0101_AC16PKACXX/sequence/1_B_GCCAAT_L002_R1_complete_filtered.fastq.gz
          FILENAME_FASTQC           : [outDir]/fastx_qc/control_run120918_SN7001149_0101_AC16PKACXX_1_B_GCCAAT_L002_R1_sequence_fastqc.zip
          RODDY_PARENT_JOBS         : parameterArray=()

This is the output for a job calling fastqc on a fastq file, to go easy,
we just named it fastqc. First, there is a fake job id, which is used in
test cases. If you call *run* instead of *testrun*, this will be
replaced by a job identifier produced by your processing backend (PBS,
SGE, etc.). The job id is followed by the resource settings specific to
your configured processing backend. Here it is the walltime setting for
a PBS system. The next lines are the parameters which will be passed to
the job. Some of the parameters are set for every job including pid/PID
("patient id", this is the "dataset"), CONFIG\_FILE or ANALYSIS\_DIR.
The abbreviations like [exDir] or [inDir] are explained in the header of
the testrun output. They are there to make things more readable. Other
parameters like e.g. FILENAME\_FASTQC are job specific. In this case,
there is a fastq file for the job input and a zip file containing the
job output. Filenames are based on rules which are normally included in
analysis configuration files.

Let's see, showconfigpaths worked, listdatasets worked, printanalysisxml
worked and also testrun. What's left? Right: run!

Let's start and run something.

.. code-block:: bash

    bash roddy.sh run [project]@[analysis] [pattern]/[ALL] --useconfig=[pathOfIniFile]

Instead of the output of testrun, Roddy will now try and run the jobs on
your processing backend. If all jobs fail, you might have the wrong
settings. If some fail, there might be problems with the backend. Roddy
will also try to tell you what sort of problems there are. But this
won't work in every case. We won't bother you with the full output now,
but something like the following will show up in case of success:

.. code-block:: bash

Finally, you started something. Now all you have to to is to wait until
your process finishes. Roddy will again offer you several commands to
help you keep track of your progress.

Process tracking, Debugging and Rerunning a process
---------------------------------------------------

Sometimes, it can be nice to know if a process is still running or if
there were faulty jobs and sometimes you just want to restart a process.
Roddy has what you need: checkworkflowstatus, testrerun and rerun.

.. code-block:: bash

    bash roddy.sh checkworkflowstatus [project]@[analysis] [pattern]/[ALL] --useconfig=[pathOfIniFile]

checkworkflowstatus will create a table listing your selection of
datasets and their states:

.. code-block:: bash

    [outDir]: /home/heinold/temp/roddyLocalTest/testproject/rpp
    Dataset       State     #    OK   ERR  User      Folder / Message
    A100          UNSTARTED 0    0    0    Not executed (or the Roddy log files were deleted).
    A200          UNSTARTED 0    0    0    Not executed (or the Roddy log files were deleted).
    stds          OK        3    3    0    testuser   /home/testuser/temp/roddyLocalTest/testproject...

The table has several columns:

-  Dataset is self explaining and shows you for which dataset the line
   is
-  State is the state for the last execution of a dataset
-  Is the number of started jobs for a process
   `===========================================`

-  OK is the number of good jobs
-  ERR is the number of faulty jobs
-  User is the user which started the last process
-  Folder / Message is the execution store folder for the process

You can e.g. use the output to grep for states, folders and other
things. If there are errornous jobs, you now have the info to look for
those jobs. The next section will show you, how to do this. For know,
we'll consider the jobs as failed for technical reasons and show you how
to restart them.

Roddys restart / rerun option tries to start only jobs which need to be
run. For this, it creates a list of all the output files which it knows
and compares these files with the existing files on disk. There are no
consistency checks done, so files with the size of zero are also taken
into account. If a job has failed, all of its descendants are
automatically marked as failed. This is also true, when a new job will
get startet. What the workflow then does is within the responsibility of
the workflows author. Similar to testrun / run, testrerun and rerun will
start to process data. However, only necessary jobs will be started.

Import list for different workflows:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please consider using only one analysis import per project xml file, if
you set configuration variables. Configuration values for different
workflows might have the same name, which could lead to
misconfigured workflows. If you do not want to create a new file, you
can still use subconfigurations for the different workflows.

.. code-block:: xml

    <!-- Roddy 2.2.x -->
    <analysis id='snvCalling' configuration='snvCallingAnalysis' useplugin="COWorkflows:1.0.132-4" />
    <analysis id='indelCalling' configuration='indelCallingAnalysis'  useplugin="COWorkflows:1.0.132-4" />
    <analysis id='copyNumberEstimation' configuration='copyNumberEstimationAnalysis' useplugin="CopyNumberEstimationWorkflow:1.0.189" />
    <analysis id='delly' configuration='dellyAnalysis' useplugin="DellyWorkflow:0.1.12"/>

    <!-- Roddy 2.3.x -->
    <analysis id='WES' configuration='exomeAnalysis' useplugin="AlignmentAndQCWorkflows:1.1.39" />
    <analysis id='WGS' configuration='qcAnalysis' useplugin="AlignmentAndQCWorkflows:1.1.39" />
    <analysis id='postMergeQC' configuration='postMergeQCAnalysis' useplugin="AlignmentAndQCWorkflows:1.1.39"/>
    <analysis id='postMergeExomeQC' configuration='postMergeExomeQCAnalysis' useplugin="AlignmentAndQCWorkflows:1.1.39"/>

    <!-- Unreleased or Beta -->
    <analysis id='rdw' configuration='snvRecurrenceDetectionAnalysis' useplugin="SNVRecurrenceDetectionWorkflow"/>
    <analysis id='WGBS' configuration='bisulfiteCoreAnalysis' useplugin="AlignmentAndQCWorkflows:1.1.39"/>

