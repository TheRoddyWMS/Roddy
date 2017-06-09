/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.cluster.sge;

import de.dkfz.roddy.config.ResourceSet;
import de.dkfz.roddy.config.ResourceSetSize;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSResourceProcessingCommand;
import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager;
import de.dkfz.roddy.config.*;
import de.dkfz.roddy.tools.BufferUnit;
import de.dkfz.roddy.tools.BufferValue;
import de.dkfz.roddy.tools.TimeUnit;
import org.junit.Test;

/**
 */
public class SGEJobManagerTest {

    @Test
    public void testConvertToolEntryToPBSCommandParameters() {
        ResourceSet rset1 = new ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, 1, new TimeUnit("h"), null, null, null);
        ResourceSet rset2 = new ResourceSet(ResourceSetSize.l, null, null, 1, new TimeUnit("h"), null, null, null);
        ResourceSet rset3 = new ResourceSet(ResourceSetSize.l, new BufferValue(1, BufferUnit.G), 2, null, null, null, null, null);

        Configuration cfg = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null));

        BatchEuphoriaJobManager cFactory = null; //new SGEJobManager(false);
        PBSResourceProcessingCommand test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset1);
        assert test.getProcessingString().trim().equals("-V -l s_data=1024M");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset2);
        assert test.getProcessingString().equals(" -V");

        test = (PBSResourceProcessingCommand) cFactory.convertResourceSet(rset3);
        assert test.getProcessingString().equals(" -V -l s_data=1024M");
    }


}
