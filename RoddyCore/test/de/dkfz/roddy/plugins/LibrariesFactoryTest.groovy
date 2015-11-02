package de.dkfz.roddy.plugins

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.execution.io.ExecutionHelper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.Tuple2
import org.junit.After
import org.junit.Before
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

    private static Object testLock = new Object();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    /**
     * Test, if the plugin chain with "full" plugins is loading.
     */
    public void testLoadLibraryChainWithJarFile() {
        try {
            synchronized (testLock) {
                Roddy.resetMainStarted();
                Roddy.main(["testrun", "TestProjectForUnitTests@test", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories=" + RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);
                assert (LibrariesFactory.getInstance().getLoadedLibrariesInfoList().size() == 3)
            }
        } finally {
            LibrariesFactory.initializeFactory(true);
        }
    }

    @Test
    /**
     * Test, if the plugin chain with "mixed/empty" plugins is loading. Empty plugins are such, that have no jar file.
     */
    public void testLoadLibraryChainWithoutJarFile() {
        try {
            synchronized (testLock) {
                Roddy.resetMainStarted();
                Roddy.main(["testrun", "TestProjectForUnitTests@testWithoutJar", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories=" + RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);
                assert (LibrariesFactory.getInstance().getLoadedLibrariesInfoList().size() == 4)
            }
        } finally {
            LibrariesFactory.initializeFactory(true);
        }
    }

    @Rule
    public TemporaryFolder pluginsBaseDir = new TemporaryFolder();

    @Test
    public void testLoadPluginsFromDirectories() {
        // Build up test data
        // First, create a map of plugins and versions
        //  x.y.z / current (version)
        //  -n (revision)
        //  ,b (beta)
        //  ,c (compatible)
        //  ,bc (beta and compatible)
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> testPluginsList = [
                A: ["1.0.24" : ["PluginBase:1.0.24", "DefaultPlugin:1.0.28"],
                    "current": ["PluginBase:current", "DefaultPlugin:current"]
                ],
                B: ["1.0.1"    : ["A:1.0.24"],
                    "1.0.1-r"  : [],   // Is not valid and will be filtered
                    "1.0.2"    : ["A:1.0.24"],
                    "1.0.2-1"  : ["A:1.0.24"],
                    "1.0.2-2,b": ["A:1.0.24"],
                    "1.0.3,c"  : ["A:current"]
                ],
                C: ["1.0.1"    : ["B:1.0.1"],
                    "1.0.2,c"  : ["B:1.0.2-1"],
                    "1.0.3"    : ["B:1.0.3"],
                    "current,c": ["B:1.0.3"]
                ],
                D: ["1.0.1"  : ["C:1.0.1"],
                    "1.0.2"  : ["C:1.0.2", "B:1.0.1"],
                    "1.0.2-1": ["C:1.0.2"],
                    "1.0.3"  : ["C:1.0.3", "B:1.0.2"],
                    "current": ["C:current"]
                ],
        ]

        ArrayList<Tuple2<File, String[]>> collectedPluginDirectories = []

        // Create the folder and file structure so that the loadPluginsFromDirectories method will work.
        for (String plugin : testPluginsList.keySet()) {
            Map<String, List<String>> pluginInfoMap = testPluginsList[plugin];
            List<String> listOfVersions = pluginInfoMap.keySet() as List<String>;
            for (int i = 0; i < listOfVersions.size(); i++) {
                String version = listOfVersions[i];
                List<String> vString = version.split(StringConstants.SPLIT_COMMA) as List<String>
                List<String> importList = pluginInfoMap[version];

                String folderName = plugin
                if (vString[0] != "current")
                    folderName += "_" + vString[0]

                File pFolder = pluginsBaseDir.newFolder(folderName);
                collectedPluginDirectories << new Tuple2<File, String[]>(pFolder, pFolder.getName().split(StringConstants.SPLIT_UNDERSCORE));
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

        //Add additional "native" plugins (DefaultPlugin, PluginBase)
        List<File> baseplugins = [
                new File(Roddy.getApplicationDirectory(), "/dist/plugins/DefaultPlugin"),
                new File(Roddy.getApplicationDirectory(), "/dist/plugins/DefaultPlugin_1.0.29"),
                new File(Roddy.getApplicationDirectory(), "/dist/plugins/DefaultPlugin_1.0.28"),
                new File(Roddy.getApplicationDirectory(), "/dist/plugins/PluginBase"),
                new File(Roddy.getApplicationDirectory(), "/dist/plugins/PluginBase_1.0.24")
        ]
        collectedPluginDirectories.addAll(baseplugins.collect { File pdir ->
            return new Tuple2<File, String[]>(pdir, pdir.name.split(StringConstants.SPLIT_UNDERSCORE))
        });

        // The method is static and private and should stay that way, so get it via reflection.
        Method loadPluginsFromDirectories = LibrariesFactory.getDeclaredMethod("loadPluginsFromDirectories", List.class);
        loadPluginsFromDirectories.setAccessible(true);

        // Invoke the method and check the results.
        Map<String, Map<String, PluginInfo>> res = loadPluginsFromDirectories.invoke(null, collectedPluginDirectories) as Map<String, Map<String, PluginInfo>>;

        // Check, if all plugins were recognized and if the version count matches.
        assert res.size() == testPluginsList.size() + 2; // Take the additional plugins into account
        assert res["PluginBase"]["current-0"] == null && res["PluginBase"]["current"] != null
        assert testPluginsList["A"].size() == res["A"].size();
        assert testPluginsList["B"].size() == res["B"].size() + 1; // Lacks one filtered entry.
        assert testPluginsList["C"].size() == res["C"].size();
        assert testPluginsList["D"].size() == res["D"].size();

        assert res["B"]["1.0.2-1"]?.previousInChain == res["B"]["1.0.2-0"] && res["B"]["1.0.2-1"]?.previousInChainConnectionType == PluginInfo.PluginInfoConnection.REVISION
        assert res["B"]["1.0.3-0"]?.previousInChain == res["B"]["1.0.2-2"] && res["B"]["1.0.3-0"]?.previousInChainConnectionType == PluginInfo.PluginInfoConnection.EXTENSION

        // Now test several plugin chains for consistency and expected results.
        Map<String, PluginInfo> pluginQueueOK = LibrariesFactory.buildupPluginQueue(res, ["D:1.0.2-1"] as String[]);
        Map<String, PluginInfo> pluginQueueFaulty = LibrariesFactory.buildupPluginQueue(res, ["D:1.0.2"] as String[]);
        Map<String, PluginInfo> pluginQueueCompatible = LibrariesFactory.buildupPluginQueue(res, ["D:1.0.3"] as String[]);

        //Finally validate several plugin chains and look if all to-be-loaded versions are properly found.
        assert pluginQueueOK != null;
        assert pluginQueueOK["D"].prodVersion == "1.0.2-1" &&
                pluginQueueOK["C"].prodVersion == "1.0.2-0" &&
                pluginQueueOK["B"].prodVersion == "1.0.3-0" &&
                pluginQueueOK["A"].prodVersion == "current" &&
                pluginQueueOK["PluginBase"].prodVersion == "current" &&
                pluginQueueOK["DefaultPlugin"].prodVersion == "current";
        assert pluginQueueFaulty == null;
        assert pluginQueueCompatible != null;
        assert pluginQueueCompatible["D"].prodVersion == "1.0.3-0" &&
                pluginQueueCompatible["C"].prodVersion == "1.0.3-0" &&
                pluginQueueCompatible["B"].prodVersion == "1.0.3-0" &&
                pluginQueueCompatible["A"].prodVersion == "current" &&
                pluginQueueCompatible["PluginBase"].prodVersion == "current" &&
                pluginQueueCompatible["DefaultPlugin"].prodVersion == "current";

        println(res)

    }
}
