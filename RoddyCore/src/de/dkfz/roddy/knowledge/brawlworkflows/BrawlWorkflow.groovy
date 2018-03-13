package de.dkfz.roddy.knowledge.brawlworkflows;

import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.execution.UnexpectedExecutionResultException
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.methods.GenericMethod
import groovy.transform.CompileStatic

import java.util.concurrent.ExecutionException

@CompileStatic
class BrawlWorkflow extends Workflow {

    final BrawlWorkflow THIS = this

    ExecutionContext context

    static BrawlWorkflow create(
            ExecutionContext context,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = BrawlWorkflow) Closure script
    ) {
        def workflow = new BrawlWorkflow()
        workflow.context = context
        script.resolveStrategy = Closure.DELEGATE_FIRST
        script.delegate = workflow
        script()
        return workflow
    }

    BrawlWorkflowToolCall rule(
            String name,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = BrawlWorkflowToolCall) Closure script
    ) {
        def rule = new BrawlWorkflowToolCall(THIS, name)
        script.resolveStrategy = Closure.DELEGATE_FIRST
        script.delegate = rule
        script()
        context.configuration.tools << rule.toToolEntry()
        rule
    }

    @Override
    boolean execute(ExecutionContext context) throws ConfigurationError {
        // execute will not be used
        return false
    }

    void explicit(Closure script) {
        // this will be called by the dsl caller.
        script()
    }

    /**
     * Directly copied from Workflow class... Seems Idea or Groovy does not like direct calls of the methods from super
     * Ok, to make things easier, "ExecutionContext context, " was removed from all the method headers.
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

    public List<String> callSynchronized(String toolID, Map<String, Object> parameters) {
        return ExecutionService.getInstance().callSynchronized(context, toolID, parameters);
    }

    protected boolean getflag(String flagID) {
        return getflag(context, flagID, true);
    }

    protected boolean getflag(String flagID, boolean defaultValue) {
        return context.getConfiguration().getConfigurationValues().getBoolean(flagID, defaultValue);
    }

    protected BaseFile getSourceFile(String path) {
        return getSourceFile(context, path, BaseFile.STANDARD_FILE_CLASS);
    }

    protected BaseFile getSourceFile(String path, String _class) {
        return BaseFile.fromStorage(context, path, _class);
    }

    protected BaseFile getSourceFileUsingTool(String toolID)
            throws ExecutionException, UnexpectedExecutionResultException {
        return getSourceFileUsingTool(context, toolID, BaseFile.STANDARD_FILE_CLASS);
    }

    protected BaseFile getSourceFileUsingTool(String toolID, String _class)
            throws ExecutionException, UnexpectedExecutionResultException {
        return BaseFile.getSourceFileUsingTool(context, toolID, _class);
    }

    protected List<BaseFile> getSourceFilesUsingTool(String toolID)
            throws ExecutionException {
        return getSourceFilesUsingTool(context, toolID, BaseFile.STANDARD_FILE_CLASS);
    }

    protected List<BaseFile> getSourceFilesUsingTool(String toolID, String _class)
            throws ExecutionException {
        return BaseFile.getSourceFilesUsingTool(context, toolID, _class);
    }

    protected BaseFile getDerivedFile(BaseFile parent) {
        return getDerivedFile(parent, BaseFile.STANDARD_FILE_CLASS);
    }

    protected BaseFile getDerivedFile(BaseFile parent, String _class) {
        return BaseFile.deriveFrom(parent, _class);
    }
}

