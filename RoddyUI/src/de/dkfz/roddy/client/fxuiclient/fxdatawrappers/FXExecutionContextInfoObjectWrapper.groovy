/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.client.rmiclient.RoddyRMIInterfaceImplementation
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask
import groovy.transform.CompileStatic;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue

/**
 * Encapsulates execution context objects.
 */
@CompileStatic
public class FXExecutionContextInfoObjectWrapper {

    ExecutionContextInfoObject executionContextInfoObject;

    private IntegerProperty noOfJobs = new SimpleIntegerProperty();

    private StringProperty noOfRunningJobs = new SimpleStringProperty();
    private StringProperty noOfFailedJobs = new SimpleStringProperty();
    private StringProperty noOfFinishedJobs = new SimpleStringProperty();
    private StringProperty noOfUnknownJobs = new SimpleStringProperty();
    private StringProperty noOfUnknownReadoutJobs = new SimpleStringProperty();
    private StringProperty noOfQueuedJobs = new SimpleStringProperty();
    private StringProperty noOfJobsOnHold = new SimpleStringProperty();

    Map<JobState, StringProperty> mappedProperties = new HashMap<>();


    public FXExecutionContextInfoObjectWrapper(ExecutionContextInfoObject executionContextInfoObject) {
        this.executionContextInfoObject = executionContextInfoObject;
        mappedProperties.put(JobState.FAILED, noOfFailedJobs);
        mappedProperties.put(JobState.RUNNING, noOfRunningJobs);
        mappedProperties.put(JobState.OK, noOfFinishedJobs);
        mappedProperties.put(JobState.UNKNOWN, noOfUnknownJobs);
        mappedProperties.put(JobState.UNKNOWN_READOUT, noOfUnknownReadoutJobs);
        mappedProperties.put(JobState.QUEUED, noOfQueuedJobs);
        mappedProperties.put(JobState.HOLD, noOfJobsOnHold);
        jobStateChangedEvent(null);

    }

    public String getID() {
        return "Run: " + executionContextInfoObject.getExecutionDate();
    }

    public ObservableValue<? extends String> getJobStateProperty(JobState jobState) {
        return mappedProperties.get(jobState);
    }

    public void jobStateChangedEvent(JobInfoObject job) {
        final Map<JobState, Integer> mapIntegers = new LinkedHashMap<>();

        for (JobState js : JobState.values())
            mapIntegers.put(js, 0);

        for (JobInfoObject j : executionContextInfoObject.getExecutedJobs()) {
            int c;
            JobState readoutJobState = JobState.FAILED;
            try {
                readoutJobState = j.getJobState();
                c = mapIntegers.get(readoutJobState);
            } catch (Exception e) {
                c = 1000;
            }
            mapIntegers.put(readoutJobState, c + 1);
        }

        RoddyUITask.invokeLater(new Runnable() {
            @Override
            public void run() {
                synchronized (mappedProperties) {
                    for (JobState js : mappedProperties.keySet())
                        mappedProperties.get(js).setValue(mapIntegers.get(js).toString());
                }
            }
        }, "update econtext info", false);
    }

}
