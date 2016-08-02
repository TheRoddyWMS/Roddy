/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.roddy.execution.jobs.Job;

import java.io.File;

/**
 * A listener is able to receive changes for execution contexts.
 * This is mainly for active contexts but it might also be of use if
 * old contexts are read in.
 */
public interface ExecutionContextListener {

    /**
     * This event occurs if a new event was created, i.e. if a context was read from storage or a new one was started.
     *
     * @param context
     */
    public void newExecutionContextEvent(ExecutionContext context);

    /**
     * This event is raised when a job's state was changed
     *
     * @param job
     */
    public void jobStateChangedEvent(Job job);

    /**
     * This event is raised when a job was added to the contexts list.
     *
     * @param job
     */
    public void jobAddedEvent(Job job);

    public void fileAddedEvent(File file);

    public void detailedExecutionContextLevelChanged(ExecutionContext context);
}
