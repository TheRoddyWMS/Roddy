/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

/**
 * Classes implementing this interface can be informed by a job if the job's state changes.
 */
public interface JobStatusListener {
    public void jobStatusChanged(Job job, JobState oldState, JobState newState);
}
