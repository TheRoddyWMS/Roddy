/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

// This script is used to extract plugin jar files from the various plugin directories.
// The plugin directories come from the roddy configuration
// The dependencies are stored within the buildinfo.txt files for the specified plugin
// The script needs several parameters:
// - The plugin line from the configuration file
// - The roddy binary directory
// - The working directory for the current plugin
// i.e. def args = ["pluginDirectories=/data/michael/Projekte/Roddy/Plugins", "/data/michael/Projekte/Roddy/Plugins/COExperimentalWorkflows"]

def roddyDirectory = args[1]
def workingDirectory = args[2]
def pluginDirectories = args[0]
def pluginLine = ["${roddyDirectory}/dist/plugins"]  // Add the Roddy directory
pluginLine.addAll(pluginDirectories.split("[=]")[1].split("[,:]")) // Add lines from ini file
pluginLine = pluginLine.unique() // Filter the list to get rid of doublettes.

def dependencies =  (new File("${workingDirectory}/buildinfo.txt")).readLines().findAll { it.startsWith("dependson") }.collect { it.split("[=]")[1];};
def possibleJars = []

dependencies.each {
	dep ->
		path = dep.replace(":", "_");
		id = dep.split("[:]")[0];
		path = path.replace("_current", "");
		possibleJars += pluginLine.collect { File f = new File(new File(it, path), "${id}.jar"); }
}

def libs = possibleJars.findAll() { it.exists()  }

if(!libs.size() == dependencies.size())
	System.exit(5);

println(libs.join(":"))
return
