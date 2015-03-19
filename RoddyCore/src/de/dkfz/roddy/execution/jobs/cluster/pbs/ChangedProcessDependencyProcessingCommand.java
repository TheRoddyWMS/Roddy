package de.dkfz.roddy.execution.jobs.cluster.pbs;

import de.dkfz.roddy.execution.jobs.ProcessingCommands;

/**
 * Change a jobs dependency from afterok (default) to something different.
 */
public class ChangedProcessDependencyProcessingCommand extends ProcessingCommands {
    private final ProcessDependency processDependency;

    public enum ProcessDependency {
        after,
        afterok,
        afterfail,
        before,
        beforeok
    }

    public ChangedProcessDependencyProcessingCommand(ProcessDependency processDependency) {
        this.processDependency = processDependency;
    }

    public ProcessDependency getProcessDependency() {
        return processDependency;
    }
}
