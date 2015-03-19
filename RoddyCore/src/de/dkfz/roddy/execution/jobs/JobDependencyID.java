package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;

import java.io.Serializable;

/**
 */
public abstract class JobDependencyID implements Serializable {

    protected JobDependencyID(Job job) {
        this.job = job;
    }

    public abstract boolean isValidID();

    public final Job job;

    /**
     * Various reasons why a job was not executed and is a fake job.
     */
    public enum FakeJobReason {
        NOT_EXECUTED,
        FILE_EXISTED,
        UNDEFINED,
    }

    public static class FakeJobID extends JobDependencyID implements Serializable {
        private FakeJobReason fakeJobReason;
        private long nanotime;
        private boolean isArray;

        public FakeJobID(Job job, FakeJobReason fakeJobReason, boolean isArray) {
            super(job);
            this.fakeJobReason = fakeJobReason;
            this.isArray = isArray;
            nanotime = System.nanoTime();
        }

        public FakeJobID(Job job, boolean isArray) {
            this(job, FakeJobReason.UNDEFINED, isArray);
        }

        public FakeJobID(Job job, FakeJobReason fakeJobReason) {
            this(job, fakeJobReason, false);
        }

        public FakeJobID(Job job) {
            this(job, FakeJobReason.UNDEFINED, false);
        }

        /**
         * Fake ids are never valid!
         *
         * @return
         */
        @Override
        public boolean isValidID() {
            return false;
        }

        @Override
        public String getId() {
            return String.format("%s.%s", getShortID(), fakeJobReason.name());
        }

        @Override
        public String getShortID() {
            return String.format("0x%08X%s", nanotime, isArray ? "[]" : "");
        }

        @Override
        public boolean isArrayJob() {
            return false;
        }

        @Override
        public String toString() {
            return getShortID();
        }

        public static boolean isFakeJobID(String jobID) {
            return jobID.startsWith("0x");
        }
    }

    public abstract String getId();

    public abstract String getShortID();

    public abstract boolean isArrayJob();

    public static FakeJobID getNotExecutedFakeJob(Job job) {
        return getNotExecutedFakeJob(job, false);
    }
    public static FakeJobID getNotExecutedFakeJob(Job job, boolean array) {
        return new FakeJobID(job, FakeJobReason.NOT_EXECUTED,array);
    }
    public static FakeJobID getFileExistedFakeJob(ExecutionContext context) {
        return getFileExistedFakeJob(new Job.FakeJob(context), false);
    }
    public static FakeJobID getFileExistedFakeJob(Job job, boolean array) {
        return new FakeJobID(job, FakeJobReason.FILE_EXISTED, array);
    }
}
