/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.client.rmiclient.RoddyRMIInterfaceImplementation;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.eilslabs.batcheuphoria.jobs.JobState;
//import de.dkfz.roddy.execution.jobs.JobStatusListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Wrapper for Roddy (cluster) jobs.
 */
public class FXJobInfoObjectWrapper {
//
//} implements JobStatusListener {
    private RoddyRMIInterfaceImplementation.JobInfoObject job;

    private StringProperty jobIDProperty = new SimpleStringProperty();
    private StringProperty toolIDProperty = new SimpleStringProperty();
    private ObjectProperty jobStateProperty = new SimpleObjectProperty();

    public FXJobInfoObjectWrapper(RoddyRMIInterfaceImplementation.JobInfoObject job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return job.getJobId() + ": " + job.getToolId();
    }

    public RoddyRMIInterfaceImplementation.JobInfoObject getJob() {
        return job;
    }

    public StringProperty jobIDProperty() {
        return jobIDProperty;
    }

    public StringProperty toolIDProperty() {
        return toolIDProperty;
    }

    public ObjectProperty jobStateProperty() {
        return jobStateProperty;
    }

}
