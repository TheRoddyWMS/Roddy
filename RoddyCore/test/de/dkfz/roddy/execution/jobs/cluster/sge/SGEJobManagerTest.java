package de.dkfz.roddy.execution.jobs.cluster.sge;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.InformationalConfigurationContent;
import de.dkfz.roddy.config.ResourceSetSize;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.execution.jobs.JobManager;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSResourceProcessingCommand;
import de.dkfz.roddy.tools.BufferUnit;
import de.dkfz.roddy.tools.BufferValue;
import de.dkfz.roddy.tools.TimeUnit;
import org.junit.Test;

/**
 */
public class SGEJobManagerTest {

    @Test
    public void testConvertToolEntryToPBSCommandParameters() {
        ToolEntry.ResourceSet rset1 = new ToolEntry.ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, 1, new TimeUnit("h"), null, null, null);
        ToolEntry.ResourceSet rset2 = new ToolEntry.ResourceSet(ResourceSetSize.l, null, null, 1, new TimeUnit("h"), null, null, null);
        ToolEntry.ResourceSet rset3 = new ToolEntry.ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, null, null, null, null, null);

        Configuration cfg = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null));

        JobManager cFactory = new SGEJobManager(false);
        PBSResourceProcessingCommand test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset1);
        assert test.getProcessingString().trim().equals("-V -l s_data=1024M");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset2);
        assert test.getProcessingString().equals(" -V");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset3);
        assert test.getProcessingString().equals(" -V -l s_data=1024M");
    }


}
