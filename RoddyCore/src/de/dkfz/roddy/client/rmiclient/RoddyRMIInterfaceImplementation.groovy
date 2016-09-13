/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ProjectFactory
import de.dkfz.roddy.execution.jobs.JobState
import groovy.transform.CompileStatic

import javax.management.remote.rmi.RMIServer
import java.rmi.RemoteException

/**
 * The RMI Server implementation for the Roddy RMI run mode
 * Created by heinold on 07.09.16.
 */
@CompileStatic
public class RoddyRMIInterfaceImplementation implements RoddyRMIInterface {

    public static class DataSetInfoObject implements Serializable {
        String id;
        String project;
        File path;

        DataSetInfoObject(DataSet ds) {
            this.id = ds.id
            this.project = ds.getProject().getName()
            this.path = ds.getOutputBaseFolder()
        }
    }

    @Override
    boolean ping(boolean keepalive) throws RemoteException {
        if(keepalive)
            RoddyRMIServer.touchServer();
        return RoddyRMIServer.isActive();
    }

    @Override
    void close() {
        RoddyRMIServer.stopServer();
    }

    @Override
    List<DataSetInfoObject> listdatasets(String analysisId) {
        RoddyRMIServer.touchServer();
        Analysis analysis = ProjectFactory.getInstance().loadAnalysis(Roddy.getCommandLineCall().arguments[1]);
        if (!analysis) return [];

        def collect = analysis.getListOfDataSets().collect { DataSet it -> new DataSetInfoObject(it) }
        RoddyRMIServer.touchServer();
        return collect
    }

    @Override
    JobState queryDataSetState(String dataSetId, String analysisId) throws RemoteException {
        RoddyRMIServer.touchServer();

        RoddyRMIServer.touchServer();
        return null
    }

    @Override
    void run() throws RemoteException {
        RoddyRMIServer.touchServer();

        RoddyRMIServer.touchServer();
    }

    @Override
    void testrun() {
        RoddyRMIServer.touchServer();

        RoddyRMIServer.touchServer();
    }

    @Override
    void rerun() throws RemoteException {
        RoddyRMIServer.touchServer();

        RoddyRMIServer.touchServer();
    }

    @Override
    void testrerun() throws RemoteException {
        RoddyRMIServer.touchServer();

        RoddyRMIServer.touchServer();
    }

}
