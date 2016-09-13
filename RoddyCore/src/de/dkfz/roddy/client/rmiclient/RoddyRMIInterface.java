/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient;

import de.dkfz.roddy.execution.jobs.JobState;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * The stub for Roddy's RMI interface.
 * <p>
 * Don't convert to Groovy. This might interfere with RMI... Or test it.
 * <p>
 * Created by heinold on 07.09.16.
 */

public interface RoddyRMIInterface extends Remote {

    boolean ping(boolean keepalive) throws RemoteException;

    /**
     * Close the service
     */
    void close() throws RemoteException;

    List<RoddyRMIInterfaceImplementation.DataSetInfoObject> listdatasets(String analysisId) throws RemoteException;

    JobState queryDataSetState(String dataSetId, String analysisId) throws RemoteException;

    void run() throws RemoteException;

    void testrun() throws RemoteException;

    void rerun() throws RemoteException;

    void testrerun() throws RemoteException;
}
