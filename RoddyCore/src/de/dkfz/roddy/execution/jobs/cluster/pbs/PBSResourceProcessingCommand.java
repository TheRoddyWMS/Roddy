/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.cluster.pbs;

import java.io.Serializable;

/**
 */
public class PBSResourceProcessingCommand extends PBSProcessingCommands implements Serializable {

    private final String processingString;

    public PBSResourceProcessingCommand(String processingString) {

        this.processingString = processingString;
    }

    public String getProcessingString() {
        return processingString != null ? processingString : "";
    }

    @Override
    public String toString() {
        return "" + processingString;
    }
}
