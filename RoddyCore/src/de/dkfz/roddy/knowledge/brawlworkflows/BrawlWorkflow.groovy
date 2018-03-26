package de.dkfz.roddy.knowledge.brawlworkflows;

import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.execution.UnexpectedExecutionResultException
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.methods.GenericMethod
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.util.concurrent.ExecutionException
import java.util.logging.Level

@CompileStatic
class BrawlWorkflow extends Workflow {

    final static LoggerWrapper logger = LoggerWrapper.getLogger(BrawlWorkflow)

    final BrawlWorkflow THIS = this

    /**
     * The context in which this Brawl workflow will run.
     */
    ExecutionContext context

    /**
     * This is more or less the execute method of a normal workflow. It is set by the DSL method explicit.
     * Additionally we might implement implicit workflows, which will then be used to run things automatically.
     * Currently, however, we only support the strict explicit syntax. (aka you tell Roddy what to do)
     */
    Closure explicitWorkflow


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

    /**
     * This is used to create ToolEntry objects for the workflow configuration
     * @param name
     * @param script
     * @return
     */
    BrawlWorkflowToolCall rule(
            String name,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = BrawlWorkflowToolCall) Closure script
    ) {
        if (name == null)
            throw new ConfigurationError("A rule in your workflow script does not have a name.", "BRAWLWORKFLOW")

        def rule = new BrawlWorkflowToolCall(THIS, name)
        script.resolveStrategy = Closure.DELEGATE_FIRST
        script.delegate = rule
        script()
        context.configuration.tools << rule.toToolEntry()
        rule
    }

    BrawlWorkflowToolCall tool(String name, @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = BrawlWorkflowToolCall) Closure script
    ) {
        rule(name, script)
    }

    /**
     * Run the workflow
     * @param context
     * @return
     * @throws ConfigurationError
     */
    @Override
    boolean execute(ExecutionContext context) throws ConfigurationError {
        if (explicitWorkflow)
            explicitWorkflow.call()
        else {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SCRIPT_INVALID.expand("Implicit Brawl workflows are not yet supported. You have to use the explicit method to create the workflow.", Level.SEVERE))
            throw new NotImplementedException()
        }
    }

    /**
     * Set the explicit execute closure.
     * @param script
     */
    void explicit(Closure script) {
        explicitWorkflow = script
    }

    /**
     * Add a "generic" configuration value.
     * @param id
     * @param value
     * @param type
     */
    void cvalue(String id, String value, String type) {
        context.configurationValues << new ConfigurationValue(context.configuration, id, value, type)
    }

    /**
     * Add an integer configuration value
     * @param id
     * @param value
     */
    void cvalue(String id, int value) {
        context.configurationValues << new ConfigurationValue(context.configuration, id, value.toString(), "integer")
    }

    /**
     * Add a float configuration value
     * @param id
     * @param value
     */
    void cvalue(String id, float value) {
        context.configurationValues << new ConfigurationValue(context.configuration, id, value.toString(), "float")
    }

    /**
     * Add a double configuration value
     * @param id
     * @param value
     */
    void cvalue(String id, double value) {
        context.configurationValues << new ConfigurationValue(context.configuration, id, value.toString(), "double")
    }

    /**
     * Add a boolean configuration value
     * @param id
     * @param value
     */
    void cvalue(String id, boolean value) {
        context.configurationValues << new ConfigurationValue(context.configuration, id, value.toString(), "boolean")
    }

    /**
     * Directly copied from Workflow class... Seems Idea or Groovy does not like direct calls of the methods from super
     * Ok, to make things easier, "ExecutionContext context, " was removed from all the method headers.
     *
     * IMPORTANT: The name differs from the name in Workflow! Originally I wanted to have the same syntax there BUT:
     * groovy.lang.MissingMethodException: No signature of method: roddy2910343181550689460brawlTemp$_run_closure1$_closure2.call() is applicable for argument types: (java.lang.String, de.dkfz.roddy.synthetic.files.StandardRoddyFileClass) values: [ToolB, BaseFile of type de.dkfz.roddy.synthetic.files.StandardRoddyFileClass with path /tmp/aFile]
     Possible solutions: doCall(), any(), any(), collect(), grep(), find()
     * This error always popped up when I tried to test the workflow using call(). _call(), run() etc. all worked.
     *
     */
    @CompileDynamic
    protected FileObject run(String toolName, BaseFile input, Object... additionalInput) {
        return GenericMethod.callGenericTool(toolName, input, additionalInput);
    }

    @CompileDynamic
    protected FileGroup runWithOutputFileGroup(String toolName, BaseFile input, int numericCount, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, numericCount, additionalInput);
    }

    @CompileDynamic
    protected FileGroup runWithOutputFileGroup(String toolName, BaseFile input, List<String> indices, Object... additionalInput) {
        return GenericMethod.callGenericToolWithFileGroupOutput(toolName, input, indices, additionalInput);
    }

    public List<String> runSynchronized(String toolID, Map<String, Object> parameters) {
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

