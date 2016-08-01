package de.dkfz.roddy.tools

import de.dkfz.roddy.Constants
import de.dkfz.roddy.RunMode
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import groovy.transform.CompileStatic;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class to cover RoddyIOHelperMethods.
 *
 * Created by heinold on 11.11.15.
 */
@CompileStatic
public class RoddyIOHelperMethodsTest {

    @Test
    public void testGetMD5OfText() {
        assert RoddyIOHelperMethods.getMD5OfText("ABCD") == "cb08ca4a7bb5f9683c19133a84872ca7";
    }

    @Test
    public void testGetMD5OfFile() {
        String md5
        File testFile
        try {
            File testBaseDir = MockupExecutionContextBuilder.getDirectory(RoddyIOHelperMethodsTest.name, "testGetMD5OfFile");
            testFile = new File(testBaseDir, "A");
            testFile << "ABCD";
            md5 = RoddyIOHelperMethods.getMD5OfFile(testFile);
        } finally {
            testFile?.delete();
        }
        assert md5 == "cb08ca4a7bb5f9683c19133a84872ca7";
    }

    private File getTestBaseDir() {
        File testBaseDir = MockupExecutionContextBuilder.getDirectory(RoddyIOHelperMethodsTest.name, "testGetSingleMD5OfFilesInDirectory");
        testBaseDir
    }

    private List<String> getMD5OfFilesInDirectories(File testBaseDir, File md5TestDir, List<String> filenames) {
        String md5TestDirShort = (md5TestDir.absolutePath - testBaseDir.absolutePath)[1 .. -1];
        return filenames.collect {
            File f = new File(md5TestDir, it)
            f << it
            return RoddyIOHelperMethods.getMD5OfText("${md5TestDirShort}/${it}") + RoddyIOHelperMethods.getMD5OfFile(f)
        }
    }

    @Test
    public void testGetSingleMD5OfFilesInDifferentDirectories() {
        File testBaseDir = MockupExecutionContextBuilder.getDirectory(RoddyIOHelperMethodsTest.name, "testGetSingleMD5OfFilesInDirectory");
        File md5TestDir1 = new File(testBaseDir, "md5sumtest1");
        File md5TestDir2 = new File(md5TestDir1, "md5sumtest2");
        md5TestDir1.mkdirs();
        md5TestDir2.mkdirs();

        assert getMD5OfFilesInDirectories(testBaseDir, md5TestDir1, ["A", "B"]).join(Constants.ENV_LINESEPARATOR) != getMD5OfFilesInDirectories(testBaseDir, md5TestDir2, ["A", "B"]).join(Constants.ENV_LINESEPARATOR)
    }

    @Test
    public void testGetSingleMD5OfFilesInDirectory() {
        File testBaseDir = getTestBaseDir()
        File md5TestDir = new File(testBaseDir, "md5sumtest");
        File md5TestSubDir = new File(md5TestDir, "sub");
        md5TestSubDir.mkdirs();

        List<String> aList = getMD5OfFilesInDirectories(testBaseDir, md5TestDir, ["A", "B", "C", "D"])
        aList += getMD5OfFilesInDirectories(testBaseDir, md5TestSubDir, ["E", "F"]);

        String text = aList.join(Constants.ENV_LINESEPARATOR)
        assert RoddyIOHelperMethods.getSingleMD5OfFilesInDirectory(md5TestDir) == RoddyIOHelperMethods.getMD5OfText(text);
    }

    @Test
    public void testCopyDirectory() {
        File base = MockupExecutionContextBuilder.getDirectory(RoddyIOHelperMethodsTest.class.name, "copyDirectory")
        File src = new File(base, "src");
        File dst = new File(base, "dst");
        File dst2 = new File(dst, "dst")

        String nonexecutable = "nonexecutable"
        String executable = "executable"

        src.mkdirs();

        File ne = new File(src, nonexecutable)
        ne << "a"

        File ex = new File(src, executable)
        ex << "b"
        ex.setExecutable(true);

        assert !ne.canExecute()
        assert ex.canExecute();

        // To non existing directory with new name
        RoddyIOHelperMethods.copyDirectory(src, dst)
        assert dst.exists()
        assert !new File(dst, nonexecutable).canExecute();
        assert new File(dst, executable).canExecute()

        // To existing directory without new name
        RoddyIOHelperMethods.copyDirectory(src, dst2)
        assert dst2.exists()
        assert !new File(dst2, nonexecutable).canExecute();
        assert new File(dst2, executable).canExecute()
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
                "0000": "0777",
                "0007": "0770",
                "0067": "0710",
                "0002": "0775",
                "0602": "0175",
        ]

        valuesAndResults.each {
            String rights, String res ->
                assert res == RoddyIOHelperMethods.convertUMaskToAccessRights(rights);
        }
    }
}