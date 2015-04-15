// This script is used to extract find all plugin base folders.
// The script needs two parameters:
// - The plugin line from the configuration file
// - The working directory for the current plugin
// - The plugin id for which the folder should be resolved.
//def args = ["pluginDirectories=/data/michael/Projekte/Roddy/Plugins", "/data/michael/Projekte/Roddy", "COWorkflows"]
def pluginID = args[2]
def workingDirectory = args[1]
def pluginLine = ["${workingDirectory}/dist/plugins"]
def pluginFolders = []
pluginLine.addAll(args[0].split("[=]")[1].split("[,]"))
pluginLine.each { pl -> pluginFolders.addAll(new File(pl).listFiles().findAll { dir -> dir.isDirectory() }) }
println(pluginFolders.find { it.name.endsWith(pluginID); })

return