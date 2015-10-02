package de.dkfz.roddy.plugins

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import org.junit.After
import org.junit.Before
import org.junit.Test

@groovy.transform.CompileStatic
/**
 * The tests for the jar file loading are not very nice.
 * The core classes Roddy and LibrariesFactory are singletons and they need to be reset for each test.
 * Also it is necessary to synchronize some of the tests. Not nice, but working.
 */
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
}
