package de.dkfz.roddy;

import de.dkfz.roddy.client.cliclient.CommandLineCall
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.JobManager;
import de.dkfz.roddy.tools.LoggerWrapper;
import org.junit.Test

import java.lang.reflect.Field;
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

    @Test
    public void testGetUsedResourceSize() {
        // Nothing in.
        assert Roddy.getUsedResourcesSize() == null

        // Wrongly set => null
        Field f = Roddy.class.getDeclaredField("commandLineCall")
        f.setAccessible(true);
        f.set(null, new CommandLineCall(["testrun", "a@b", "--usedresourcessize=xlff"]));

        assert Roddy.getUsedResourcesSize() == null

        // Right set
        f.set(null, new CommandLineCall(["testrun", "a@b", "--usedresourcessize=l"]));
        assert Roddy.getUsedResourcesSize() == ResourceSetSize.l;
    }
}
