/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient;

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

    boolean ping() throws RemoteException;

    /**
     * Close the service
     */
    void close() throws RemoteException;

    Map<String, String> listdatasets() throws RemoteException;

    void testrun() throws RemoteException;
}
