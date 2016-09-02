/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

// This script is used to extract find all plugin base folders.
// The script needs several parameters:
// - The plugin line from the configuration file
// - The working directory for the current plugin
// - The plugin id for which the folder should be resolved.
//def args = ["pluginDirectories=/data/michael/Projekte/Roddy/Plugins", "/data/michael/Projekte/Roddy", "COWorkflows"]


def splitPlugin = args[0].split("[=]");
def workingDirectory = args[1]
def pluginInfo = args[2].split(":")

def pluginID = pluginInfo[0]
def pluginVersion = pluginInfo.size() > 1 ? pluginInfo[1] : null
def pluginLine = ["${workingDirectory}/dist/plugins", "${workingDirectory}/dist/plugins_2.49plus", "${workingDirectory}/dist/plugins_R2.3"]
def pluginFolders = []

if (splitPlugin.size() > 1) pluginLine.addAll(splitPlugin[1].split("[,]"))
pluginLine.each { pl -> pluginFolders.addAll(new File(pl).listFiles().findAll { dir -> dir.isDirectory() }) }
println(pluginFolders.find { it.name.endsWith(pluginID + ( (pluginVersion && !pluginVersion.equals("current"))? "_$pluginVersion" : "")); })

return