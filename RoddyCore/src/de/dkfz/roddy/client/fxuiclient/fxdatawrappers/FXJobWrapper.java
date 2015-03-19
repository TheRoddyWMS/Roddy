package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.execution.jobs.JobStatusListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Wrapper for Roddy (cluster) jobs.
 */
public class FXJobWrapper implements JobStatusListener {
    private Job job;

    private StringProperty jobIDProperty = new SimpleStringProperty();
    private StringProperty toolIDProperty = new SimpleStringProperty();
    private ObjectProperty jobStateProperty = new SimpleObjectProperty();

    public FXJobWrapper(Job job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return job.getJobID() + ": " + job.toolID;
    }

    public Job getJob() {
        return job;
    }


    @Override
    public void jobStatusChanged(Job job, JobState oldState, JobState newState) {
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
