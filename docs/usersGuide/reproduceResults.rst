Reproduce Roddy Results
=======================

Reproducibility in bioinformatics is not an easy task. Even keeping everything identical except the CPU may produce slightly different results, so exact reproducibility is unlikely to be achievable at all. Also exact reproducibility is kind of an exaggerated aim when experimental data is concerned, which is always associated with a measurement error.

Generally, to reproduce bioinformatic results you need the following components

  * Configuration values (maybe including random seeds)
  * Software versions (but you use Conda, or not ;-) ), including workflow versions
  * Reference data
  * Input data to be analysed

A workflow management system like the Roddy core itself usually plays only a minor role, except if implementation details (in particular bugs) affect any of the aspects above. You should make sure that you have a good description of all these parameters and ideally, you should have a a backup copy of this.

Exact Reproduction
------------------

The simplest way to (almost) exactly reproduce a Roddy analysis is of course to use the same Roddy call in the same environment. Roddy stores the call used for each analysis in the ``roddyCall.sh`` file in the ``roddyExecutionStore/exec_*`` directory. Note that the file may not contain correctly quoted/escaped commandline parameters, so the call may not exactly executed as given in the file. The reason is that the ``--cvalues`` parameter may contain shell special characters like ';' or '!' that need to be escaped or quoted in the shell when to be interpreted as normal characters.

Here an example:

.. code-block:: bash

    /path/to/roddy.sh rerun \
        config.WGS@alignment \
        pid1,pid2 \
        --useconfig=/path/to/configs/applicationProperties-analysis-local-lsf.ini \
        --usefeaturetoggleconfig=/path/to/configs/featureToggles.ini \
        --usePluginVersion=AlignmentAndQCWorkflows:1.2.73-0 \
        --configurationDirectories=/path/to/configs \
        --useiodir=/path/to/pidDir/,/path/to/outputDir \
        --cvalues=fastq_files:/path/to/fastqs/r1.fq.gz;/path/to/fastqs/r2.fq.gz \
        --useRoddyVersion=3.3.3

The content of the file is a one-liner, but here it is broken down into arguments for readability. You'll notice the ``--cvalues`` parameter and that it contains a ';' used in the fastq_files configuration value to delimit read 1 and 2 FASTQs. The content of the configuration values can be arbitrary and is completely up to the workflow developer. In this case, the semicolons is als statement terminator in Bash and will be interpreted as such, unless quoted or escaped. Thus, the fix here is of course to quote the value of the parameter:

.. code-block:: bash

    --cvalues='fastq_files:/path/to/fastqs/r1.fq.gz;/path/to/fastqs/r2.fq.gz' \

Same Analysis on Different Input Data
-------------------------------------

If you want to extend your analyses with new input data you can start with an old Roddy call and adapt it. Often used adaptations are

  * change the output directory
  * change the input directory
  * change the applicationProperties.ini: if you intend to change the cluster configuration or configuration file directories
  * change configuration values that are metadata of the workflow specifically derived from the analysed sample (e.g. insert size distribution parameters for the sample)

Note that these changes can be quite tedious and error prone to be done manually. Usually it is best to write a small script -- in particular if you have to run your analyses on many new input datasets.


Beware: Multiple Execution Stores
---------------------------------

Beware that you may have multiple execution store directories if Roddy was run with in "rerun" mode, e.g. to complete a failed job. Here things can get really complicated if the different parts of the output data were produced with different versions or configurations.

  * Ideally you should call Roddy with explicitly mentioning the plugin version, like in the example above. Due to the plugin dependency and automatic loading you may want to check that all used plugins were identical. You can check the ``versionInfo.txt`` file in the execution stores to learn which versions were used during each execution.
  * For configuration files the situation is more complex. Currently, the logfiles are only permanently logged in the ``$HOME/.roddy/logs`` directory. But the most important resource for configuration values are the ``.parameter`` files for each job in the ``roddyExecutionStore`` directories.

If you are paranoid, you should always restart your analysis completely, if you intend to change any of the above mentioned factors like configurations, etc. If you know what you do, you may decide differently and change parameters even for an individual dataset, but then you should document what you do. It is really easy to mess up the configuration by changing a configuration value in a file. Better use versioned configuration files.

Of course similar problems arise if you have multiple samples processed by different workflow runs and you need a homogenously processed data set.

