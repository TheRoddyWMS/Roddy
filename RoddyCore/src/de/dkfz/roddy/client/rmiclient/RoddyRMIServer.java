/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.client.cliclient.CommandLineCall;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.LoggerWrapper;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Semaphore;

/**
 * The Roddy RMI client.
 * <p>
 * Don't convert to Groovy. This might interfere with RMI... Or test it.
 * <p>
 * Created by heinold on 07.09.16.
 */
public class RoddyRMIServer {
    private static final LoggerWrapper loggerWrapper = LoggerWrapper.getLogger(RoddyRMIServer.class.getName());

    public static final String RODDY_SERVER_UP_MESSAGE = "Roddy RMI server instance is ready";
    public static final String RODDY_SERVER_FAIL_MESSAGE = "Roddy RMI server failed at startup: ";

    /**
     * The semaphore for the server. Initialised with 0, it will be acquired, as soon as it was released once (by close)
     **/
    private static final Semaphore rmiActiveSemaphore = new Semaphore(0);

    /**
     * A countdown which needs to be refreshed upon each server action.
     * Keeps the server alive.
     *
     * Set to 0 (run stopServer) to exit the rmiserver
     */
    private static int lastActionCountdown;

    /**
     * @param clc
     */
    public static void startServer(CommandLineCall clc) {
        try {
            RoddyRMIInterfaceImplementation obj = new RoddyRMIInterfaceImplementation();
            RoddyRMIInterface stub = (RoddyRMIInterface) UnicastRemoteObject.exportObject(obj, 0);
            int portNumber = RoddyConversionHelperMethods.toInt(System.getenv("RMIPORT"));

            // TODO IMPORTANT  Enable SSL for RMI! http://docs.oracle.com/javase/1.5.0/docs/guide/rmi/socketfactory/SSLInfo.html
            // Bind the remote object's stub in the registry
            loggerWrapper.postAlwaysInfo("Trying to connect to local rmi registry on port " + portNumber);
            Registry registry = LocateRegistry.createRegistry(portNumber);
            registry.bind("RoddyRMIInterface", stub);

            System.err.println(RODDY_SERVER_UP_MESSAGE);

            startCountdownThread();

            rmiActiveSemaphore.acquire();
            lastActionCountdown = 0;

            registry.unbind("RoddyRMIInterface");
            System.err.println("Stopped server");
        } catch (Exception e) {
            System.err.println(RODDY_SERVER_FAIL_MESSAGE + e.toString());
            e.printStackTrace();
        }
    }

    public static void touchServer() {
        lastActionCountdown = getRMIServerCountDown() * 4; // multiply by four, steps are in quarter seconds.
        System.err.println("Resetting countdown");
    }

    private static int getRMIServerCountDown() {
        return RoddyConversionHelperMethods.toInt(Roddy.getApplicationProperty("roddyRMIServerCountDown", "180"), 180);
    }

    public static void stopServer() {
        System.err.println("Stopping server.");
        lastActionCountdown = 0;
        rmiActiveSemaphore.release();
    }

    public static boolean isActive() {
        return rmiActiveSemaphore.hasQueuedThreads();
    }

    private static void startCountdownThread() {
        Thread countdownThread = new Thread(() -> {
            System.err.println("Start countdown thread with " + getRMIServerCountDown() + "s, lastActionCountDown == " + lastActionCountdown);
            touchServer();
            while (lastActionCountdown > 0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lastActionCountdown--;
            }
            System.err.println("Stopped server due to timeout");
            stopServer();
        });
        countdownThread.setName("RMI countdown thread");
        countdownThread.start();
    }
}
