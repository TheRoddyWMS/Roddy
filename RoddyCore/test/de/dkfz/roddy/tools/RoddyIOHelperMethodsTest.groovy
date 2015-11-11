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
                return 0022; // Mock this value to a default value. This might otherwise change from system to system.
            }
        });

        Map<String, Integer> valuesAndExpectedMap = [
                "u=rwx,g=rwx,o=rwx": 0000,
                "u=rwx,g=rwx,o-rwx": 0007,
                "u+rwx,g+rwx,o-rwx": 0007,
                "u+rw,g-rw,o-rwx"  : 0077,
        ]

        valuesAndExpectedMap.each {
            String rights, Integer res ->
                assert res == RoddyIOHelperMethods.symbolicToNumericAccessRights(rights);
        }
    }

//    @Test
//    public void testNumericToHashAccessRights() throws Exception {
//        assert [u:RoddyIOHelperMethods.numericToHashAccessRights(0777);
//    }
}