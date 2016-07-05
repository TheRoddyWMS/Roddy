package de.dkfz.roddy.tools;

import de.dkfz.roddy.Roddy;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * This test class does runtime environment specific tests. It is not very well written and uses some hardcoded elements.
 * However, the dependency to the runtime system makes it necessary to somehow hardcode things :(... Don't currently know, how to make it better.
 * Created by heinold on 20.06.16.
 */
public class RuntimeToolsTest {
    @Test
    public void getRoddyRuntimeVersion() throws Exception {
        assert RuntimeTools.getRoddyRuntimeVersion().equals("2.3");
    }

    @Test
    public void getJavaRuntimeVersion() throws Exception {
        assert RuntimeTools.getJavaRuntimeVersion().equals("1.8");
    }

    @Test
    public void getGroovyRuntimeVersion() throws Exception {
        assert RuntimeTools.getGroovyRuntimeVersion().equals("2.4");
    }

    @Test
    public void testGetBuildinfoFile() {
        assert RuntimeTools.getBuildinfoFile().getAbsolutePath().equals(RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "buildinfo.txt").getAbsolutePath());
    }

    @Test
    public void testGetCurrentDistFolder() {
        assert RuntimeTools.getCurrentDistFolder().getAbsolutePath().equals(RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current").getAbsolutePath());
    }

    @Test
    public void testGetGroovyLibrary() {
        File gPath = RuntimeTools.getGroovyLibrary();
        File aPath = RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "lib", "groovy-all-2.4.5-indy.jar");
        assert gPath.getAbsolutePath().equals(aPath.getAbsolutePath());
    }
}