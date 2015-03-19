package de.dkfz.roddy.plugins

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.tools.*
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.Initializable
import de.dkfz.roddy.core.LibraryEntry

import java.lang.reflect.Method

//import net.xeoh.plugins.base.PluginManager
//import net.xeoh.plugins.base.impl.PluginManagerFactory
/**
 * Factory to load and integrate plugins.
 */
@groovy.transform.CompileStatic
public class LibrariesFactory extends Initializable {
    public static final String PLUGIN_VERSION_CURRENT = "current";

    private static LoggerWrapper logger = LoggerWrapper.getLogger(LibrariesFactory.class.getName());

    private static LibrariesFactory librariesFactory;

    private List<LibraryEntry> loadedLibraries = [];

    private List<PluginInfo> pluginInfoList = [];

    private Map<String, Map<String, PluginInfo>> mapOfPlugins = [:];

    /**
     * This resets the singleton and is not thread safe!
     * @return
     */
    public static LibrariesFactory initializeFactory() {
        if (!librariesFactory)
            librariesFactory = new LibrariesFactory();
        return librariesFactory;
    }

    /**
     * Resolve all used / necessary plugins and also look for miscrepancies.
     * @param usedPlugins
     */
    public void resolveAndLoadPlugins(String[] usedPlugins) {
        Map<String, PluginInfo> pluginsToActivate = [:];
        loadMapOfAvailablePlugins();
        for (String requestedPlugin in usedPlugins) {
            String[] pSplit = requestedPlugin.split(StringConstants.SPLIT_COLON);
            String id = pSplit[0];
            String version = pSplit[1];

            PluginInfo pInfo = mapOfPlugins[id][version];
            if (pluginsToActivate[id] != null && pluginsToActivate[id].prodVersion != version) {
                throw new RuntimeException("There is a version mismatch for plugin dependencies! Not starting up.");
            }
            pluginsToActivate[id] = pInfo;
        }
        pluginInfoList = pluginsToActivate.values() as List;
        loadLibraries(pluginInfoList);
    }

    public List<PluginInfo> loadGenericPluginInfo() {
        return pluginInfoList;
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
    private Map<String, Map<String, PluginInfo>> loadMapOfAvailablePlugins() {
        if (mapOfPlugins.size() > 0) {
            return mapOfPlugins;
        }

        //Search all plugin folders and also try to join those if possible.
        List<File> pluginDirectories = Roddy.getPluginDirectories();
        def blacklist = ["PluginBase", ".idea", "out", "Template", ".svn"]

        for (File pBaseDirectory : pluginDirectories) {
            for (File pEntry : pBaseDirectory.listFiles().sort()) {
                String dirName = pEntry.getName();
                boolean isZip = dirName.endsWith(".zip");
                if (isZip)
                    dirName = dirName[0..-5]; // Remove .zip from the end.
                String[] splitName = dirName.split(StringConstants.SPLIT_UNDERSCORE); //First split for .zip then for the version
                String pluginName = splitName[0];
                if ((!pEntry.isDirectory() && !isZip) || !pluginName || blacklist.contains(pluginName))
                    continue;

                String pluginVersion = splitName.length > 1 ? splitName[1] : PLUGIN_VERSION_CURRENT;

                boolean helpMode = Roddy.getCommandLineCall().startupMode == RoddyStartupModes.help

                File develEntry = null;
                File prodEntry = null;
                File zipFile = null;

                if (pEntry.getName().endsWith(".zip")) { // Zip files are handled differently and cannot be checked for contents!
                    zipFile = pEntry;

                    if (!new File(zipFile.getAbsolutePath()[0..-5]).exists()) {
                        if (!helpMode) logger.postAlwaysInfo("Unzipping zipped plugin (this is done unchecked and unlocked, processes could interfere with each other!)")
                        (new RoddyIOHelperMethods.NativeLinuxZipCompressor()).decompress(zipFile, null, zipFile.getParentFile());
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

                //Get dependency list from plugin
                File buildinfoFile = pEntry.listFiles().find { File f -> f.name == "buildversion.txt"};
                Map<String, String> pluginDependencies = [:];
                buildinfoFile.eachLine {
                    String line ->
                        if(!line.startsWith("dependson"))
                            return;
                        String[] split = line.split(StringConstants.SPLIT_EQUALS)[1].split(StringConstants.SPLIT_COLON);
                        String workflow = split[0];
                        String version = split.length > 1 ? split[1] : "current";
                }

                mapOfPlugins.get(pluginName, [:])[pluginVersion] = new PluginInfo(pluginName, zipFile, prodEntry, develEntry, pluginVersion, pluginVersion);
            }
        }
        mapOfPlugins
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
            Class[] parameters = [URL.class];
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke((URLClassLoader) ClassLoader.getSystemClassLoader(), u);
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
        for (PluginInfo pi : pluginInfo) {
            if (!pi.directory)
                continue;
            File jarFile = pi.directory.listFiles().find { File f -> f.name.endsWith(".jar") };
            if (!jarFile || !addFile(jarFile))
                continue;
            logger.postAlwaysInfo("The plugin ${pi.getName()} was loaded.")
        }
    }

    public List<LibraryEntry> getAllLibraries() {
        return new LinkedList<>(loadedLibraries);
    }

    public Class tryLoadClass(String className) throws ClassNotFoundException {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);

        } catch (any) {
            logger.severe("Could not load class className");
            return null;
        }
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return ClassLoader.getSystemClassLoader().loadClass(className);
    }

    @Override
    public boolean initialize() {
    }

    @Override
    public void destroy() {
    }
}
