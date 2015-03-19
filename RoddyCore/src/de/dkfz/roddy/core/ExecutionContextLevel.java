package de.dkfz.roddy.core;

/**
 * Specifies the level of an ExecutionContext object
 */
public enum ExecutionContextLevel {

    /**
     * Do not execute anything. Used for collecting data for previous
     * pipeline runs.
     */
    QUERY_STATUS,
    /**
     * Run everything.
     */
    RUN(true, true),
    /**
     * Only run if files are not there
     */
    RERUN(true, true),
    TESTRERUN(false,false),
    /**
     * Allowed after run/rerun
     * Tells the system that submitting jobs is not allowed any more
     */
    ABORTED(false, true),
    /**
     * Level to create test data
     */
    CREATETESTDATA,
    /**
     * Nothing special or unknown
     */
    UNSET,
    /**
     * Valid for read out jobs.
     */
    READOUT,
    CLEANUP(true,true);

    public final boolean canSubmitJobs;

    public final boolean isOrWasAllowedToSubmitJobs;

    private ExecutionContextLevel() {
        this.canSubmitJobs = false;
        this.isOrWasAllowedToSubmitJobs = false;
    }

    private ExecutionContextLevel(boolean canSubmitJobs, boolean isOrWasAllowedToSubmitJobs) {
        this.canSubmitJobs = canSubmitJobs;
        this.isOrWasAllowedToSubmitJobs = isOrWasAllowedToSubmitJobs;
    }
}
