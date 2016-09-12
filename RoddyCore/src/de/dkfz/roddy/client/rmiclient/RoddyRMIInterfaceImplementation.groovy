/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.DataSet
import groovy.transform.CompileStatic

import java.rmi.RemoteException

/**
 * The RMI Server implementation for the Roddy RMI run mode
 * Created by heinold on 07.09.16.
 */
@CompileStatic
public class RoddyRMIInterfaceImplementation implements RoddyRMIInterface {

    @Override
    boolean ping() throws RemoteException {
        return true;
    }

    @Override
    void close() {
        RoddyRMIServer.rmiActiveSemaphore.release(); // Just release it, hope it works
    }

    @Override
    void testrun() {

    }

    @Override
    Map<String, String> listdatasets() {
        RoddyCLIClient.listDatasets(Roddy.getCommandLineCall().arguments.toArray(new String[0])).collectEntries {
            DataSet ds ->
                return [ds.getId(), ds.getOutputBaseFolder().getAbsolutePath()]
        }
    }
}
