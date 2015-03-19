package de.dkfz.roddy.execution.io;

import de.dkfz.roddy.core.TriState;
import de.dkfz.roddy.execution.jobs.Command;

/**
 * Listens to Execution Service events.
 * Gets a message if a job was created or a command was executed.
 */
public interface ExecutionServiceListener {
    /**
     * Receives an ExecutionResult object of a string execution.
     * Commands are normally converted to a string upon execution so make sure you override the right method.
     * @param result
     */
    void stringExecuted(String commandString, ExecutionResult result);

    /**
     * Receives an executed command. You can pull out the ExecutionResult afterwards.
     * @param result
     */
    void commandExecuted(Command result);

    /**
     * Change the state (active / nonactive) of an execution service.
     * @param state TRUE, FALSE or UNKNOWN / CONNECTING
     */
    void changeExecutionServiceState(TriState state);

    /**
     * Fired if an execution has started.
     */
    void executionStarted(long id, String text);

    /**
     * Fired if an execution has stopped.
     * @param id
     */
    void executionFinished(long id, String text);
}
