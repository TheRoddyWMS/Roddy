package de.dkfz.roddy.plugins

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import org.junit.After
import org.junit.Before
import org.junit.Test

@groovy.transform.CompileStatic
public class LibrariesFactoryTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLoadLibrariyChainWithoutJarFile() {
        Roddy.main(["listdatasets", "TestProjectForUnitTests@testWithoutJar", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories="+RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);

    }
}
