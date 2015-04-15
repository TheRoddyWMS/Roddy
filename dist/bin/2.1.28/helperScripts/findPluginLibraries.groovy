// This script is used to extract plugin jar files from the various plugin directories.
// The plugin directories come from the roddy configuration
// The dependencies are stored within the buildinfo.txt files for the specified plugin
// The script needs two parameters: 
// - The plugin line from the configuration file
// - The working directory for the current plugin
// i.e. def args = ["pluginDirectories=/data/michael/Projekte/Roddy/Plugins", "/data/michael/Projekte/Roddy/Plugins/COExperimentalWorkflows"]

//TODO The script only takes one level of depth! Sync with the code from the Roddy binary! Maybe call that binay instead?

def workingDirectory = args[1]
def pluginLine = ["${workingDirectory}/dist/plugins"]
pluginLine.addAll(args[0].split("[=]")[1].split("[,][:]"))

def dependencies =  (new File("${workingDirectory}/buildinfo.txt")).readLines().findAll { it.startsWith("dependson") }.collect { it.split("[=]")[1];};
def collectedJars = dependencies.collect { 
	dep -> 
	path = dep.replace(":", "_"); 
	id = dep.split("[:]")[0];  
	pluginLine.collect { File f = new File(new File(it, path), "${id}.jar"); 
	return f 
}.find { it.exists() } }
collectedJars.each { println(it) }
return
