// This script is used to extract plugin jar files from the various plugin directories.
// The plugin directories come from the roddy configuration
// The dependencies are stored within the buildinfo.txt files for the specified plugin
// The script needs two parameters: 
// - The plugin line from the configuration file
// - The roddy binary directory
// - The working directory for the current plugin
// i.e. def args = ["pluginDirectories=/data/michael/Projekte/Roddy/Plugins", "/data/michael/Projekte/Roddy/Plugins/COExperimentalWorkflows"]

//TODO The script only takes one level of depth! Sync with the code from the Roddy binary! Maybe call that binay instead?

def roddyDirectory = args[1]
def workingDirectory = args[2]
def pluginLine = ["${roddyDirectory}/dist/plugins"]
pluginLine.addAll(args[0].split("[=]")[1].split("[,][:]"))
def dependencies =  (new File("${workingDirectory}/buildinfo.txt")).readLines().findAll { it.startsWith("dependson") }.collect { it.split("[=]")[1];};
def libs = []
def collectedJars = dependencies.collect { 
	dep ->
	path = dep.replace(":", "_");
	id = dep.split("[:]")[0];
	path = path.replace("_current", "");
	pluginLine.collect { File f = new File(new File(it, path), "${id}.jar");
	return f
}.each { if(it.exists()) libs << it } }
println libs.join(":")
return
