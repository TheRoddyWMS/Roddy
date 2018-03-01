/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */


List<String> parsePluginDirectoriesParameter(String roddyDirectory, String parameterValue) {
	// Note: The plugins in the dist/plugins directory will be found first!
	List<String> pluginLines = ["${roddyDirectory}/dist/plugins"] // Add the Roddy directory
	pluginLines.addAll(parameterValue.split("[=]")[1].split("[,:]")) // Add lines from ini file
	return pluginLines.unique() // Filter the list to get rid of doublettes.
}

List<String> extractDependenciesFromBuildinfoFile(String pluginDir) {
	File pluginPath = new File(pluginDir)
	if (!pluginPath.canExecute()) {
		throw new FileNotFoundException("Cannot access directory ${pluginPath.toString()}")
	}
	return (new File(pluginPath, "buildinfo.txt")).
			readLines().
			findAll {
				it.startsWith("dependson")
			}.collect {
		it.split("[=]")[1]
	}
}

/** Given a plugin name with version, if the version is not 'develop' then the plugin jar may be called $id_$version.jar or $id.jar.
 *  If the searched plugin version is 'develop', the jar must be called $id.jar.
 *  This allows to compile against plugin directories renamed to versioned directories, but still containing older jars compiled while they were
 *  'develop'.
 */
List<File> findPossibleJarsForDependency(List<String> pluginDirectories, String pluginNameAndVersion) {
	String path = pluginNameAndVersion.replace(":", "_")
	def (id, version) = pluginNameAndVersion.split("[:]")
	path = path.replace("_develop", "")

	return pluginDirectories.collect { String line ->
		def jar = new File(new File(line, path), "${id}.jar")
		if (version != "develop")
			[new File(new File(line, path), "${id}_${version}.jar"), jar]
		else
			[jar]
	}.flatten()
}

List<File> findPossibleJarsForDependencies(List<String> pluginDirectories, List<String> pluginsAndVersions) {
	return pluginsAndVersions.collect { findPossibleJarsForDependency(pluginDirectories, it) }.flatten()
}

List<File> collectAllJars(List<String> pluginSearchDirectories, String pluginDirectory) {
	List<String> dependencies = extractDependenciesFromBuildinfoFile(pluginDirectory)
	List<File> possibleJars = findPossibleJarsForDependencies(pluginSearchDirectories, dependencies)

	def jars = possibleJars.findAll() { it.exists()  }
	if(!jars.size() == dependencies.size()) {
		System.err.println("Number of jars didn't match the number of dependencies: dependencies=${dependencies.size()}, jars=${jars.size()}, pluginDirectory=${pluginDirectory}")
		System.exit(5)
	}

	return jars.collect {
		it.parent.toString()
	}.collect {
		jars + collectAllJars(pluginSearchDirectories, it)
	}.flatten().unique()
}


// This script is used to extract plugin jar files from the various plugin directories.
// The plugin directories come from the roddy configuration
// The dependencies are stored within the buildinfo.txt files for the specified plugin
// The script needs several parameters:
// - The plugin line from the configuration file
// - The roddy binary directory
// - The working directory for the current plugin

if (args.size() != 3) {
	System.err.println('Given a list of pluginDirectories (search paths), a roddyDirectory, and a pluginDirectory, find all plugin jars in the plugins dependencies.')
	System.err.println('Usage: findPluginLibraries pluginDirectories=pluginDirectoryA,pluginDirectoryB roddyDirectory/ pluginDirectory/')
	System.exit(1)
}
def pluginDirectoryParameter = args[0]
def roddyDirectory = args[1]
def pluginDirectory = args[2]

List<String> pluginDirectories = parsePluginDirectoriesParameter(roddyDirectory, pluginDirectoryParameter)
List<File> jars = collectAllJars(pluginDirectories, pluginDirectory).reverse()
System.out.println(jars.join(":"))


