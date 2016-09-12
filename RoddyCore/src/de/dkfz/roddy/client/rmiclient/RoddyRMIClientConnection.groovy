/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.execution.io.ExecutionHelper
import groovy.transform.CompileStatic

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry

/**
 * The (JavaFX) RMI client for Roddy.
 * Created by heinold on 08.09.16.
 */
@CompileStatic
public class RoddyRMIClientConnection {

    private RoddyRMIInterface connection;

    private RoddyRMIInstanceInfo instanceInfo;

    private static int rmiregistryStart = 60000;

    public static class RoddyRMIInstanceInfo {

        private Process process;

        RoddyRMIInstanceInfo(Process process) {
            this.process = process
        }

        public String getProcessID() {
            return ExecutionHelper.getProcessID(process);
        }
    }

    public RoddyRMIClientConnection() {

    }


    private RoddyRMIInstanceInfo startRMIInfo(String startString) {
        rmiregistryStart++;
        startString = "RMIPORT=${rmiregistryStart} ${startString}".replace("#PORTNUMBER#", "" + rmiregistryStart)
        instanceInfo = new RoddyRMIInstanceInfo(ExecutionHelper.executeNonBlocking(startString));

        OutputStream appendable = new ByteArrayOutputStream();

        // Read out the lines of the process and check for messages like server up.
        float timeout = 10; //seconds
        boolean serverIsRunning = false;
        while (timeout > 0 && !serverIsRunning) {
            instanceInfo.process.consumeProcessOutput(appendable, appendable)
            def lines = appendable.toString().readLines();
            assert !lines.find { it == RoddyRMIServer.RODDY_SERVER_FAIL_MESSAGE };
            serverIsRunning = lines.find { it == RoddyRMIServer.RODDY_SERVER_UP_MESSAGE }
            timeout -= 0.125;
            Thread.sleep(125)

        }
        System.out.println(appendable.toString());

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", rmiregistryStart);
            connection = (RoddyRMIInterface) registry.lookup("RoddyRMIInterface");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        return instanceInfo;
    }

    public RoddyRMIInstanceInfo startLocalRoddyRMIServerAndConnect(String inifile, String project, String analysis) {
        def binary = "${Roddy.getApplicationDirectory()}/roddy.sh"

        // TODO autoselect?  --useRoddyVersion=auto
        def startString = "${binary} rmi ${project}@${analysis} #PORTNUMBER# --useconfig=${inifile}"
        instanceInfo = startRMIInfo(startString);
        return instanceInfo
    }

    public RoddyRMIInstanceInfo startLocalRoddyRMIServerAndConnect(File roddyClassesDirectory) {

        // Get all jar files from the class path
        def collectedLibraries = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs().collect { it.toString().replace("file:", "") }.join(":")

        // Assemble and run the Roddy command, don't forget the jfx runtime jar file which is in ext folder)
        // TODO this is really platform dependent code. Leave it for now, but we might get trouble with it.
        def cmd = "jfxrtLib=\$(ls \$(dirname \$(dirname `which java`))/jre/lib/ext/jfxrt.jar); cd ${Roddy.getApplicationDirectory()}; java -cp \$jfxrtLib:${roddyClassesDirectory}:${collectedLibraries} -Djava.rmi.server.codebase=${roddyClassesDirectory} de.dkfz.roddy.Roddy rmi \$\$"

        instanceInfo = startRMIInfo(cmd);
        return instanceInfo;
    }

    RoddyRMIInstanceInfo getInstanceInfo() {
        return instanceInfo
    }

    public boolean pingServer() {
        try {
            return connection.ping();
        } catch (Exception ex) {
            return false;
        }
    }

    public void closeServer() {
        try {
            connection.close();
        } catch (Exception ex) {

        }
    }

    public Map<String, String> getListOfDatasets() {
        try {
            return connection.listdatasets();
        } catch (Exception ex) {
            return [:];
        }
    }

}
