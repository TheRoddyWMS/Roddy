package de.dkfz.roddy.plugins

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.lang.reflect.Method

/**
 * The tests for the jar file loading are not very nice.
 * The core classes Roddy and LibrariesFactory are singletons and they need to be reset for each test.
 * Also it is necessary to synchronize some of the tests. Not nice, but working.
 */
@groovy.transform.CompileStatic
public class LibrariesFactoryTest {

//    private static Object testLock = new Object();

//    @Test
//    /**
//     * Test, if the plugin chain with "full" plugins is loading.
//     */
//    public void testLoadLibraryChainWithJarFile() {
//        try {
//            synchronized (testLock) {
//                Roddy.resetMainStarted();
//                Roddy.main(["testrun", "TestProjectForUnitTests@test", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories=" + RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);
//                assert (LibrariesFactory.getInstance().getLoadedLibrariesInfoList().size() == 3)
//            }
//        } finally {
//            LibrariesFactory.initializeFactory(true);
//        }
//    }
//
//    @Test
//    /**
//     * Test, if the plugin chain with "mixed/empty" plugins is loading. Empty plugins are such, that have no jar file.
//     */
//    public void testLoadLibraryChainWithoutJarFile() {
//        try {
//            synchronized (testLock) {
//                Roddy.resetMainStarted();
//                Roddy.main(["testrun", "TestProjectForUnitTests@testWithoutJar", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories=" + RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);
//                assert (LibrariesFactory.getInstance().getLoadedLibrariesInfoList().size() == 4)
//            }
//        } finally {
//            LibrariesFactory.initializeFactory(true);
//        }
//    }

    /**
     * Test data map for plugin chain loading mechanims
     * The map contains various plugins, versions and dependencies to other plugins.
     *  x.y.z / current (version)
     *  -n (revision)
     *  ,b (beta)
     *  ,c (compatible)
     *  ,bc (beta and compatible)
     *  ,z (zip file (without e.g. buildinfo))
     */
    private static final LinkedHashMap<String, LinkedHashMap<String, List<String>>> mapWithTestPlugins = [
            A: ["1.0.1"  : [],
                "1.0.24" : ["PluginBase:current", "DefaultPlugin:current"],
                "current": ["PluginBase:current", "DefaultPlugin:current"]
            ],
            B: ["0.9.0"    : ["A:1.0.1"],
                "1.0.1"    : ["A:1.0.24"],
                "1.0.1-r"  : [],   // Is not valid and will be filtered
                "1.0.2"    : ["A:1.0.24"],
                "1.0.2-1"  : ["A:1.0.24"],
                "1.0.2-2,b": ["A:1.0.24"],
                "1.0.3,c"  : ["A:current"]
            ],
            C: ["0.9.0"    : ["B:0.9.0"],
                "1.0.0,z"  : [], // Also test, if zipped plugins can be recognized. There should come a warning and they will get filtered.
                "1.0.0"    : [], // Unzipped version of the prior zip file
                "1.0.1"    : ["B:1.0.1"],
                "1.0.2,c"  : ["B:1.0.2-1"],
                "1.0.3"    : ["B:1.0.3"],
                "current,c": ["B:1.0.3"]
            ],
            D: ["0.9.0"  : ["C:0.9.0"],
                "1.0.1"  : ["C:1.0.1"],
                "1.0.2"  : ["C:1.0.2", "B:1.0.1"],
                "1.0.2-1": ["C:1.0.2"],
                "1.0.3"  : ["C:1.0.3", "B:1.0.2"],
                "1.0.4,z": [],
                "current": ["C:current"]
            ],
    ]

    /**
     * Keeps all available plugins which were created with the help of the mapWithTestPlugins map
     */
    private static Map<String, Map<String, PluginInfo>> mapOfAvailablePlugins

    public static TemporaryFolder pluginsBaseDir = new TemporaryFolder();

    @AfterClass
    public static void tearDownClass() {
        pluginsBaseDir.delete();
    }

    @BeforeClass
    public static void setupTestDataForPluginQueueTests() {

        pluginsBaseDir.create();

        // Create the folder and file structure so that the loadPluginsFromDirectories method will work.
        for (String plugin : mapWithTestPlugins.keySet()) {
            Map<String, List<String>> pluginInfoMap = mapWithTestPlugins[plugin];
            List<String> listOfVersions = pluginInfoMap.keySet() as List<String>;
            for (int i = 0; i < listOfVersions.size(); i++) {
                String version = listOfVersions[i];
                List<String> vString = version.split(StringConstants.SPLIT_COMMA) as List<String>
                List<String> importList = pluginInfoMap[version];

                String folderName = plugin
                if (vString[0] != "current")
                    folderName += "_" + vString[0]

                if (vString[1]?.contains("z")) {
                    // Touch a zip file and continue
                    folderName += ".zip"
                    pluginsBaseDir.newFile(folderName);
                    continue
                }

                File pFolder = pluginsBaseDir.newFolder(folderName);
                File buildinfo = new File(pFolder, "buildinfo.txt");

                // Check, if there are additions to the versioning string.
                if (vString[1]?.contains("b"))
                    buildinfo << "status=beta\n"

                if (vString[1]?.contains("c") && i > 0)
                    buildinfo << "compatibleto=${listOfVersions[i - 1].split(StringConstants.SPLIT_COMMA)[0]}\n"

                importList.each {
                    buildinfo << "dependson=${it}\n"
                }
            }
        }

        //Add additional "native" plugins (DefaultPlugin, PluginBase) and the temporary plugin folder
        List<File> pluginDirectories = [
                pluginsBaseDir.root
        ]

        mapOfAvailablePlugins = callLoadMapOfAvailablePlugins(pluginDirectories)

        // Check, if all plugins were recognized and if the version count matches.
        assert mapOfAvailablePlugins.size() >= mapWithTestPlugins.size(); // Take the additional plugins into account
        assert mapOfAvailablePlugins["PluginBase"]["current-0"] == null && mapOfAvailablePlugins["PluginBase"]["current"] != null
        assert mapWithTestPlugins["A"].size() == mapOfAvailablePlugins["A"].size();
        assert mapWithTestPlugins["B"].size() == mapOfAvailablePlugins["B"].size() + 1; // Lacks one filtered entry.
        assert mapWithTestPlugins["C"].size() == mapOfAvailablePlugins["C"].size() + 1; // Lacks one filtered zip entry.
        assert mapWithTestPlugins["D"].size() == mapOfAvailablePlugins["D"].size() + 1; // Lacks one filtered zip entry.

        assert mapOfAvailablePlugins["B"]["1.0.2-1"]?.previousInChain == mapOfAvailablePlugins["B"]["1.0.2-0"] && mapOfAvailablePlugins["B"]["1.0.2-1"]?.previousInChainConnectionType == PluginInfo.PluginInfoConnection.REVISION
        assert mapOfAvailablePlugins["B"]["1.0.3-0"]?.previousInChain == mapOfAvailablePlugins["B"]["1.0.2-2"] && mapOfAvailablePlugins["B"]["1.0.3-0"]?.previousInChainConnectionType == PluginInfo.PluginInfoConnection.EXTENSION
    }

    public static Map<String, Map<String, PluginInfo>> callLoadMapOfAvailablePlugins(List<File> additionalPluginDirectories = []) {
        List<File> pluginDirectories = [new File(Roddy.getApplicationDirectory(), "/dist/plugins/")]
        pluginDirectories += additionalPluginDirectories

        // The method is static and private and should stay that way, so get it via reflection.
        Method loadMapOfAvailablePlugins = LibrariesFactory.getDeclaredMethod("loadMapOfAvailablePlugins", List.class);
        loadMapOfAvailablePlugins.setAccessible(true);

        // Invoke the method and check the results.
        mapOfAvailablePlugins = loadMapOfAvailablePlugins.invoke(null, pluginDirectories) as Map<String, Map<String, PluginInfo>>;
        return mapOfAvailablePlugins
    }

    @Test
    public void testBuildupValidPluginQueue() {
        // Now test several plugin chains for consistency and expected results.
        // Validate if all to-be-loaded versions are properly found.
        Map<String, PluginInfo> pluginQueueOK = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:1.0.2-1"] as String[]);
        assert pluginQueueOK != null;
        assert pluginQueueOK["D"].prodVersion == "1.0.2-1" &&
                pluginQueueOK["C"].prodVersion == "1.0.2-0" &&
                pluginQueueOK["B"].prodVersion == "1.0.3-0" &&
                pluginQueueOK["A"].prodVersion == "current" &&
                pluginQueueOK["PluginBase"].prodVersion == "current" &&
                pluginQueueOK["DefaultPlugin"].prodVersion == "current";

    }

    @Test
    public void testBuildupInvalidPluginQueue() {
        Map<String, PluginInfo> pluginQueueFaulty = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:1.0.2"] as String[]);
        assert pluginQueueFaulty == null;

    }

    @Test
    public void testBuildupPluginQueueContainingCompatibleEntries() {
        Map<String, PluginInfo> pluginQueueCompatible = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:1.0.3"] as String[]);
        assert pluginQueueCompatible != null;
        assert pluginQueueCompatible["D"].prodVersion == "1.0.3-0" &&
                pluginQueueCompatible["C"].prodVersion == "current" &&
                pluginQueueCompatible["B"].prodVersion == "1.0.3-0" &&
                pluginQueueCompatible["A"].prodVersion == "current" &&
                pluginQueueCompatible["PluginBase"].prodVersion == "current" &&
                pluginQueueCompatible["DefaultPlugin"].prodVersion == "current";

    }

    @Test
    public void testBuildupInvalidPluginQueueWithFixatedEntries() {
        Map<String, PluginInfo> pluginQueueFixatedEntriesFaulty = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:1.0.3", "C:1.0.3", "B:1.0.2-2"] as String[]);
        assert pluginQueueFixatedEntriesFaulty == null;

    }

    @Test
    public void testBuildupValidPluginQueueWithFixatedEntries() {
        Map<String, PluginInfo> pluginQueueFixatedEntriesOK = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:1.0.2-1", "C:1.0.2", "B:1.0.2-1"] as String[]);
        assert pluginQueueFixatedEntriesOK != null;
        assert pluginQueueFixatedEntriesOK["D"].prodVersion == "1.0.2-1" &&
                pluginQueueFixatedEntriesOK["C"].prodVersion == "1.0.2-0" &&
                pluginQueueFixatedEntriesOK["B"].prodVersion == "1.0.2-1" &&
                pluginQueueFixatedEntriesOK["A"].prodVersion == "1.0.24-0" &&
                pluginQueueFixatedEntriesOK["PluginBase"].prodVersion == "current" &&
                pluginQueueFixatedEntriesOK["DefaultPlugin"].prodVersion == "current";

    }

    @Test
    public void testBuildupValidPluginQueueWithMissingDefaultLibraryEntries() {
        Map<String, PluginInfo> pluginQueueWODefaultLibs = LibrariesFactory.buildupPluginQueue(mapOfAvailablePlugins, ["D:0.9.0"] as String[]);
        assert pluginQueueWODefaultLibs != null;
        assert pluginQueueWODefaultLibs["PluginBase"].prodVersion == "current" &&
                pluginQueueWODefaultLibs["DefaultPlugin"].prodVersion == "current";
    }

}
