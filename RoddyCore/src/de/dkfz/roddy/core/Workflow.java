/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;
import de.dkfz.roddy.knowledge.files.FileObject;
import de.dkfz.roddy.knowledge.methods.GenericMethod;

import java.lang.reflect.Method;
import java.util.List;

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

    /**
     * Convenience method to call GenericMethod.callGenericTool()
     * @param toolName
     * @param input
     * @param additionalInput
     * @return
     */
    protected FileObject call(String toolName, BaseFile input, Object... additionalInput) {
        return GenericMethod.callGenericTool(toolName, input, additionalInput);
    }

    protected FileGroup callWithOutputFileGroup(String toolName, BaseFile input, int numericCount, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, numericCount, additionalInput);
    }

    protected FileGroup callWithOutputFileGroup(String toolName, BaseFile input, List<String> indices, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, indices, additionalInput);
    }

    /**
     * Convenience method to get a boolean runflag from the context config, defaults to true
     * @param context
     * @param flagID
     * @return
     */
    protected boolean getflag(ExecutionContext context, String flagID) {
        return getflag(context, flagID, true);
    }

    /**
     * Convenience method to get a boolean runflag from the context config, defaults to defaultValue
     * @param context
     * @param flagID
     * @return
     */
    protected boolean getflag(ExecutionContext context, String flagID, boolean defaultValue) {
        return context.getConfiguration().getConfigurationValues().getBoolean(flagID, defaultValue);
    }
}
