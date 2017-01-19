/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

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
