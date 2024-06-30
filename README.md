[![CircleCI](https://circleci.com/gh/TheRoddyWMS/Roddy/tree/master.svg?style=svg)](https://circleci.com/gh/TheRoddyWMS/Roddy/tree/master) [![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTheRoddyWMS%2FRoddy.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FTheRoddyWMS%2FRoddy?ref=badge_shield)

# What is Roddy? 

Roddy is a framework for development and management of workflows on a batch processing cluster. It has been developed at the German Cancer Research Center (DKFZ) in Heidelberg in the eilslabs group and is used by a number of in-house workflows such as the [PanCancer Alignment Workflow](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows) and the [ACEseq workflow](https://github.com/eilslabs/ACEseqWorkflow). The development is now continued in the Omics IT and Data Management Core Facility (ODCF) at the DKFZ.

> NOTE: This software is intended for research-use only!

> <table><tr><td><a href="https://www.denbi.de/"><img src="docs/images/denbi.png" alt="de.NBI logo" width="300" align="left"></a></td><td><strong>Your opinion matters!</strong> The development of Roddy is supported by the <a href="https://www.denbi.de/">German Network for Bioinformatic Infrastructure (de.NBI)</a>. By completing <a href="https://www.surveymonkey.de/r/denbi-service?sc=hd-hub&tool=roddy">this very short survey</a> you support our efforts to improve this tool.</td></tr></table>

> No new features will be implemented for Roddy! We will continue to fix bugs occurring with currently existing workflows. On the long run, existing workflows should be migrated to other workflow management systems. 

# Workflows

The following workflows have been developed at the DKFZ based on Roddy as workflow management system:

  * [Alignment and QC workflows](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows)
  * [SNV-Calling workflow](https://github.com/DKFZ-ODCF/SNVCallingWorkflow)
  * [ACEseq workflow](https://github.com/DKFZ-ODCF/ACEseqWorkflow) for copy-number variation calling
  * [InDel-Calling workflow](https://github.com/DKFZ-ODCF/IndelCallingWorkflow) workflow
  * [Sophia workflow](https://github.com/DKFZ-ODCF/SophiaWorkflow) for structural variation calling
  * [RNA-seq workflow](https://github.com/DKFZ-ODCF/RNAseqWorkflow)
  * CNVkit for copy-number variation calling on exome data (to be published)
  * Leaf-Cutter workflow
  * [Bam-to-FASTQ](https://github.com/TheRoddyWMS/BamToFastqPlugin) plugin
  
The following plugins are available as support for the workflows:

  * [COWorkflowBasePlugin](https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin) with basic control code for many of the workflows
  * [PluginBase](https://github.com/TheRoddyWMS/Roddy-Base-Plugin) just the plugin base-class from which other plugins are derived
  * [DefaultPlugin](https://github.com/TheRoddyWMS/Roddy-Default-Plugin) with the `wrapInScript.sh` that wraps all Roddy cluster jobs

# Documentation

You can find the documentation at [Read the Docs](http://roddy-documentation.readthedocs.io), including 

* [detailed installation instructions](https://roddy-documentation.readthedocs.io/en/latest/installationGuide.html)
* [contributors documentation](https://roddy-documentation.readthedocs.io/en/stable/roddyDevelopment/developersGuide.html)

# Contributing to Roddy

If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

Some basic information:

* We use [Semantic Versioning 2.0](https://semver.org/).
   * Release versions are named according to the pattern `\d\.\d\.\d(-(RC)?\d+`.
   * The first three levels are the "major", "minor", and "patch" number. The patch number is occasionally also called "build" number.
   * Additionally, to the major, minor, and patch numbers, a "revision" number `-\d+` can be attached.
   * It is possible to tag release candidate using suffixes `-RC\d+`
* We use [Github-Flow](https://githubflow.github.io/) as branching models.
* Additionally, to the "master" branch for long-term support of older versions it is possible to have dedicated release branches.
   * Release branches should be named according to the pattern `ReleaseBranch_\d+\.\d+(\.\d+)`.
* Issues can be marked with the following labels
  * `in progress`
  * `bug::candidate`
  * `bug::minor`
  * `bug::normal`
  * `bug::critical`

# Change Log

The change log is managed in a separate file [CHANGELOG.md](CHANGELOG.md).

# License

By contributing your code, you agree to license your contribution under the terms of the MIT License:

http://opensource.org/licenses/mit-license.html
https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt

If you are adding a new file it should have a header like this:

```
/**
 * Copyright 2024 German Cancer Research Center (DKFZ).
 * 
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
 ```
