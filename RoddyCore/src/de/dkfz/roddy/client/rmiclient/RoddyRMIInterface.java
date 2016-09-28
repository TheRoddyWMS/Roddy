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

    RoddyRMIInterfaceImplementation.ExtendedDataSetInfoObjectCollection queryExtendedDataSetInfo(String id, String analysis) throws RemoteException;

    JobState queryDataSetState(String dataSetId, String analysisId) throws RemoteException;

    boolean queryDataSetExecutability(String id, String analysis) throws RemoteException;

    List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> run(List<String> datasetIds, String analysisId) throws RemoteException;

    List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> testrun(List<String> datasetIds, String analysisId) throws RemoteException;

    List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> rerun(List<String> datasetIds, String analysisId) throws RemoteException;

    List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> testrerun(List<String> datasetIds, String analysisId) throws RemoteException;

    Map<String, JobState> queryJobState(List<String> jobIds) throws RemoteException;

    List<String> readLocalFile(String path) throws RemoteException;

    List<String> readRemoteFile(String path) throws RemoteException;
}
