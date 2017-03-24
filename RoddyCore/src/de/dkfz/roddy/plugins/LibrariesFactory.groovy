/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.client.cliclient.CommandLineCall
import de.dkfz.roddy.execution.io.ExecutionHelper
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.tools.*
import de.dkfz.roddy.tools.Tuple2
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.Initializable
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.TypeCheckingMode

import java.lang.reflect.Method
import java.util.regex.Pattern

/**
 * Factory to load and integrate plugins.
 */
@groovy.transform.CompileStatic
public class LibrariesFactory extends Initializable {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(LibrariesFactory.class.getSimpleName());

    private static LibrariesFactory librariesFactory;

    public static URLClassLoader urlClassLoader
    public static GroovyClassLoader centralGroovyClassLoader;

    public static final String SYNTHETIC_PACKAGE = "de.dkfz.roddy.synthetic.files"

    public static final String PLUGIN_VERSION_CURRENT = "current";
    public static final String PLUGIN_DEFAULT = "DefaultPlugin";
    public static final String PLUGIN_BASEPLUGIN = "PluginBase";
    public static final String BUILDINFO_DEPENDENCY = "dependson";
    public static final String BUILDINFO_COMPATIBILITY = "compatibleto";
    public static final String BUILDINFO_TEXTFILE = "buildinfo.txt";
    public static final String BUILDINFO_STATUS = "status"
    public static final String BUILDINFO_STATUS_BETA = "beta"
    public static final String BUILDINFO_RUNTIME_JDKVERSION = "JDKVersion"
    public static final String BUILDINFO_RUNTIME_GROOVYVERSION = "GroovyVersion"
    public static final String BUILDINFO_RUNTIME_APIVERSION = "RoddyAPIVersion"

    private List<String> loadedLibrariesInfo = [];

    private List<PluginInfo> loadedPlugins = [];

    private Map<PluginInfo, File> loadedJarsByPlugin = [:]

    private PluginInfoMap mapOfPlugins = [:];

    private boolean librariesAreLoaded = false;

    private SyntheticPluginInfo synthetic

    /**
     * The synthetic plugin info object is solely used for automatically created synthetic file classes.
     * It is not usable for other purposes.
     */
    private static class SyntheticPluginInfo extends PluginInfo {

        SyntheticPluginInfo(String name, File zipFile, File directory, File developmentDirectory, String prodVersion, Map<String, String> dependencies) {
            super(name, zipFile, directory, developmentDirectory, prodVersion, RuntimeTools.getRoddyRuntimeVersion(), RuntimeTools.getJavaRuntimeVersion(), RuntimeTools.getGroovyRuntimeVersion(), dependencies);
        }

        private Map<String, Class> map = [:]

        public void addClass(Class cls) {
            map[cls.name] = cls;
            map[cls.simpleName] = cls;
        }
    }

    /**
     * This resets the singleton and is not thread safe!
     * Actually only creates a new singleton clearing out old values.
     * @return
     */
    public static LibrariesFactory initializeFactory(boolean enforceinit = false) {
        if (!librariesFactory || enforceinit)
            librariesFactory = new LibrariesFactory();
        return librariesFactory;
    }

    private LibrariesFactory() {
        synthetic = new SyntheticPluginInfo("Synthetic", null, null, null, "current", [:]);
    }

    public SyntheticPluginInfo getSynthetic() {
        return synthetic;
    }

    /**
     * TODO Leave this static? Or make it a libraries factory based thing?
     * @return
     */
    public static GroovyClassLoader getGroovyClassLoader() {
        if (centralGroovyClassLoader == null) {
            centralGroovyClassLoader = new GroovyClassLoader(ClassLoader.getSystemClassLoader())
            urlClassLoader = centralGroovyClassLoader;
        }
        return centralGroovyClassLoader;
    }

    private Map<PluginInfo, List<String>> classListCacheByPlugin = [:];

    private Package[] _cachedRoddyPackages = null;

    Package[] getRoddyPackages() {
        if (!_cachedRoddyPackages) {
            def gcl = getGroovyClassLoader()
            Method getPackages //= gcl.class.getDeclaredMethod("getPackages")
            for (Class clc = gcl.class; clc != null; clc = clc.superclass) {
                def foundMethod = clc.getDeclaredMethods().find { Method m -> m.name == "getPackages" }
                if (!foundMethod)
                    continue;
                getPackages = foundMethod;
                break;
            }
            if (!getPackages) throw new RuntimeException("The classloader hierarchy does not contain a valid classloader with the getPackages method")

            // Make method accessible
            getPackages.setAccessible(true)
            Package[] packageList = getPackages.invoke(gcl) as Package[]
            // Invoke it and get list of all gcl loaded packages
            Package[] roddyPackages = packageList.findAll { Package p -> p.name.startsWith(Roddy.package.name) }
            // Find all Roddy packages
            _cachedRoddyPackages = roddyPackages
            return roddyPackages
        }
        return _cachedRoddyPackages
    }

    Class searchForClass(String name) {
        def groovyClassLoader = getGroovyClassLoader()
        if (name.contains(".")) {
            return groovyClassLoader.loadClass(name);
        } else {
            // Search synthetic classes first.
            if (getSynthetic().map.containsKey(name))
                return getSynthetic().map[name];

            // Search core classes second. Find packages of Roddy first. Search for the class in every! package.
            // This is some bad reflection, but I won't get the package information without it!
            Class foundCoreClass = null;
            for (Package p in getRoddyPackages()) {
                String className = "${p.name}.${name}"
                try {
                    foundCoreClass = loadClass(className)
                } catch (ClassNotFoundException ex) {
                    // Silently ignore it. If the class can not be found, it is fine here.
                }
                if (foundCoreClass) break
                // Ignore if it is empty, we will fall back to the plugin strategy afterwards! Or search in the next package
            }

            // We found it in core, so return it.
            if (foundCoreClass)
                return foundCoreClass

            // TODO This is a very quick hack and heavily depends on the existence of jar on the system!
            // Java normally ships jar with it, so it might not be the SEVERE / SUPERBAD bad hack.
            List<String> listOfClasses = []
            synchronized (loadedPlugins) {
                loadedPlugins.each {
                    PluginInfo plugin ->
                        if (!classListCacheByPlugin.containsKey(plugin)) {
                            String text = ExecutionHelper.execute("jar tvf ${loadedJarsByPlugin[plugin]}")
                            classListCacheByPlugin[plugin] = text.readLines();
                        }
                        classListCacheByPlugin[plugin].each {
                            String line ->
                                if (!line.endsWith(".class")) return;
                                String cls = line.split("[ ]")[-1][0..-7];
                                if (cls.endsWith("/" + name)) {
                                    cls = cls.replace("/", ".");
                                    cls = cls.replace("\\", ".");
                                    synchronized (listOfClasses) {
                                        listOfClasses << cls;
                                    }
                                }
                        }
                }
            }
            if (listOfClasses.size() > 1) {
                logger.severe("Too many available classes, please specify fully, choosing one of the following: ")
                listOfClasses.each { logger.severe("  " + it) }
                return null;
            }
            if (listOfClasses.size() == 1) {
                return groovyClassLoader.loadClass(listOfClasses[0]);
            }
            logger.severe("No class found for ${name}")
            return null;
        }
    }

    public Class loadRealOrSyntheticClass(String classOfFileObject, String baseClassOfFileObject) {
        Class<BaseFile> _cls = searchForClass(classOfFileObject);
        if (_cls == null) {
            _cls = generateSyntheticFileClassWithParentClass(classOfFileObject, baseClassOfFileObject, LibrariesFactory.getGroovyClassLoader())
            LibrariesFactory.getInstance().getSynthetic().addClass(_cls);
            logger.severe("Class ${classOfFileObject} could not be found, created synthetic class ${_cls.name}.");
        }
        return _cls
    }

    public Class loadRealOrSyntheticClass(String classOfFileObject, Class<FileObject> constructorClass) {
        return loadRealOrSyntheticClass(classOfFileObject, constructorClass.name);
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public static Class generateSyntheticFileClassWithParentClass(String syntheticClassName, String constructorClassName, GroovyClassLoader classLoader = null) {
        String syntheticFileClass =
                """
                package $SYNTHETIC_PACKAGE

                public class ${syntheticClassName} extends de.dkfz.roddy.knowledge.files.BaseFile {

                    public ${syntheticClassName}(de.dkfz.roddy.knowledge.files.BaseFile.ConstructionHelperForBaseFiles helper) {
                        super(helper);
                    }
                }
            """
        GroovyClassLoader groovyClassLoader = classLoader ?: new GroovyClassLoader();
        Class _classID = (Class<BaseFile>) groovyClassLoader.parseClass(syntheticFileClass);
        return _classID
    }

    /**
     * Resolve all used / necessary plugins and also look for miscrepancies.
     * @param usedPlugins
     */
    public boolean resolveAndLoadPlugins(String[] usedPlugins) {
        def mapOfAvailablePlugins = loadMapOfAvailablePluginsForInstance()
        if (!mapOfAvailablePlugins) {
            logger.severe("Could not load plugins from storage. Are the plugin directories properly set?\n" + Roddy.getPluginDirectories().join("\n\t"))
            return false
        }
        def queue = buildupPluginQueue(mapOfAvailablePlugins, usedPlugins)
        if (queue == null) {
            logger.severe("Could not build the plugin queue for: \n" + usedPlugins.join("\n\t"))
            return false
        }
        librariesAreLoaded = loadLibraries(queue.values() as List);
        return librariesAreLoaded;
    }

    public boolean areLibrariesLoaded() {
        return librariesAreLoaded
    }

    public List<PluginInfo> getLoadedPlugins() {
        return loadedPlugins;
    }

    public PluginInfoMap loadMapOfAvailablePluginsForInstance() {
        if (!mapOfPlugins) {
            def directories = Roddy.getPluginDirectories()
            mapOfPlugins = loadMapOfAvailablePlugins(directories)
        };
        return mapOfPlugins
    }

    /**
     * This method returns a list of all plugins found in plugin directories.
     * This method distinguishes between Roddy environments for development and packed Roddy versions.
     * If the user is a developer, the development directories are set and preferred over bundled libraries.
     * * Why is this necessary?
     * - If you develop scripts and plugins, you possibly use a version control system. However the scripts are copied to the
     *   plugin directory on compilation (and possibly out of version control. So Roddy tries to take the "original" version
     *   which resides in the plugins project space.
     * * Where can plugins be found?
     * - In the dist/libraries folder (non developer)
     * - In any other configured folder. You as the developer has to set external projects up in the configuration. (developer)
     *
     * @return
     */
    static PluginInfoMap loadMapOfAvailablePlugins(List<File> pluginDirectories) {

        //Search all plugin folders and also try to join those if possible.
        List<Tuple2<File, String[]>> collectedPluginDirectories = [];
        boolean warningUnzippedDirectoriesMissing = false;

        for (File pBaseDirectory : pluginDirectories) {
            logger.postSometimesInfo("Parsing plugins folder: ${pBaseDirectory}");
            if (!pBaseDirectory.exists()) {
                logger.warning("The plugins directory $pBaseDirectory does not exist.")
                continue;
            }
            if (!pBaseDirectory.canRead()) {
                logger.warning("The plugins directory $pBaseDirectory is not readable.")
            }

            File[] directoryList = pBaseDirectory.listFiles().sort() as File[];
            for (File pEntry in directoryList) {

                if (!isValidPluginFolder(pEntry))
                    continue;

                String[] splitName = pEntry.name.split(StringConstants.SPLIT_UNDERSCORE); //First split for .zip then for the version
                collectedPluginDirectories << new Tuple2<File, String[]>(pEntry, splitName);
            }
        }

        return loadPluginsFromDirectories(collectedPluginDirectories)
    }

    /**
     * This and the following method should not be in here! We should use the FileSystemAccessProvider for it.
     * However, the FSAP always tries to use the ExecService, if possible. All in all, with the current setup for FSAP / ES
     * interaction, it will not work. As we already decided to change that at some point, I'll put the method in here
     * and mark them as deprecated.
     * @param file
     * @return
     */
    @Deprecated
    private static boolean checkFile(File file) {
        return file.exists() && file.isFile() && file.canRead()
    }

    @Deprecated
    private static boolean checkDirectory(File file) {
        return file.exists() && file.isDirectory() && file.canRead() && file.canExecute()
    }

    static boolean isValidPluginFolder(File directory) {
        logger.postRareInfo("  Parsing plugin folder: ${directory}");

        List<String> errors = []

        if (!directory.isDirectory())
            errors << "File is not a directory"
        if (directory.isHidden())
            errors << "Directory is hidden"
        if (!directory.canRead())
            errors << "Directory cannot be read"

        if (errors) {
            logger.postRareInfo((["A directory was rejected as a plugin directory because:"] + errors).join("\n\t"))
            return false
        }

        String dirName = directory.getName();
        if (!isPluginDirectoryNameValid(dirName)) {
            logger.postRareInfo("A directory was rejected as a plugin directory because its name did not match the naming rules.")
            return false
        }

//        def f = Roddy.getLocalFileSystemInfoProvider()
        if (!checkFile(new File(directory, "buildinfo.txt")))
            errors << "The buildinfo.txt file is missing"
        if (!checkFile(new File(directory, "buildversion.txt")))
            errors << "The buildversion.txt file is missing"
        if (!checkDirectory(new File(directory, "resources/analysisTools")))
            errors << "The analysisTools resource directory is missing"
        if (!checkDirectory(new File(directory, "resources/configurationFiles")))
            errors << "The configurationFiles resource directory is missing"

        if (errors) {
            logger.postRareInfo((["A directory was rejected as a plugin directory because:"] + errors).join("\n\t"))
            return false
        }
        return true
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private static List<Tuple2<File, String[]>> checkValidPluginNames(List<Tuple2<File, String[]>> collectedPluginDirectories) {
        List<Tuple2<File, String[]>> collectedTemporary = [];
        collectedPluginDirectories.each { tuple ->
            String rev = (tuple.x.name.split("[-]") as List)[1]
            if (tuple.x.name.endsWith(".zip")) {
                logger.info("Did not consider to check ${tuple.x.name} as it is compressed and cannot be evaluated.")
                return
            }
            if (rev) rev = rev.split("[.]")[0]; // Filter out .zip
            if (rev?.isNumber() || !rev) collectedTemporary << tuple
            else logger.severe("Filtered out plugin ${tuple.x.name}, as the revision id is not numeric.")
        }
        return collectedTemporary
    }

    /** Make sure that the plugin directories are properly sorted before we start. This is especially useful
     *  if we search for revisions and extensions.
     */
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private static List<Tuple2<File, String[]>> sortPluginDirectories(List<Tuple2<File, String[]>> collectedPluginDirectories) {
        collectedPluginDirectories = collectedPluginDirectories.sort {
            Tuple2<File, String[]> left, Tuple2<File, String[]> right ->
                logger.postRareInfo("Call to plugin directory sort for ${left.x} vs ${right.x}");
                List<String> splitLeft = left.x.name.split("[_:.-]") as List;
                List<String> splitRight = right.x.name.split("[_:.-]") as List;
                Tuple5<String, Integer, Integer, Integer, Integer> tLeft = new Tuple5<>(
                        splitLeft[0],
                        (splitLeft[1] ?: Integer.MAX_VALUE) as Integer,
                        (splitLeft[2] ?: Integer.MAX_VALUE) as Integer,
                        (splitLeft[3] ?: Integer.MAX_VALUE) as Integer,
                        (splitLeft[4] ?: 0) as Integer
                );
                Tuple5<String, Integer, Integer, Integer, Integer> tRight = new Tuple5<>(
                        splitRight[0],
                        (splitRight[1] ?: Integer.MAX_VALUE) as Integer,
                        (splitRight[2] ?: Integer.MAX_VALUE) as Integer,
                        (splitRight[3] ?: Integer.MAX_VALUE) as Integer,
                        (splitRight[4] ?: 0) as Integer
                );
                if (tLeft.x != tRight.x) return tLeft.x.compareTo(tRight.x);
                if (tLeft.y != tRight.y) return tLeft.y.compareTo(tRight.y);
                if (tLeft.z != tRight.z) return tLeft.z.compareTo(tRight.z);
                if (tLeft.w != tRight.w) return tLeft.w.compareTo(tRight.w);
                if (tLeft.q != tRight.q) return tLeft.q.compareTo(tRight.q);
                return 0;
        }
        return collectedPluginDirectories
    }

    /**
     * Loads all available plugins (including revisions and versions) from a set of directories.
     * Type checking mode is set to skip for this method, because Groovy does not likes Generics and Comparisons in a combination...
     * return tLeft.x.compareTo(tRight.x) * 10000 +
     * tLeft.y.compareTo(tRight.y) * 1000 +
     * tLeft.z.compareTo(tRight.z) * 100 +
     * tLeft.w.compareTo(tRight.w) * 10 +
     * tLeft.q.compareTo(tRight.q) * 1;
     *
     * Will not work!
     *
     * @param collectedPluginDirectories
     * @return
     */
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private static PluginInfoMap loadPluginsFromDirectories(List<Tuple2<File, String[]>> collectedPluginDirectories) {
        collectedPluginDirectories = sortPluginDirectories(checkValidPluginNames(collectedPluginDirectories))

        Map<String, Map<String, PluginInfo>> _mapOfPlugins = [:];
        for (Tuple2<File, String[]> _entry : collectedPluginDirectories) {
            logger.postRareInfo("Processing plugin entry: ${_entry.x}")
            File pEntry = _entry.x;
            String[] splitName = _entry.y;

            String pluginName = splitName[0];
            String[] pluginVersionInfo = splitName.length > 1 ? splitName[1].split(StringConstants.SPLIT_MINUS) : [PLUGIN_VERSION_CURRENT] as String[];
            String pluginVersion = pluginVersionInfo[0];
            String pluginRevision = pluginVersionInfo.length > 1 ? pluginVersionInfo[1] : "0";
            String pluginFullVersion = pluginVersion + "-" + pluginRevision;
            if (pluginVersion == PLUGIN_VERSION_CURRENT) pluginFullVersion = PLUGIN_VERSION_CURRENT;

            int revisionNumber = pluginRevision as Integer;

            CommandLineCall clc = Roddy.getCommandLineCall();
            boolean helpMode = clc ? clc.startupMode == RoddyStartupModes.help : false

            File develEntry = null;
            File prodEntry = null;
            File zipFile = null;

            if (zipFile != null) { // Only "releases" / packages have a zip file and need not to be dissected further.
                continue;
            }

            // TODO Sort file list before validation?
            // TODO Get version also from directory in case that the zip file is missing?

            File prodJarFile = pEntry.listFiles().find { File f -> f.name.endsWith ".jar"; }
            if (prodJarFile) {
                prodEntry = pEntry;
            }

            File devSrcPath = pEntry.listFiles().find { File f -> f.name == "src"; }
            if (devSrcPath) {
                develEntry = pEntry;
            }

            if (!prodEntry && !develEntry) {
                //Now we might have a plugin without a jar file. This is allowed to happen since 2.2.87
                prodEntry = pEntry;
            }


            def pluginMap = _mapOfPlugins.get(pluginName, new LinkedHashMap<String, PluginInfo>())

            BuildInfoFileHelper biHelper = new BuildInfoFileHelper(pluginName, pluginFullVersion, pEntry.listFiles().find { File f -> f.name == BUILDINFO_TEXTFILE })

            PluginInfo previousPlugin = pluginMap.values().size() > 0 ? pluginMap.values().last() : null;
            boolean isRevisionOfPlugin = previousPlugin?.getMajorAndMinor() == pluginVersion && previousPlugin?.getRevision() == revisionNumber - 1;
            boolean isCompatible = biHelper.isCompatibleTo(previousPlugin);
            boolean isBetaPlugin = biHelper.isBetaPlugin();

            //Create a helper object which parses the buildinfo text file

            PluginInfo newPluginInfo = new PluginInfo(pluginName, zipFile, prodEntry, develEntry, pluginFullVersion, biHelper.getRoddyAPIVersion(), biHelper.getJDKVersion(), biHelper.getGroovyVersion(), biHelper.getDependencies())
            pluginMap[pluginFullVersion] = newPluginInfo;
            if (isRevisionOfPlugin || isCompatible) {
                newPluginInfo.previousInChain = previousPlugin;
                previousPlugin.nextInChain = newPluginInfo;
            }

            if (isRevisionOfPlugin)
                newPluginInfo.previousInChainConnectionType = PluginInfo.PluginInfoConnection.REVISION;
            else if (isCompatible)
                newPluginInfo.previousInChainConnectionType = PluginInfo.PluginInfoConnection.EXTENSION;

            if (isBetaPlugin)
                newPluginInfo.isBetaPlugin = true;
        }
        return new PluginInfoMap(_mapOfPlugins)
    }

    public static Map<String, PluginInfo> buildupPluginQueue(PluginInfoMap mapOfPlugins, String[] usedPlugins) {
        List<String> usedPluginsCorrected = [];
        List<Tuple2<String, String>> pluginsToCheck = usedPlugins.collect { String requestedPlugin ->
            List<String> pSplit = requestedPlugin.split("[:-]") as List;
            String id = pSplit[0];
            String version = pSplit[1] ?: PLUGIN_VERSION_CURRENT;
            String revision = pSplit[2] ?: "0"
            String fullVersion = version + (version != PLUGIN_VERSION_CURRENT ? "-" + revision : "")

            usedPluginsCorrected << [id, fullVersion].join(":");
            return new Tuple2(id, fullVersion);
        }
        usedPlugins = usedPluginsCorrected;

        Map<String, PluginInfo> pluginsToActivate = [:];
        while (pluginsToCheck.size() > 0) {

            final String id = pluginsToCheck[0].x;
            String version = pluginsToCheck[0].y;
            //There are now some  as String conversions which are just there for the Idea code view... They'll be shown as faulty otherwise.
            if (version != PLUGIN_VERSION_CURRENT && !(version as String).contains("-")) version += "-0";

            if (!mapOfPlugins.checkExistence(id as String, version as String)) {
                logger.severe("The plugin ${id}:${version} could not be found, are the plugin paths properly set?");
                return null;
            }
            pluginsToCheck.remove(0);

            // Set pInfo to a valid instance.
            PluginInfo pInfo = mapOfPlugins.getPluginInfo(id as String, version as String);

            // Now, if the plugin is not in usedPlugins (and therefore not fixed), we search the newest compatible
            // version of it which may either be a revision (x:x.y-[0..n] or a higher compatible version.
            // Search the last valid entry in the chain.
            if (!usedPlugins.contains("${id}:${version}")) {
                while (true) {
                    version = pInfo.prodVersion;
                    if (usedPlugins.contains("${id}:${version}")) //Break, if the list of used plugins contains the selected version of the plugin
                        break
                    if (pInfo.nextInChain == null) break // Break if this was the last entry in the chain
                    pInfo = pInfo.nextInChain
                }
            }

            if (pInfo == null)
                pInfo = mapOfPlugins.getPluginInfo(id as String, PLUGIN_VERSION_CURRENT);
            if (pInfo == null)
                continue;
            if (pluginsToActivate[id as String] != null) {
                if (pluginsToActivate[id as String].prodVersion != version) {
                    def msg = "Plugin version collision: Plugin ${id} cannot both be loaded in version ${version} and ${pluginsToActivate[id as String].prodVersion}. Not starting up."
                    logger.severe(msg)
                    return null
                } else {
                    //Not checking again!
                }
            } else {
                Map<String, String> dependencies = pInfo.getDependencies()
                dependencies.each { String k, String v ->
                    if (v != PLUGIN_VERSION_CURRENT && !v.contains("-")) v += "-0";
                    pluginsToCheck << new Tuple2(k, v);
                }
                pluginsToActivate[id as String] = pInfo;
            }
            //Load default plugins, if necessary.
            if (!pluginsToCheck) {
                if (!pluginsToActivate.containsKey(PLUGIN_DEFAULT)) {
                    pluginsToActivate[PLUGIN_DEFAULT] = mapOfPlugins.getPluginInfo(PLUGIN_DEFAULT, PLUGIN_VERSION_CURRENT);
                }
                if (!pluginsToActivate.containsKey(PLUGIN_BASEPLUGIN)) {
                    pluginsToActivate[PLUGIN_BASEPLUGIN] = mapOfPlugins.getPluginInfo(PLUGIN_BASEPLUGIN, PLUGIN_VERSION_CURRENT);
                }
            }
        }
        return pluginsToActivate;
    }

    public static boolean addFile(File f) throws IOException {
        return addURL(f.toURI().toURL());
    }

    /**
     * The following method adds a jar file to the current classpath.
     * The code is initially taken from here:
     * http://stackoverflow.com/questions/60764/how-should-i-load-jars-dynamically-at-runtime
     * Beware that classes must only be added once due to several constrictions.
     * See the mentioned site for more information.
     *
     * @param u
     * @throws IOException
     */
    public static boolean addURL(URL u) throws IOException {
        try {
            getGroovyClassLoader().addURL(u);
            return true;
        } catch (Throwable t) {
            logger.severe("A plugin could not be loaded: " + u)
            return false;
        }
    }

    public static LibrariesFactory getInstance() {
        if (librariesFactory == null) {
            logger.postAlwaysInfo("The libraries factory for plugin management was not initialized! Creating a new, empty object.")
            librariesFactory = new LibrariesFactory();
        }

        return librariesFactory;
    }

    public boolean loadLibraries(List<PluginInfo> pluginInfo) {
        if (!performAPIChecks(pluginInfo))
            return false;

        // TODO Cover with a unit or integration test (if not already done...)
        List<String> errors = [];
        //All is right? Let's go
        pluginInfo.parallelStream().each { PluginInfo pi ->
            if (!pi.directory) {
                synchronized (errors) {
                    errors << "Ignored ${pi.fullID}, directory not found.".toString();
                }
                return;
            }

            File jarFile = pi.directory.listFiles().find { File f -> f.name.endsWith(".jar") };
            if (jarFile && !addFile(jarFile)) {
                synchronized (errors) {
                    errors << "Ignored ${pi.fullID}, Jar file was not available.".toString();
                }
                return;
            }

            def loadInfo = "The plugin ${pi.getName()} [ Version: ${pi.getProdVersion()} ] was loaded."
            logger.postAlwaysInfo(loadInfo)
            synchronized (loadedLibrariesInfo) {
                loadedPlugins << pi;
                loadedLibrariesInfo << loadInfo.toString()
                loadedJarsByPlugin[pi] = jarFile;
            }
        }

        if (errors) {
            logger.severe("Some plugins were not loaded:\n\t" + errors.join("\n\t"));
        }
        return !errors;
    }

    /**
     * Perform checks, if all API versions match the current runtime setup.
     * Includes Groovy, Java and Roddy.
     */
    public static boolean performAPIChecks(List<PluginInfo> pluginInfos) {
        List<PluginInfo> incompatiblePlugins = []
        for (pi in pluginInfos) {
            if (!(RuntimeTools.groovyRuntimeVersion == pi.getGroovyVersion() &&
                    RuntimeTools.javaRuntimeVersion == pi.getJdkVersion() &&
                    RuntimeTools.roddyRuntimeVersion == pi.getRoddyAPIVersion()))
                incompatiblePlugins << pi;
        }
        if (incompatiblePlugins) {
            logger.severe("Could not load plugins, runtime API versions mismatch! (Current Groovy: ${RuntimeTools.groovyRuntimeVersion}, JDK ${RuntimeTools.javaRuntimeVersion}, Roddy ${RuntimeTools.getRoddyRuntimeVersion()})\n"
                    + incompatiblePlugins.collect { PluginInfo pi -> pi.fullID }.join("\n\t")
            )
        }
        return !incompatiblePlugins;
    }

    public List<String> getLoadedLibrariesInfoList() {
        return loadedLibrariesInfo;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return getGroovyClassLoader().loadClass(className);
    }

    public static boolean isVersionStringValid(String s) {
        Pattern patternOfPluginIdentifier = ~/([0-9]*[.][0-9]*[.][0-9]*([-][0-9]){0,}|[:]current)/
        return s ==~ patternOfPluginIdentifier;
    }

    /**
     * A helper method to identify whether a workflow identification string is valid, e.g.:
     *       "COWorkflows:1.0.1-0:current": false,
     *       "COWorkflows:1.0.1-0"        : true,
     *       "COWorkflows:1.0.1-3"        : true,
     *       "COWorkflows"                : true,
     *       "COWorkflows:current"        : true
     * @param s
     * @return
     */
    public static boolean isPluginIdentifierValid(String s) {
        //Pattern patternOfPluginIdentifier = ~/[a-zA-Z]*[:]{1,1}[0-9]*[.][0-9]*[.][0-9]*([-][0-9]){0,}|[a-zA-Z]*[:]current|[a-zA-Z]*/
        Pattern patternOfPluginIdentifier = ~/([a-zA-Z]*)([:]{1,1}[0-9]*[.][0-9]*[.][0-9]*([-][0-9]){0,}|[:]current|$)/
        return s ==~ patternOfPluginIdentifier;
    }

    /**
     * A helper method to identify whether a plugin directory name is valid, e.g.:
     *        "COWorkflows_1.0.1-0:current": false,
     *        "COWorkflows:1.0.1-r"        : false,
     *        "COWorkflows:1.0.1-3"        : false,
     *        "COWorkflows_1.0.1-3"        : true,
     *        "COWorkflows"                : true,
     *        "COWorkflows_current"        : false
     * @param s
     * @return
     */
    public static boolean isPluginDirectoryNameValid(String s) {
        //Pattern patternOfPluginIdentifier = ~/[a-zA-Z]*[_]{1,1}[0-9]*[.][0-9]*[.][0-9]*[-][0-9]{1,}|[a-zA-Z]*[_]{1,1}[0-9]*[.][0-9]*[.][0-9]*|[a-zA-Z]*/
        Pattern patternOfPluginIdentifier = ~/([a-zA-Z]*)([_]{1,1}[0-9]*[.][0-9]*[.][0-9]*[-][0-9]{1,}|[_]{1,1}[0-9]*[.][0-9]*[.][0-9]*|$)/
        return s ==~ patternOfPluginIdentifier;
    }

    @Override
    public boolean initialize() {
    }

    @Override
    public void destroy() {
    }
}
