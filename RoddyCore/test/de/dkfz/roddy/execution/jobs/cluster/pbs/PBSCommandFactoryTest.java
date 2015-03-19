package de.dkfz.roddy.execution.jobs.cluster.pbs;

import de.dkfz.roddy.execution.jobs.ProcessingCommands;
import org.junit.Test;

import java.io.File;

/**
 */
public class PBSCommandFactoryTest {
    @Test
    public void testExtractProcessingCommandsFromToolScript() {
        File f = new File("./analysisTools/qcPipeline/bwaAlignSequence.sh");
        PBSCommandFactory fac = new PBSCommandFactory();
        ProcessingCommands processingCommands = fac.extractProcessingCommandsFromToolScript(f);
        System.out.println(processingCommands);
    }
}
