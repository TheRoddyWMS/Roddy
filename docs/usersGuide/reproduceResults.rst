.. _`jq` : https://stedolan.github.io/jq/

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

  * Ideally, you should call Roddy explicitly mentioning the plugin version, like in the example above. Due to the plugin dependency and automatic loading you may want to check that all used plugins were identical. You can check the ``versionInfo.txt`` file in the execution stores to learn which versions were used during each execution.
  * For configuration files the situation is more complex. Currently, the logfiles are only permanently logged in the ``$HOME/.roddy/logs`` directory. But the most important resource for configuration values are the ``.parameter`` files for each job in the ``roddyExecutionStore`` directories.

If you are paranoid, you should always restart your analysis completely, if you intend to change any of the above mentioned factors like configurations, etc. If you know what you do, you may decide differently and change parameters even for an individual dataset, but then you should document what you do. It is really easy to mess up the configuration by changing a configuration value in a file. Better use versioned configuration files.

Of course similar problems arise if you have multiple samples processed by different workflow runs and you need a homogeneously processed data set.

To cope somewhat with this problem, a Python script is included in the Roddy distribution. It is located in the root directory of the Roddy installation, just besides the ``roddy.sh``.

The script relies on a Python file with code for summarizing the configuration in the ``.parameter`` files found in the ``roddyExecutionStore/exec_*`` directories. At the time of writing only the AlignmentAndQCWorkflows plugin had such a file called ``ConfigSummary.py`` (the file name does not matter though). A call of the ``group-configs.py`` may look similar to the following:

.. code-block:: bash

    python3 group-configs.py \
        /path/to/plugin/ConfigSummary.py \
        $(find /path/to/data -name "roddyExecutionStore")

You need at least Python 3.7 for this and the package "more_itertools". You can also run unit test
(with ``pytest -v group-configs.py``).

This will return some diagnostic/runtime information on standard error and a JSON formatted report on standard output. E.g. you may pipe the standard output through `jq`_ to get a colored and nicely formatted report.

The JSON output will look similar to this:

.. code-block:: json

    [
      {
        "contexts": [
          "/path/to/data/roddyExecutionStore/exec_220325_112945616_user_WGS"
        ],
        "parameters": {
          "workflow": {
            "id": "qcAnalysis"
          },
          "roddy": {
            "Roddy": "3.5.9",
            "AlignmentAndQCWorkflows": "develop",
            "COWorkflows": "1.2.76-0",
            "PluginBase": "1.2.1-0",
            "DefaultPlugin": "1.2.2-0"
          },
          "base": {
            "PERL_VERSION": "5.20.2",
            "PYTHON_VERSION": "2.7.9",
            "R_VERSION": "3.4.0",
            "SAMTOOLS_VERSION": "0.1.19"
          },
          "adapter_trimming": {},
          "fastqc": {},
          "duplication_marking": {
            "markDuplicatesVariant": "sambamba",
            "SAMBAMBA_MARKDUP_VERSION": "0.6.5",
            "SAMBAMBA_MARKDUP_OPTS": "\"-t 1 -l 0 --hash-table-size=2000000 --overflow-list-size=1000000 --io-buffer-size=64\""
          },
          "sorting": {
            "useBioBamBamSort": "false",
            "SAMPESORT_MEMSIZE": "2000000000",
            "SAMTOOLS_VERSION": "0.1.19"
          },
          "qc": {
            "INSERT_SIZE_LIMIT": "1000",
            "SAMBAMBA_FLAGSTATS_VERSION": "0.4.6"
          },
          "aceseq_qc": {
            "runACEseqQc": "false",
            "HTSLIB_VERSION": "0.2.5",
            "VCFTOOLS_VERSION": "0.1.10"
          },
          "fingerprinting": {
            "runFingerprinting": "false"
          },
          "alignment": {
            "BWA_VERSION": "0.7.15",
            "BWA_MEM_THREADS": "8",
            "BWA_MEM_OPTIONS": "\" -T 0 \"",
            "INDEX_PREFIX": "/path/to/assembly/assembly.fa"
          },
          "bwa_post_alt": {
            "runBwaPostAltJs": "true",
            "K8_BINARY": "/path/to/software/bwa.kit/k8",
            "ALT_FILE": "/path/to/assembly/assembly.fa.alt",
            "bwaPostAltJsPath": "/path/to/software/bwa.kit/bwa-postalt.js",
            "bwaPostAltJsMinPaRatio": "1.0",
            "bwaPostAltJsHla": "false"
          },
          "wes": {},
          "wgbs": {}
        }
      }
    ]


Content of the config summary module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``ConfigSummary.py`` must contain a Class ``ConfigSummary`` with a method ``summarize`` that has the following signature:

.. code-block:: Python

    def summarize(self,
                  plugin_versions: Mapping[str, str],
                  parameters: Mapping[str, str]) -> \
                      Mapping[str, Mapping[str, Optional[str]]]

The ``summarize`` method takes as input a dictionary of plugin-versions as read from a ``roddyExecutionStore`` and the parameters extracted from a single ``.parameters`` file. It then returns a dictionary structure representing the summarized configuration and will be inserted in the ``parameters`` field of the result JSON (see previous section). The plugin developer can decide to include or exclude configurations, e.g. to not include parameters that are irrelevant, if a feature is turned off.

The ``group-configs.py`` script takes these per-parameter-file summaries and checks that they are consistent within each ``exec_*`` directory (i.e. for a single execution of the workflow) to guard against including manually changed configurations.

After that the validated configurations are simply grouped by identity and a list of configuration variants is returned together with all ``exec_*`` directories in which they are applied.

Note that with this approach it is possible to recognize whether the workflow was run multiple times on the same directory (using the same ``roddyExecutionStore``), but it is not possible to tell which files were produced during which run. The script does no analysis of the log-files. Plugin versions are exclusively identified by the ``versionInfo.txt`` file, no MD5 sums or similar to unambiguously identify the plugin version. You should therefore only rely on this script, if you only have unmodified, persistently named plugin versions.