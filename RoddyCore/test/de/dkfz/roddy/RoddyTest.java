package de.dkfz.roddy;

import de.dkfz.roddy.client.cliclient.CommandLineCall;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.JobManager;
import de.dkfz.roddy.tools.LoggerWrapper;
import org.junit.Test;

import java.util.LinkedList;

/**
 */
public class RoddyTest {
    @Test
    public void testInitializeRoddy() {
        LoggerWrapper.setup();

//        Roddy.createInterruptSignalHandler();

        CommandLineCall clc = new CommandLineCall(new LinkedList<>());

        Roddy.performInitialSetup(new String[0], clc.startupMode);

        Roddy.parseAdditionalStartupOptions(clc);

        // TODO: Currently this needs an applicationProperties.ini in ~/.roddy/.
        Roddy.loadPropertiesFile();

        Roddy.initializeServices(true);

        assert FileSystemAccessProvider.getInstance() != null;
        assert JobManager.getInstance() != null;
        assert ExecutionService.getInstance() != null;
    }
    
}
