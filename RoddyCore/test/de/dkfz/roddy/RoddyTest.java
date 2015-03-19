package de.dkfz.roddy;

import de.dkfz.roddy.client.cliclient.RoddyCLIClient;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
import de.dkfz.roddy.execution.jobs.CommandFactory;
import de.dkfz.roddy.tools.LoggerWrapper;
import org.junit.Test;

/**
 */
public class RoddyTest {
    @Test
    public void testInitializeRoddy() {
        LoggerWrapper.setup();

//        Roddy.createInterruptSignalHandler();

        RoddyCLIClient.CommandLineCall clc = new RoddyCLIClient.CommandLineCall(new String[0]);

        Roddy.performInitialSetup(new String[0], clc.startupMode);

        Roddy.parseAdditionalStartupOptions(clc);

        Roddy.loadPropertiesFile();

        Roddy.initializeServices(true);

        assert FileSystemInfoProvider.getInstance() != null;
        assert CommandFactory.getInstance() != null;
        assert ExecutionService.getInstance() != null;
    }
    
}
