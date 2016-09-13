/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient;

import de.dkfz.roddy.Roddy
import groovy.transform.CompileStatic;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class with integration tests for the RMI interface.
 * <p>
 * Created by heinold on 12.09.16.
 */
@CompileStatic
public class RoddyRMIInterfaceImplementationTest {


    @Test
    public void listdatasets() throws Exception {

        // Don't really know how to test this. The semaphore is handy but blocks things in an unfortunate way...
        // also don't want to introduce more threads just for testing.
        // One possibility would be to introduce a further thread which constantly queries the server semaphore until a job is waiting (the server), then start the tests.
        Thread.start {
            Roddy.main("rmi coWorkflowsTestProject@genome 66000 --useconfig=/data/michael/.roddy/applicationProperties.ini".split(" "));
        }
        while (!RoddyRMIServer.isActive()) {
            Thread.sleep(125);
        }
        RoddyRMIInterfaceImplementation iface = new RoddyRMIInterfaceImplementation();

        def listdatasets = iface.listdatasets("genome");

        assert listdatasets != null;
        assert listdatasets.size() > 0;

        // Try a second time, this failed in the past.
        listdatasets = iface.listdatasets("genome");

        assert listdatasets != null;
        assert listdatasets.size() > 0;
    }

}