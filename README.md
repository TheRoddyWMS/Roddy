# Overview

Roddy is a framework for cluster based workflow development.

# Prerequisites

Roddy currently needs a JDK / JRE installed in either ~/.roddy/runtime or ~/.roddy/runtimeDevel
Roddy versions 2.2 and below need Groovy 2.3.x and the JDK/JRE in version 8 (Oracle!)
Roddy versions 2.3 and upwards need Groovy 2.4.x and the JDK/JRE in version 8 (also Oracle!)

```
~/Projekte/Roddy> ll ~/.roddy/runtimeDevel/
 groovy -> groovy-2.3.6
 groovy-2.3.6
 groovy-2.4.3
 jdk -> jdk1.8.0_20
 jdk1.8.0_20
 jdk1.8.0_40
 jre -> jdk/jre
```

Support for Groovy 2.4.x has been tested and it is currently not working!

# Contents



# Directory Structure

RoddyCore   The core Roddy project with the include CLI client.

RoddyGUI    Roddys graphical user interface (Currently not in use)

dist 	    A ready to use version of roddy (without jre or groovy)
            The folder also contains Roddy jar files with version tags.

dist/plugins
            The folder contains basic Roddy plugins
            It also might contain zip files with different version tags.

dist/plugins/PluginBase
	        A basic project from which plugins must be derived./dist/plugins/Template
	        A template plugin which can be used for your own development.

dist/plugins/Template
	        A template plugin which you can use to create your own plugins or workflows.
	        This also gets used if you call the createnewworkflow option.

# Available configuration flags for project configuration files

```
ID                                      Default Description
debugOptionsUsePipefail                 true    Enable process termination for pipes
debugOptionsUseVerboseOutput            true    Print a lot, like "set -v" in bash
debugOptionsUseExecuteOutput            true    Print executed lines, like "set -x" in bash
debugOptionsUseUndefinedVariableBreak   false   Fail, if a variable in a script is missing, like "set -u" in bash
debugOptionsUseExitOnError              false   Fail, if a script throws an error, like "set -e" in bash
debugOptionsParseScripts                false

processOptionsSetUserGroup              true    Overrides the users default group on the target system
processOptionsSetUserMask               true    Overrides the users default usermask on the target system
processOptionsQueryEnv                  false   Call something like env (in bash) on the target system
processOptionsQueryID                   false

```