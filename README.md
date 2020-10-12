[![Build Status - Travis](https://travis-ci.org/TheRoddyWMS/Roddy.svg?branch=master)](https://travis-ci.org/TheRoddyWMS/Roddy)
# What is Roddy? 

Roddy is a framework for development and management of workflows on a batch processing cluster. It has been developed at the German Cancer Research Center (DKFZ) in Heidelberg in the eilslabs group and is used by a number of in-house workflows such as the [PanCancer Alignment Workflow](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows) and the [ACEseq workflow](https://github.com/eilslabs/ACEseqWorkflow). The development is now continued in the Omics IT and Data Management Core Facility (ODCF) at the DKFZ.

> <table><tr><td><a href="https://www.denbi.de/"><img src="docs/images/denbi.png" alt="de.NBI logo" width="300" align="left"></a></td><td><strong>Your opinion matters!</strong> The development of Roddy is supported by the <a href="https://www.denbi.de/">German Network for Bioinformatic Infrastructure (de.NBI)</a>. By completing <a href="https://www.surveymonkey.de/r/denbi-service?sc=hd-hub&tool=roddy">this very short survey</a> you support our efforts to improve this tool.</td></tr></table>

> No new features will be implemented for Roddy! We will continue to fix bugs occurring with currently existing workflows. On the long run, existing workflows should be migrated to other workflow management systems. 

# Documentation

You can find the documentation at [Read the Docs](http://roddy-documentation.readthedocs.io), including [detailed installation instructions](http://roddy-documentation.readthedocs.io/installationGuide.html).

# Workflows

The following workflows have been developed at the DKFZ based on Roddy as workflow management system:

  * [Alignment and QC workflows](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows)
  * [SNV-Calling workflow](https://github.com/DKFZ-ODCF/SNVCallingWorkflow)
  * [ACEseq workflow](https://github.com/DKFZ-ODCF/ACEseqWorkflow) for copy-number variation calling
  * [InDel-Calling workflow](https://github.com/DKFZ-ODCF/IndelCallingWorkflow) workflow
  * [Sophia workflow](https://github.com/DKFZ-ODCF/SophiaWorkflow) for structural variation calling
  * RNA-seq workflow (to be published)
  * CNVkit for copy-number variation calling on exome data (to be published)
  * Leaf-Cutter workflow
  * [Bam-to-FASTQ](https://github.com/TheRoddyWMS/BamToFastqPlugin) plugin
  
The following plugins are available as support for the workflows:

  * [COWorkflowBasePlugin](https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin) with basic control code for many of the workflows
  * [PluginBase](https://github.com/TheRoddyWMS/Roddy-Base-Plugin) just the plugin base-class from which other plugins are derived
  * [DefaultPlugin](https://github.com/TheRoddyWMS/Roddy-Default-Plugin) with the `wrapInScript.sh` that wraps all Roddy cluster jobs
