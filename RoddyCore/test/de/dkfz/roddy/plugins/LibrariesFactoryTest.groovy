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
        Map<String, List<String>> testPluginsList = [
                A: ["1.0.24", "current"],
                B: ["1.0.1", "1.0.2", "1.0.2-1", "1.0.2-2,bc", "1.0.3,c"],
                C: ["1.0.1", "1.0.2,c", "1.0.3", "current,c"],
                D: ["1.0.1", "1.0.2", "1.0.2-1", "1.0.3"],
        ]

        Map<String>

        ArrayList<Tuple2<File, String[]>> collectedPluginDirectories = []

        // Create the folder and file structure so that the loadPluginsFromDirectories method will work.
        for (String plugin : testPluginsList.keySet()) {
            List<String> listOfVersions = testPluginsList[plugin];
            for (int i = 0; i < listOfVersions.size(); i++) {
                String version = listOfVersions[i];
                String[] vString = version.split(StringConstants.SPLIT_COMMA)

                String folderName = plugin
                if (vString[0] != "current")
                    folderName += "_" + vString[0]

                File pFolder = pluginsBaseDir.newFolder(folderName);
                collectedPluginDirectories << new Tuple2<File, String[]>(pFolder, pFolder.getName().split(StringConstants.SPLIT_UNDERSCORE));
                File buildinfo = new File(pFolder, "buildinfo.txt");

                // Check, if there are additions to the versioning string.
                if (!(vString.size() > 1)) continue

                if (vString[1].contains("b"))
                    buildinfo << "status=beta\n"

                if (vString[1].contains("c") && i > 0)
                    buildinfo << "compatibleto=${listOfVersions[i - 1].split(StringConstants.SPLIT_COMMA)[0]}\n"
            }
        }

        // The method is static and private and should stay that way, so get it via reflection.
        Method loadPluginsFromDirectories = LibrariesFactory.getDeclaredMethod("loadPluginsFromDirectories", List.class);
        loadPluginsFromDirectories.setAccessible(true);

        // Invoke the method and check the results.
        Map<String, Map<String, PluginInfo>> res = loadPluginsFromDirectories.invoke(null, collectedPluginDirectories) as Map<String, Map<String, PluginInfo>>;

        // Check, if all plugins were recognized and if the version count matches.
        assert res.size() == testPluginsList.size()
        res.keySet().each {
            String pID ->
            assert testPluginsList[pID].size() == res[pID].size();
        }


        // Now test several plugin chains for consistency and expected results.


        println(res)

    }
}
