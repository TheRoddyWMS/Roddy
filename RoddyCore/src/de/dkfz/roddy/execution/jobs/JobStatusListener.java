package de.dkfz.roddy.execution.jobs;

/**
 * Classes implementing this interface can be informed by a job if the job's state changes.
 */
public interface JobStatusListener {
    public void jobStatusChanged(Job job, JobState oldState, JobState newState);
}
