/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.execution.UnexpectedExecutionResultException
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.methods.GenericMethod

import java.lang.reflect.Method
import java.util.concurrent.ExecutionException

/**
 * A worklow can be created and executed to process a set of data.
 *
 * @author michael
 */
abstract class Workflow {

    /**
     * Keep this private so it cannot be overriden. Setter allows to set it ONCE.
     */
    private ExecutionContext context

    Workflow() {
    }

    ExecutionContext getContext() {
        return context
    }

    void setContext(ExecutionContext context) {
        if (context == null)
            this.context = context
        else
            throw new RuntimeException("It is not allowed to reset the context object of a workflow.") // This is a programming error
    }

    /**
     * Workflow specific setups can be created here.
     * This includes i.e. the creation of paths.
     */
    void setupExecution(ExecutionContext context) {
    }

    void finalizeExecution(ExecutionContext context) {
    }

    /**
     * Override this method to enable initial checks for a workflow run.
     * I.e. check if input files can be found.
     *
     * @param context
     * @return
     */
    boolean checkExecutability(ExecutionContext context) {
        return true
    }

    abstract boolean execute(ExecutionContext context) throws ConfigurationError

    boolean hasCleanupMethod() {
        for (Method m : this.getClass().getMethods()) {
            if (m.getName().equals("cleanup") && m.getDeclaringClass() == this.getClass()) {
                return true
            }
        }
        return false
    }

    boolean cleanup(DataSet dataset) {
        return false
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
        return GenericMethod.callGenericTool(toolName, input, additionalInput)
    }

    protected FileGroup callWithOutputFileGroup(String toolName, BaseFile input, int numericCount, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, numericCount, additionalInput)
    }

    protected FileGroup callWithOutputFileGroup(String toolName, BaseFile input, List<String> indices, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, indices, additionalInput)
    }

    /**
     * Call the tool toolID with parameters synchronously and using the wrapInScript.
     *
     * @param toolID
     * @param parameters
     * @return
     */
    List<String> callSynchronized(String toolID, Map<String, Object> parameters) {
        return ExecutionService.getInstance().callSynchronized(context, toolID, parameters)
    }

    /**
     * Convenience method to get a boolean runflag from the context config, defaults to true
     *
     * @param flagID
     * @return
     */
    protected boolean getflag(String flagID) {
        return getflag(flagID, true)
    }


    /**
     * Convenience method to get a boolean runflag from the context config, defaults to defaultValue
     *
     * @param flagID
     * @return
     */
    protected boolean getflag(String flagID, boolean defaultValue) {
        return context.getConfiguration().getConfigurationValues().getBoolean(flagID, defaultValue)
    }

    /**
     * Instantiate a source file object representing a file on storage.
     * Will call getSourceFile with context, path and BaseFile.STANDARD_FILE_CLASS
     *
     * @param path Pathname string to the file (remote or local)
     * @return
     */
    protected BaseFile getSourceFile(String path) {
        return getSourceFile(context, path, BaseFile.STANDARD_FILE_CLASS)
    }
    /**
     * Instantiate a source file object representing a file on storage.
     *
     * @param path   Pathname string to the file (remote or local)
     * @param _class The class name of the new file object. This may be an existing or a new class (which will then be created)
     * @return
     */
    protected BaseFile getSourceFile(String path, String _class) {
        return BaseFile.fromStorage(context, path, _class)
    }

    /**
     * Instantiate a source file object representing a file on storage.
     *
     * @param toolID String representing a tool ID.
     * @return
     */
    protected BaseFile getSourceFileUsingTool(String toolID)
            throws ExecutionException, UnexpectedExecutionResultException {
        return getSourceFileUsingTool(toolID, BaseFile.STANDARD_FILE_CLASS)
    }

    /**
     * Instantiate a single source file object representing a file on storage.
     *
     * @param toolID String representing a tool ID.
     * @param _class The class name of the file object to return.
     * @return
     */
    protected BaseFile getSourceFileUsingTool(String toolID, String _class)
            throws ExecutionException, UnexpectedExecutionResultException {
        return BaseFile.getSourceFileUsingTool(context, toolID, _class)
    }

    /**
     * Like getSourceFileUsingTool(ExecutionContext, String) but returning multiple file objects.
     *
     * @param toolID
     * @return
     */
    protected List<BaseFile> getSourceFilesUsingTool(String toolID)
            throws ExecutionException {
        return getSourceFilesUsingTool(toolID, BaseFile.STANDARD_FILE_CLASS)
    }

    /**
     * Like getSourceFileUsingToo(ExecutionContext, String, String) but returning multiple file objects.
     *
     * @param toolID
     * @param _class
     * @return
     */
    protected List<BaseFile> getSourceFilesUsingTool(String toolID, String _class)
            throws ExecutionException {
        return BaseFile.getSourceFilesUsingTool(context, toolID, _class)
    }


    /**
     * Easily create a file which inherits from another file.
     * Will call getDerivedFile with parent and BaseFile.STANDARD_FILE_CLASS
     *
     * @param parent The file from which the new file object inherits
     * @return
     */
    protected BaseFile getDerivedFile(BaseFile parent) {
        return getDerivedFile(parent, BaseFile.STANDARD_FILE_CLASS)
    }

    /**
     * Easily create a file which inherits from another file.
     *
     * @param parent The file from which the new file object inherits
     * @param _class The class of the new file object. This may be an existing or a new class (which will then be created)
     * @return
     */
    protected BaseFile getDerivedFile(BaseFile parent, String _class) {
        return BaseFile.deriveFrom(parent, _class)
    }


    //////////////////////////////////////////////////
    // Methods will be removed at some point. With 4.0
    //////////////////////////////////////////////////

    @Deprecated
    protected BaseFile getSourceFile(ExecutionContext context, String path, String _class) {
        return BaseFile.fromStorage(context, path, _class)
    }

    @Deprecated
    protected BaseFile getSourceFile(ExecutionContext context, String path) {
        return getSourceFile(context, path, BaseFile.STANDARD_FILE_CLASS)
    }

    @Deprecated
    protected boolean getflag(ExecutionContext context, String flagID) {
        return getflag(context, flagID, true)
    }

    @Deprecated
    protected boolean getflag(ExecutionContext context, String flagID, boolean defaultValue) {
        return context.getConfiguration().getConfigurationValues().getBoolean(flagID, defaultValue)
    }

    @Deprecated
    protected BaseFile getSourceFileUsingTool(ExecutionContext context, String toolID)
            throws ExecutionException, UnexpectedExecutionResultException {
        return getSourceFileUsingTool(context, toolID, BaseFile.STANDARD_FILE_CLASS)
    }

    @Deprecated
    protected BaseFile getSourceFileUsingTool(ExecutionContext context, String toolID, String _class)
            throws ExecutionException, UnexpectedExecutionResultException {
        return BaseFile.getSourceFileUsingTool(context, toolID, _class)
    }

    @Deprecated
    protected List<BaseFile> getSourceFilesUsingTool(ExecutionContext context, String toolID, String _class)
            throws ExecutionException {
        return BaseFile.getSourceFilesUsingTool(context, toolID, _class)
    }

    @Deprecated
    protected List<BaseFile> getSourceFilesUsingTool(ExecutionContext context, String toolID)
            throws ExecutionException {
        return getSourceFilesUsingTool(context, toolID, BaseFile.STANDARD_FILE_CLASS)
    }
}
