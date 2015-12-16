package de.dkfz.roddy.execution.io.fs;

import de.dkfz.roddy.RunMode;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.LocalExecutionService;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by heinold on 10.11.15.
 */
public class FileSystemAccessProviderTest {

    public static final File outputDirectory = new File("/tmp/testproject");

    @BeforeClass
    public static void initializeTests() {
        ExecutionService.initializeService(LocalExecutionService.class, RunMode.CLI);
        outputDirectory.mkdir();
    }

    // TODO Think, how this test can be designed. The method via bash and checks on a group basis. The user is actually ignored...
//    @Test
//    public void testCheckIfAccessRightsCanBeSetToFalse() throws Exception {
//        ExecutionContext mockedContext = new ExecutionContext("testuser", null, null, ExecutionContextLevel.UNSET, outputDirectory, outputDirectory, null);
//
//        FileSystemAccessProvider p = new FileSystemAccessProvider();
//        assertFalse(p.checkIfAccessRightsCanBeSet(mockedContext));
//
//    }

    @Test
    public void testCheckIfAccessRightsCanBeSetToTrue() throws Exception {
        ExecutionContext mockedContext = new ExecutionContext(System.getProperty("user.name"), null, null, ExecutionContextLevel.UNSET, outputDirectory, outputDirectory, null);

        FileSystemAccessProvider p = new FileSystemAccessProvider();
        assertTrue(p.checkIfAccessRightsCanBeSet(mockedContext));

    }
}