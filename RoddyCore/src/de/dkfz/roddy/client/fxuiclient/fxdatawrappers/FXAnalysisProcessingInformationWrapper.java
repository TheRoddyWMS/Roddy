/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.core.AnalysisProcessingInformation;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextListener;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates execution context objects.
 */
public class FXAnalysisProcessingInformationWrapper implements ExecutionContextListener {

    private AnalysisProcessingInformation analysisProcessingInformation;

    private IntegerProperty noOfJobs = new SimpleIntegerProperty();

    private StringProperty noOfRunningJobs = new SimpleStringProperty();
    private StringProperty noOfFailedJobs = new SimpleStringProperty();
    private StringProperty noOfFinishedJobs = new SimpleStringProperty();
    private StringProperty noOfUnknownJobs = new SimpleStringProperty();
    private StringProperty noOfUnknownReadoutJobs = new SimpleStringProperty();
    private StringProperty noOfQueuedJobs = new SimpleStringProperty();
    private StringProperty noOfJobsOnHold = new SimpleStringProperty();

    Map<JobState, StringProperty> mappedProperties = new HashMap<>();


    public FXAnalysisProcessingInformationWrapper(AnalysisProcessingInformation analysisProcessingInformation) {
        this.analysisProcessingInformation = analysisProcessingInformation;
        mappedProperties.put(JobState.FAILED, noOfFailedJobs);
        mappedProperties.put(JobState.RUNNING, noOfRunningJobs);
        mappedProperties.put(JobState.OK, noOfFinishedJobs);
        mappedProperties.put(JobState.UNKNOWN, noOfUnknownJobs);
        mappedProperties.put(JobState.UNKNOWN_READOUT, noOfUnknownReadoutJobs);
        mappedProperties.put(JobState.QUEUED, noOfQueuedJobs);
        mappedProperties.put(JobState.HOLD, noOfJobsOnHold);
        analysisProcessingInformation.getDetailedProcessingInfo().registerContextListener(this);
        jobStateChangedEvent(null);

    }

    public AnalysisProcessingInformation getAnalysisProcessingInformation() {
        return analysisProcessingInformation;
    }

    public ExecutionContext getExecutionContext() {
        return analysisProcessingInformation.getDetailedProcessingInfo();
    }
//
//    @Override
//    public List<AnalysisProcessingInformation> getProcessingInfo() {
//        return Arrays.asList(analysisProcessingInformation);
//    }
//
//    @Override
//    public AnalysisProcessingInformation getFirstProcessingInfoWithContextLevel(ExecutionContextLevel executionContextLevel) {
//        if(analysisProcessingInformation.getDetailedProcessingInfo().getExecutionContextLevel() == executionContextLevel)
//            return analysisProcessingInformation;
//        return null;
//    }

    //    @Override
    public String getID() {
        return "Run: " + analysisProcessingInformation.getExecutionDateHumanReadable();
    }

    public ObservableValue<? extends String> getJobStateProperty(JobState jobState) {
        return mappedProperties.get(jobState);
    }

    @Override
    public void newExecutionContextEvent(ExecutionContext context) {
    }

    @Override
    public void jobStateChangedEvent(Job job) {
        final Map<JobState, Integer> mapIntegers = new LinkedHashMap<>();

        for (JobState js : JobState.values())
            mapIntegers.put(js, 0);

        for (Job j : getExecutionContext().getExecutedJobs()) {
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

    @Override
    public void jobAddedEvent(Job job) {
        synchronized (noOfJobs) {
            noOfJobs.set(analysisProcessingInformation.getDetailedProcessingInfo().getExecutedJobs().size());
        }
        jobStateChangedEvent(job);
    }

    @Override
    public void fileAddedEvent(File file) {
    }

    @Override
    public void detailedExecutionContextLevelChanged(ExecutionContext context) {
    }

}
