package de.dkfz.roddy.execution.jobs;

/**
 * This enum defines various kinds of job types.
 *
 *
 */
public enum JobType  {
    /**
     * A regular job.
     */
    STANDARD,

    /**
     * The head job for an array. This job is the "parent" job for array child jobs.
     */
    ARRAY_HEAD,

    /**
     * Array child job.
     */
    ARRAY_CHILD
}
