/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.execution.io.ExecutionHelper
import de.dkfz.roddy.execution.jobs.JobState
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

    private boolean alive = true;

    private static int rmiregistryStart = 60000;


    public static class RoddyRMIInstanceInfo {

        private Process process;

        private OutputStream appendable = new ByteArrayOutputStream();

        private List<String> lines = [];

        RoddyRMIInstanceInfo(Process process) {
            this.process = process
        }

        public String getProcessID() {
            return ExecutionHelper.getProcessID(process);
        }

        public synchronized List<String> updateOutput() {
            process.consumeProcessOutput(appendable, appendable)
            def allReadLines = appendable.toString().readLines();
            def diff = allReadLines - lines;
            if(diff) println(diff);
            lines = allReadLines;
            return allReadLines;
        }
    }

    private static List<RoddyRMIClientConnection> connectionList = [];

    private static checkAndStartCheckConsumptionThread(RoddyRMIClientConnection newObject) {
        synchronized (connectionList) {
            connectionList << newObject;
            if(connectionList.size() > 1) return;
        }

        boolean active = true;

        while (active) {
            List<RoddyRMIClientConnection> listToCheck = [];
            synchronized (connectionList) {
                listToCheck += connectionList.findAll { it.alive && it.pingServer(false) }
            }

            listToCheck.each {
                RoddyRMIClientConnection connection ->
                    connection.instanceInfo.process.consumeProcessOutput()
            }

            synchronized (connectionList) {
                connectionList.clear();
                connectionList += listToCheck;
                active = connectionList;
            }
            Thread.sleep(250);
        }
        println("Closed client surveillance thread.")
    }


    public RoddyRMIClientConnection() {

    }


    private RoddyRMIInstanceInfo startRMIInfo(String startString) {
        rmiregistryStart++;
        startString = "RMIPORT=${rmiregistryStart} ${startString}".replace("#PORTNUMBER#", "" + rmiregistryStart)
        instanceInfo = new RoddyRMIInstanceInfo(ExecutionHelper.executeNonBlocking(startString));

        // The method needs an instanceinfo
        checkAndStartCheckConsumptionThread(this);

        // Read out the lines of the process and check for messages like server up.
        float timeout = 20; //seconds
        boolean serverIsRunning = false;
        while (timeout > 0 && !serverIsRunning) {
            def lines = instanceInfo.updateOutput();
            assert !lines.find { it == RoddyRMIServer.RODDY_SERVER_FAIL_MESSAGE };
            serverIsRunning = lines.find { it == RoddyRMIServer.RODDY_SERVER_UP_MESSAGE }
            timeout -= 0.125;
            Thread.sleep(125)

        }

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

    public boolean pingServer(boolean keepAlive = true) {
        try {
            return connection.ping(keepAlive);
        } catch (Exception ex) {
            return false;
        }
    }

    public void closeServer() {
        try {
            alive = true;
            connection.close();
        } catch (Exception ex) {

        }
    }

    public List<RoddyRMIInterfaceImplementation.DataSetInfoObject> getListOfDatasets(String analysisId) {
        try {
            return connection.listdatasets(analysisId);
        } catch (Exception ex) {
            return [];
        }
    }

    RoddyRMIInterfaceImplementation.ExtendedDataSetInfoObjectCollection queryExtendedDataSetInfo(String id, String analysis) {
        try {
            return connection.queryExtendedDataSetInfo(id, analysis);
        } catch (Exception ex) {
            return null;
        }
    }

    public JobState queryDataSetState(String dataSetId, String analysisId) {
        try {
            return connection.queryDataSetState(dataSetId, analysisId);
        } catch (Exception ex) {

        }
    }


    boolean queryDataSetExecutability(String dataSetId, String analysisId) {
        try {
            return connection.queryDataSetExecutability(dataSetId, analysisId);
        } catch (Exception ex) {

        }
    }


    public Map<String, JobState> queryJobState(List<String> jobIds) {
        try {
            return connection.queryJobState(jobIds);
        } catch (Exception ex) {

        }
    }

    public List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> run(List<String> datasets, String analysisId, boolean test) {
        try {
            return connection.run(datasets, analysisId);
        } catch (Exception ex) {
            return [];
        }
    }

    public List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> rerun(List<String> datasets, String analysisId, boolean test) {
        try {
            return connection.run(datasets, analysisId);
        } catch (Exception ex) {
            return [];
        }
    }

    public List<String> readLocalFile(String file) {
        try {
            return connection.readLocalFile(file);
        } catch (Exception ex) {
            return []
        }
    }

    public List<String> readRemoteFile(String file) {
        try {
            return connection.readRemoteFile(file);
        } catch (Exception ex) {
            return []
        }
    }

}
