package de.dkfz.roddy.execution.jobs;

/**
 * A generic state for jobs.
 */
public enum JobState {
    /**
     * Job is still running
     */
    RUNNING,
    /**
     * Job has failed
     */
    FAILED,
    /**
     * Job was ok
     */
    OK,
    /**
     * Job state is unknown
     */
    UNKNOWN,
    /**
     * Job state is unknown because it was freshly read out from a file.
     */
    UNKNOWN_READOUT,
    /**
     * Recently submitted job, state is unknown
     */
    UNKNOWN_SUBMITTED,
    /**
     * Jobs which were submitted but not started (i.e. due to crashed or cancelled succeeding jobs).
     */
    UNSTARTED,
    HOLD,
    QUEUED,
    /**
     * Dummy jobs were not executed but can contain runtime information for future runs.
     */
    DUMMY, ABORTED, FAILED_POSSIBLE;

    public boolean isPlannedOrRunning() {
        return _isPlannedOrRunning(this);
    }

    public static boolean _isPlannedOrRunning(JobState jobState) {
        return jobState == JobState.UNSTARTED || jobState == JobState.RUNNING || jobState == JobState.QUEUED || jobState == JobState.HOLD;
    }

    public boolean isDummy() {
        return this == DUMMY;
    }

    public boolean isRunning() {
        return this == JobState.RUNNING;
    }

    public boolean isUnknown() { return  this == UNKNOWN || this == UNKNOWN_READOUT || this == UNKNOWN_SUBMITTED; }
}
