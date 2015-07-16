package de.dkfz.roddy.execution.jobs.cluster;

import de.dkfz.roddy.execution.jobs.Command;
import de.dkfz.roddy.execution.jobs.CommandFactory;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommand;

/**
 * A class for processing backends running on a cluster.
 * This mainly defines variables and constants which can be set via the configuration.
 */
public abstract class ClusterCommandFactory<C extends Command> extends CommandFactory<C> {
    public static final String CVALUE_ENFORCE_SUBMISSION_TO_NODES="enforceSubmissionToNodes";
}
