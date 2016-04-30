package de.dkfz.roddy.execution.jobs;


import de.dkfz.roddy.core.ExecutionContext;

import java.io.IOException;
import java.io.Serializable;

/**
 * Base class for all types of commands.
 *
 * PBSCommand extends this. Also SGECommand and so on.
 *
 * A job is executed via a command. The command represents the job on the cluster / system side.
 *
 * @author michael
 */
public abstract class Command implements Serializable {


    public void setJob(Job job) {
        this.creatingJob = job;
    }

    public static class DummyCommand extends Command {

        private String jobName;

        public DummyCommand(Job job, ExecutionContext run, String jobName, boolean isArray) {
            super(job, run, "dummy_" + getNextIDCountValue());
            this.jobName = jobName;
            if (isArray) {
                setExecutionID(JobDependencyID.getNotExecutedFakeJob(job, true));
            } else {
                setExecutionID(JobDependencyID.getNotExecutedFakeJob(job));
            }
        }


        @Override
        public String toString() {
            return String.format("Command of class %s with id %s and name %s", this.getClass().getName(), getID(), jobName);
        }

    }
    /**
     * Static incremental counter for pipeline commands.
     */
    protected static volatile int idCounter = -1;
    /**
     * The id of this command.
     */
    private final String id;
    /**
     * The id which was created upon execution by the job system.
     */
    protected JobDependencyID executionID;

    /**
     * The job which created this command. Can be null!
     */
    protected Job creatingJob;

    protected transient final ExecutionContext executionContext;

    protected Command(Job job, ExecutionContext run, String id) {
        this.creatingJob = job;
        this.id = id;
        this.executionContext = run;
        JobManager.getInstance().addCommandToList(this);
    }

    protected Command(Job job, String id) {
        this(job, job.context, id);
        this.creatingJob = job;
    }

    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
        try {
            s.defaultWriteObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        try {
            s.defaultReadObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    protected static synchronized int getNextIDCountValue() {
        return ++idCounter;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public final void setExecutionID(JobDependencyID id) {
        this.executionID = id;
    }

    public final boolean wasExecuted() {
        return executionID.isValidID();//executionID != null && executionID.trim().length() > 0;
    }

    public final JobDependencyID getExecutionID() {
        return executionID;
    }

    public final String getID() {
        return id;
    }

    public final Job getJob() {
        return creatingJob;
    }

    public final String getFormattedID() {
        return String.format("command:0x%08X", id);
    }

    /**
     * Local commands are i.e. blocking, whereas PBSCommands are not.
     * The default is false.
     * @return
     */
    public boolean isBlockingCommand() { return false; }

    @Override
    public String toString() {
        return String.format("Command of class %s with id %s", this.getClass().getName(), getID());
    }
}
