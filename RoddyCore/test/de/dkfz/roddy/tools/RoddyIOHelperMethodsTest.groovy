package de.dkfz.roddy.tools

import de.dkfz.roddy.RunMode
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
public class RoddyIOHelperMethodsTest {

    @Test
    public void testSymbolicToNumericAccessRights() throws Exception {
        FileSystemAccessProvider.resetFileSystemAccessProvider(new FileSystemAccessProvider() {
            @Override
            int getDefaultUserMask() {
                return 0022; // Mock this value to a default value. This might otherwise change from system to system.    rwx,r,r
            }
        });

        Map<String, String> valuesAndExpectedMap = [
                "u=rwx,g=rwx,o=rwx": "0000", //rwx,rwx,rwx
                "u=rwx,g=rwx,o-rwx": "0007", //rwx,rwx,---
                "u+rwx,g+rwx,o-rwx": "0007", //rwx,rwx,---
                "u+rw,g-rw,o-rwx"  : "0067", //rwx,---,---
                "u+rw,g+rw,o-rwx"  : "0007", //rwx,rw-,---
                "u+rw,g+rw"        : "0002", //rwx,rw-,r--
                "u-w,g+rw,u-r"     : "0602", //rwx,rw-,r--
        ]

        valuesAndExpectedMap.each {
            String rights, String res ->
                assert res == RoddyIOHelperMethods.symbolicToNumericAccessRights(rights);
        }
    }

//    @Test
//    public void testNumericToHashAccessRights() throws Exception {
//        assert [u:RoddyIOHelperMethods.numericToHashAccessRights(0777);
//    }
}