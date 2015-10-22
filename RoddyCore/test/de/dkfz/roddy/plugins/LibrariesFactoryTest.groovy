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
        Map<String, List<String>> testPluginsList = [
                A: ["1.0.24", "current"],
                B: ["1.0.1", "1.0.2", "1.0.2-1", "1.0.2-2,bc", "1.0.3,c"],
                C: ["1.0.1", "1.0.2,c", "1.0.3", "current,c"],
                D: ["1.0.1", "1.0.2", "1.0.2-1", "1.0.3"],
        ]

        ArrayList<Tuple2<File, String[]>> collectedPluginDirectories = []

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
                if (vString.size() > 1) {
                    if (vString[1].contains("b"))
                        buildinfo << "status=beta\n"

                    if (vString[1].contains("c") && i > 0)
                        buildinfo << "compatibleto=${listOfVersions[i - 1].split(StringConstants.SPLIT_COMMA)[0]}\n"
                }
            }
        }


        Method loadPluginsFromDirectories = LibrariesFactory.getDeclaredMethod("loadPluginsFromDirectories", List.class);
        loadPluginsFromDirectories.setAccessible(true);

        def res = loadPluginsFromDirectories.invoke(null, collectedPluginDirectories);
        println(res)

    }
}
