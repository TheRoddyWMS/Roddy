package de.dkfz.roddy.execution.jobs.direct.synchronousexecution;

import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobDependencyID;

/**
 */
public class DirectCommandDependencyID extends JobDependencyID {
    private final String id;

    protected DirectCommandDependencyID(String id, Job job) {
        super(job);
        this.id = id;
    }

    @Override
    public boolean isValidID() {
        return id != null && id != "none";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getShortID() {
        return id;
    }

    @Override
    public boolean isArrayJob() {
        //TODO
        return false;
    }

    @Override
    public String toString() {
        return "Direct command " + id;
    }
}
