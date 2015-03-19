package de.dkfz.roddy.core;

import de.dkfz.roddy.config.Configuration;

import java.lang.reflect.Method;

/**
 * A worklow can be created and executed to process a set of data.
 *
 * @author michael
 */
public abstract class Workflow {

    private Configuration config;
    private DataSet dataSet;
    private String baseDir;

    public Workflow() {
    }

    /**
     * Workflow specific setups can be created here.
     * This includes i.e. the creation of paths.
     */
    public void setupExecution(ExecutionContext context) {
    }

    public void finalizeExecution(ExecutionContext context) {
    }

    /**
     * Override this method to enable initial checks for a workflow run.
     * I.e. check if input files can be found.
     * @param context
     * @return
     */
    public boolean checkExecutability(ExecutionContext context) {
        return true;
    }

    public abstract boolean execute(ExecutionContext context);

    public boolean canCreateTestdata() {
        for (Method m : this.getClass().getMethods()) {
            if (m.getName().equals("createTestdata") && m.getDeclaringClass() == this.getClass()) {
                return true;
            }
        }
        return false;
    }

    public boolean createTestdata(ExecutionContext context) {
        return false;
    }

    public boolean hasCleanupMethod() {
        for (Method m : this.getClass().getMethods()) {
            if (m.getName().equals("cleanup") && m.getDeclaringClass() == this.getClass()) {
                return true;
            }
        }
        return false;
    }

    public boolean cleanup(DataSet dataset) {
        return false;
    }
}
