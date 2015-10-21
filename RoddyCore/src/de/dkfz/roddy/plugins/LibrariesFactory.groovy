package de.dkfz.roddy.plugins

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.execution.io.ExecutionHelper
import de.dkfz.roddy.tools.*
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.Initializable

/**
 * Factory to load and integrate plugins.
 */
@groovy.transform.CompileStatic
public class LibrariesFactory extends Initializable {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(LibrariesFactory.class.getName());

    private static LibrariesFactory librariesFactory;

    public static URLClassLoader urlClassLoader
    public static GroovyClassLoader centralGroovyClassLoader;

    public static final String PLUGIN_VERSION_CURRENT = "current";
    public static final String PLUGIN_BASEPLUGIN = "PluginBase";
    public static final String BUILDINFO_DEPENDENCY = "dependson";
    public static final String BUILDINFO_EXTENSIONS = "extends";
    public static final String BUILDINFO_REVISION = "revises";
    public static final String BUILDINFO_TEXTFILE = "buildinfo.txt";

    private List<String> loadedLibrariesInfo = [];

    private List<PluginInfo> loadedPlugins = [];

    private Map<PluginInfo, File> loadedJarsByPlugin = [:]

    private Map<String, Map<String, PluginInfo>> mapOfPlugins = [:];

    private boolean librariesAreLoaded = false;

    private SyntheticPluginInfo synthetic

    private static class SyntheticPluginInfo extends PluginInfo {

        SyntheticPluginInfo(String name, File zipFile, File directory, File developmentDirectory, String prodVersion, Map<String, String> dependencies) {
            super(name, zipFile, directory, developmentDirectory, prodVersion, dependencies)
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

    public static GroovyClassLoader getGroovyClassLoader() {
        if (centralGroovyClassLoader == null) {
            centralGroovyClassLoader = new GroovyClassLoader(ClassLoader.getSystemClassLoader())
            urlClassLoader = centralGroovyClassLoader;
        }
        return centralGroovyClassLoader;
    }

    private Map<PluginInfo, List<String>> classListCacheByPlugin = [:];

    public Class searchForClass(String name) {
        if (name.contains(".")) {
            return getGroovyClassLoader().loadClass(name);
        } else {
            //Search synthetic classes first.
            if (getSynthetic().map.containsKey(name))
                return getSynthetic().map[name];

            // SEVERE TODO This is a very quick hack and heavily depends on the existens of jar on the system!
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
                return getGroovyClassLoader().loadClass(listOfClasses[0]);
            }
            logger.severe("No class found for ${name}")
            return null;
        }
    }
/**
 * Resolve all used / necessary plugins and also look for miscrepancies.
 * @param usedPlugins
 */
    public boolean resolveAndLoadPlugins(String[] usedPlugins) {
        Map<String, PluginInfo> pluginsToActivate = [:];
        loadMapOfAvailablePlugins();

        Map<String, String> pluginsToCheck = usedPlugins.collectEntries { String requestedPlugin ->
            String[] pSplit = requestedPlugin.split(StringConstants.SPLIT_COLON);
            String id = pSplit[0];
            String version = "current";
            if (pSplit.length >= 2)
                version = pSplit[1];
            [id, version];
        }

        while (pluginsToCheck.size() > 0) {

            String id = pluginsToCheck.keySet()[0]
            String version = pluginsToCheck[id];
            pluginsToCheck.remove(id, version);

            if (!mapOfPlugins[id] || !mapOfPlugins[id][version]) {
                logger.severe("The plugin ${id}:${version} could not be found, are the plugin paths properly set?");
                return false;
            }

            PluginInfo pInfo = mapOfPlugins[id][version];
            if (pInfo == null)
                pInfo = mapOfPlugins[id][PLUGIN_VERSION_CURRENT]
            if (pInfo == null)
                continue;
            if (pluginsToActivate[id] != null) {
                if (pluginsToActivate[id].prodVersion != version) {
                    logger.severe("There is a version mismatch for plugin dependencies! Not starting up.");
                    return false;
                } else {
                    //Not checking again!
                }
            } else {
                pluginsToCheck.putAll(pInfo.getDependencies());
                pluginsToActivate[id] = pInfo;
            }
        }
        loadedPlugins = pluginsToActivate.values() as List;
        loadLibraries(loadedPlugins);
        librariesAreLoaded = true;
        return true;
    }

    public boolean areLibrariesLoaded() {
        return librariesAreLoaded
    }

    public List<PluginInfo> getLoadedPlugins() {
        return loadedPlugins;
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
    public Map<String, Map<String, PluginInfo>> loadMapOfAvailablePlugins() {
        if (mapOfPlugins.size() > 0) {
            return mapOfPlugins;
        }

        //Search all plugin folders and also try to join those if possible.
        List<Tuple2<File, String[]>> collectedPluginDirectories = [];
        List<File> pluginDirectories = Roddy.getPluginDirectories();
        def blacklist = [".idea", "out", "Template", ".svn"]

        for (File pBaseDirectory : pluginDirectories) {
            File[] directoryList = pBaseDirectory.listFiles().sort() as File[];
            for (File pEntry in directoryList) {
                String dirName = pEntry.getName();
                boolean isZip = dirName.endsWith(".zip");
                if (isZip)
                    dirName = dirName[0..-5]; // Remove .zip from the end.
                String[] splitName = dirName.split(StringConstants.SPLIT_UNDERSCORE); //First split for .zip then for the version
                String pluginName = splitName[0];
                if ((!pEntry.isDirectory() && !isZip) || !pluginName || blacklist.contains(pluginName))
                    continue;
                collectedPluginDirectories << new Tuple2<File, String[]>(pEntry, splitName);
            }
        }

        mapOfPlugins = loadPluginsFromDirectories(collectedPluginDirectories)
        return mapOfPlugins
    }

    /**
     * Loads all available plugins (including revisions and versions) from a set of directories.
     * @param collectedPluginDirectories
     * @return
     */
    public static Map<String, Map<String, PluginInfo>> loadPluginsFromDirectories(List<Tuple2<File, String[]>> collectedPluginDirectories) {
        Map<String, Map<String, PluginInfo>> _mapOfPlugins = [:];
        for (Tuple2<File, String[]> _entry : collectedPluginDirectories) {
            File pEntry = _entry.x;
            String[] splitName = _entry.y;//pEntry.getName().split(StringConstants.SPLIT_UNDERSCORE); //First split for .zip then for the version

            String pluginName = splitName[0];
            String[] pluginVersionInfo = splitName.length > 1 ? splitName[1].split(StringConstants.SPLIT_MINUS) : [PLUGIN_VERSION_CURRENT];
            String pluginVersion = pluginVersionInfo[0];
            String pluginRevision = pluginVersionInfo.length > 1 ? pluginVersionInfo[1] : "0";

            boolean helpMode = Roddy.getCommandLineCall().startupMode == RoddyStartupModes.help

            File develEntry = null;
            File prodEntry = null;
            File zipFile = null;

            if (pEntry.getName().endsWith(".zip")) { // Zip files are handled differently and cannot be checked for contents!
                zipFile = pEntry;
                if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.UnzipZippedPlugins)) {
                    if (!new File(zipFile.getAbsolutePath()[0..-5]).exists()) {
                        if (!helpMode) logger.postAlwaysInfo("Unzipping zipped plugin (this is done unchecked and unlocked, processes could interfere with each other!)")
                        (new RoddyIOHelperMethods.NativeLinuxZipCompressor()).decompress(zipFile, null, zipFile.getParentFile());
                    }
                }
                continue;
            }

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

            if (!prodEntry && !develEntry) { //Now we might have a plugin without a jar file. This is allowed to happen since 2.2.87
                prodEntry = pEntry;
            }

            //Get dependency list from plugin
            Map<String, String> pluginDependencies = [:];
            File buildinfoFile = pEntry.listFiles().find { File f -> f.name == BUILDINFO_TEXTFILE };
            if (buildinfoFile) {
                for (String line in buildinfoFile.readLines()) {
                    if (line.startsWith(BUILDINFO_DEPENDENCY)) {
                        String[] split = line.split(StringConstants.SPLIT_EQUALS)[1].split(StringConstants.SPLIT_COLON);
                        String workflow = split[0];
                        String version = split.length > 1 ? split[1] : PLUGIN_VERSION_CURRENT;
                        pluginDependencies.put(workflow, version);
                    }
                }
                if (pluginName != "DefaultPlugin" && !pluginDependencies.containsKey(PLUGIN_BASEPLUGIN))
                    pluginDependencies.put(PLUGIN_BASEPLUGIN, PLUGIN_VERSION_CURRENT);
                if (pluginName != "DefaultPlugin" && !pluginDependencies.containsKey("DefaultPlugin"))
                    pluginDependencies.put("DefaultPlugin", PLUGIN_VERSION_CURRENT);
            }

            _mapOfPlugins.get(pluginName, [:])[pluginVersion] = new PluginInfo(pluginName, zipFile, prodEntry, develEntry, pluginVersion, pluginDependencies);
        }
        return _mapOfPlugins
    }

    /**
     * Get a list of all available plugins in their most recent version...
     * @return
     */
    public List<PluginInfo> getAvailablePluginVersion() {
        List<PluginInfo> mostCurrentPlugins = [];
        Map<String, Map<String, PluginInfo>> availablePlugins = loadMapOfAvailablePlugins();
        availablePlugins.each {
            String pluginID, Map<String, PluginInfo> versions ->
                if (versions.keySet().contains(PLUGIN_VERSION_CURRENT))
                    mostCurrentPlugins << versions[PLUGIN_VERSION_CURRENT];
                else
                    mostCurrentPlugins << versions[versions.keySet().last()]
        }

        return mostCurrentPlugins;
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

    public void loadLibraries(List<PluginInfo> pluginInfo) {
        pluginInfo.parallelStream().each { PluginInfo pi ->
            if (!pi.directory)
                return;
            File jarFile = pi.directory.listFiles().find { File f -> f.name.endsWith(".jar") };
            if (jarFile && !addFile(jarFile))
                return;

            def loadInfo = "The plugin ${pi.getName()} [ Version: ${pi.getProdVersion()} ] was loaded."
            logger.postAlwaysInfo(loadInfo)
            synchronized (loadedLibrariesInfo) {
                loadedLibrariesInfo << loadInfo.toString()
                loadedJarsByPlugin[pi] = jarFile;
            }
        }
    }

    public List<String> getLoadedLibrariesInfoList() {
        return loadedLibrariesInfo;
    }

    public Class tryLoadClass(String className) throws ClassNotFoundException {
        try {
            return loadClass(className);

        } catch (any) {
            logger.severe("Could not load class className");
            return null;
        }
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return getGroovyClassLoader().loadClass(className);
    }

    @Override
    public boolean initialize() {
    }

    @Override
    public void destroy() {
    }
}
