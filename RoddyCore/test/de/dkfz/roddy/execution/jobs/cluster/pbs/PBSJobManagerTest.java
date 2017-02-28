/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.cluster.pbs;

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet;
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize;
import de.dkfz.eilslabs.batcheuphoria.execution.cluster.pbs.PBSJobManager;
import de.dkfz.eilslabs.batcheuphoria.execution.cluster.pbs.PBSResourceProcessingCommand;
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManager;
import de.dkfz.roddy.config.*;
//import de.dkfz.eilslabs.batcheuphoria.jobs.JobManager;
import de.dkfz.roddy.execution.io.NoNoExecutionService;
import de.dkfz.roddy.tools.BufferUnit;
import de.dkfz.roddy.tools.BufferValue;
import de.dkfz.roddy.tools.TimeUnit;
import org.junit.Test;

/**
 */
public class PBSJobManagerTest {

    @Test
    public void testConvertToolEntryToPBSCommandParameters() {
        ResourceSet rset1 = new ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, 1, new TimeUnit("h"), null, null, null);
        ResourceSet rset2 = new ResourceSet(ResourceSetSize.l, null, null, 1, new TimeUnit("h"), null, null, null);
        ResourceSet rset3 = new ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, null, null, null, null, null);

        Configuration cfg = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null));

        JobManager cFactory = new PBSJobManager(new NoNoExecutionService());
        PBSResourceProcessingCommand test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset1);
        assert test.getProcessingString().trim().equals("-l mem=1024M -l nodes=1:ppn=2 -l walltime=00:01:00:00");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset2);
        assert test.getProcessingString().equals(" -l nodes=1:ppn=1 -l walltime=00:01:00:00");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset3);
        assert test.getProcessingString().equals(" -l mem=1024M -l nodes=1:ppn=2");
    }
}
