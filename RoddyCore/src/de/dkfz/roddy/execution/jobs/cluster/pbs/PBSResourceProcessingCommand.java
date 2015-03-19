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
