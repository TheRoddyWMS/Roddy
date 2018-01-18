/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationError;
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
     *
     * @param context
     * @return
     */
    public boolean checkExecutability(ExecutionContext context) {
        return true;
    }

    public abstract boolean execute(ExecutionContext context) throws ConfigurationError;

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
     *
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
     *
     * @param context
     * @param flagID
     * @return
     */
    protected boolean getflag(ExecutionContext context, String flagID) {
        return getflag(context, flagID, true);
    }

    /**
     * Convenience method to get a boolean runflag from the context config, defaults to defaultValue
     *
     * @param context
     * @param flagID
     * @return
     */
    protected boolean getflag(ExecutionContext context, String flagID, boolean defaultValue) {
        return context.getConfiguration().getConfigurationValues().getBoolean(flagID, defaultValue);
    }

    /**
     * To "load" a source file from storage
     * Will call getSourceFile with context, path and BaseFile.STANDARD_FILE_CLASS
     *
     * @param context The context to which the file belongs
     * @param path Path to the file (remote or local)
     * @return
     */
    protected BaseFile getSourceFile(ExecutionContext context, String path) {
        return getSourceFile(context, path, BaseFile.STANDARD_FILE_CLASS);
    }

    /**
     * To "load" a source file from storage
     *
     * @param context The context to which the file belongs
     * @param path Path to the file (remote or local)
     * @param _class The class of the new file object. This may be an existing or a new class (which will then be created)
     * @return
     */
    protected BaseFile getSourceFile(ExecutionContext context, String path, String _class) {
        return BaseFile.fromStorage(context, path, _class);
    }

    /**
     * Easily create a file which inherits from another file.
     * Will call getDerivedFile with parent and BaseFile.STANDARD_FILE_CLASS
     * @param parent The file from which the new file object inherits
     * @return
     */
    protected BaseFile getDerivedFile(BaseFile parent) {
        return getDerivedFile(parent, BaseFile.STANDARD_FILE_CLASS);
    }

    /**
     * Easily create a file which inherits from another file.
     * @param parent The file from which the new file object inherits
     * @param _class The class of the new file object. This may be an existing or a new class (which will then be created)
     * @return
     */
    protected BaseFile getDerivedFile(BaseFile parent, String _class) {
        return BaseFile.deriveFrom(parent, _class);
    }
}
