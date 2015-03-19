package de.dkfz.roddy.execution.jobs.direct.synchronousexecution;

import de.dkfz.roddy.execution.jobs.ProcessingCommands;

/**
 */
public class DummyProcessingCommand extends ProcessingCommands {
    private String text;

    public DummyProcessingCommand(String text) {
        this.text = text;
    }



    @Override
    public String toString() {
        return "Dummy: " + text;
    }
}
