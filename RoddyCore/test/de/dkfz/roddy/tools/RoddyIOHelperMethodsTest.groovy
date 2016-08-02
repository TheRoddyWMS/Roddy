package de.dkfz.roddy.tools

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class to cover RoddyIOHelperMethods.
 *
 * Created by heinold on 11.11.15.
 */
@groovy.transform.CompileStatic
public class RoddyIOHelperMethodsTest {

    @Test
    public void copyDirectory() {
        File base = MockupExecutionContextBuilder.getDirectory(RoddyIOHelperMethodsTest.class.name, "copyDirectory")
        File src = new File(base, "src");
        File dst = new File(base, "dst");
        File dst2 = new File(dst, "dst")

        String nonExecutableButWritable = "nonExecutableButWritable"
        String executableButNotWritable = "executableButNotWritable"

        src.mkdirs();

        File nebw = new File(src, nonExecutableButWritable)
        nebw << "a"
        nebw.setExecutable(false)
        nebw.setWritable(true)

        File exbnw = new File(src, executableButNotWritable)
        exbnw << "b"
        exbnw.setExecutable(true);
        exbnw.setWritable(false)

        assert !nebw.canExecute()
        assert nebw.canWrite()


        assert exbnw.canExecute()
        assert !exbnw.canWrite()


        // To non existing directory with new name
        RoddyIOHelperMethods.copyDirectory(src ,dst)
        assert dst.exists()
        File nebw2 = new File(dst, nonExecutableButWritable)
        assert !nebw2.canExecute()
        assert nebw2.canWrite()

        File exbnw2 = new File(dst, executableButNotWritable)
        assert exbnw2.canExecute()
        assert !exbnw2.canWrite()

        // To existing directory without new name
        RoddyIOHelperMethods.copyDirectory(src, dst2)
        assert dst2.exists()
        File nebw3 = new File(dst2, nonExecutableButWritable)
        assert !nebw3.canExecute()
        assert nebw3.canWrite()

        File exbnw3 = new File(dst2, executableButNotWritable)
        assert exbnw3.canExecute()
        assert !exbnw3.canWrite()

    }

    @Test
    public void testSymbolicToNumericAccessRights() throws Exception {
        FileSystemAccessProvider.resetFileSystemAccessProvider(new FileSystemAccessProvider() {
            @Override
            int getDefaultUserMask() {
                return 0022; // Mock this value to a default value. This might otherwise change from system to system.    rwx,r,r
            }
        });

        Map<String, String> valuesAndExpectedMap = [
                "u=rwx,g=rwx,o=rwx": "0777", //rwx,rwx,rwx
                "u=rwx,g=rwx,o-rwx": "0770", //rwx,rwx,---
                "u+rwx,g+rwx,o-rwx": "0770", //rwx,rwx,---
                "u+rw,g-rw,o-rwx"  : "0710", //rwx,---,---
                "u+rw,g+rw,o-rwx"  : "0770", //rwx,rw-,---
                "u+rw,g+rw"        : "0775", //rwx,rw-,r--
                "u-w,g+rw,u-r"     : "0175", //--x,rwx,r-x  Careful here, u ist set two times!
        ]

        valuesAndExpectedMap.each {
            String rights, String res ->
                assert res == RoddyIOHelperMethods.symbolicToNumericAccessRights(rights);
        }
    }

    @Test
    public void testConvertUMaskToAccessRights() throws Exception {
        Map<String, String> valuesAndResults = [
                "0000" : "0777",
                "0007" : "0770",
                "0067" : "0710",
                "0002" : "0775",
                "0602" : "0175",
        ]

        valuesAndResults.each {
            String rights, String res ->
                assert res == RoddyIOHelperMethods.convertUMaskToAccessRights(rights);
        }
    }
}