package de.dkfz.roddy.execution.jobs.cluster.sge;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.InformationalConfigurationContent;
import de.dkfz.roddy.config.ResourceSetSize;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.execution.jobs.CommandFactory;
import de.dkfz.roddy.execution.jobs.ProcessingCommands;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSResourceProcessingCommand;
import org.junit.Test;

import java.io.File;

/**
 */
public class SGECommandFactoryTest {

    @Test
    public void testConvertToolEntryToPBSCommandParameters() {
        ToolEntry.ResourceSet rset1 = new ToolEntry.ResourceSet(ResourceSetSize.l, 1024.0f, 2, 1, 1, null, null, null);
        ToolEntry.ResourceSet rset2 = new ToolEntry.ResourceSet(ResourceSetSize.l, null, null, 1, 1, null, null, null);
        ToolEntry.ResourceSet rset3 = new ToolEntry.ResourceSet(ResourceSetSize.l, 1024.0f, 2, null, null, null, null, null);

        Configuration cfg = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null));

        CommandFactory cFactory = new SGECommandFactory(false);
        PBSResourceProcessingCommand test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset1);
        assert test.getProcessingString().equals(" -V -l s_data=1024M -l nodes=1:ppn=2 -l walltime=01:00");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset2);
        assert test.getProcessingString().equals(" -V -l nodes=1:ppn=1 -l walltime=01:00");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(cfg, rset3);
        assert test.getProcessingString().equals(" -V -l mem=1024M -l nodes=1:ppn=2");

    }

}
