package de.dkfz.roddy.knowledge.nativeworkflows;

import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.config.ContextConfiguration;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.execution.io.ExecutionResult;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.Command;
import de.dkfz.roddy.execution.jobs.CommandFactory;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectCommand;
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutedCommandFactory;
import de.dkfz.roddy.plugins.LibrariesFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.dkfz.roddy.execution.jobs.JobConstants.PRM_TOOLS_DIR;
import static de.dkfz.roddy.execution.jobs.JobConstants.PRM_TOOL_ID;

/**
 * A Native Workflow encapsulates a (shell /perl /python) script which submits cluster jobs.
 * The analysis configuration needs to know about the target system.
 * Upon execution, the workflow script is called at the target machine.
 * The script is encapsulated in a wrapper script and an alias is created for the job calls.
 * This workflow type should make it much easier to import an existing workflow based on
 * i.e. shell scripts but also has drawbacks. The most important drawback is, that the user
 * has to take care for rerun on its own.
 * Created by michael on 06.02.15.
 */
public class NativeWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {

        ExecutionContextLevel level = context.getExecutionContextLevel();

//        if (level != ExecutionContextLevel.QUERY_STATUS && level != ExecutionContextLevel.RUN && level != ExecutionContextLevel.RERUN && level != ExecutionContextLevel.TESTRERUN) {
        if (level != ExecutionContextLevel.RUN && level != ExecutionContextLevel.RERUN) {
            return true;
        }
        ContextConfiguration configuration = (ContextConfiguration) context.getConfiguration();
        AnalysisConfiguration aCfg = configuration.getAnalysisConfiguration();

        //Get the target command factory, initialize it and create an alias for the submission command (i.e. qsub)
        CommandFactory targetCommandFactory = null;
        try {
            String clz = aCfg.getTargetCommandFactoryClass();
            ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader();
            Class<?> targetCommandFactoryClass = classLoader.loadClass(clz);
            Constructor[] c = targetCommandFactoryClass.getConstructors();
            Constructor first = c[0];
            targetCommandFactory = (CommandFactory) first.newInstance();
        } catch (Exception e) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Wrong target command factory class is set."));
            return false;
        }

        // In normal cases commands are executed by the default factory. In this case we want the command to be executed directly.
        String toolID = aCfg.getNativeToolID();
        Job wrapperJob = new Job(context, context.getTimeStampString() + "_nativeJobWrapper:" + toolID, toolID, null);

//        <tool name='wrapinScript' value='wrapInScript.sh' basepath='roddyTools'/>
//        <tool name='nativeWorkflowScriptWrapper' value='nativeWorkflowScriptWrapper.sh' basepath='roddyTools'/>

        DirectSynchronousExecutedCommandFactory dcfac = new DirectSynchronousExecutedCommandFactory();
        DirectCommand wrapperJobCommand = dcfac.createCommand(wrapperJob, aCfg.getProcessingToolPath(context, toolID), new LinkedList<>());
        String submissionCommand = targetCommandFactory.getSubmissionCommand();
        if (submissionCommand == null) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("There is no submission command for this type of command factory."));
            return false;
        }

        //Put in the submission command as an additional parameter
        String finalCommand = "SUBMISSION_COMMAND=" + submissionCommand + " " + wrapperJobCommand.toString();

        //Replace the wrapinscript with the nativeWorkflowWrapperScript
        String wrapinScript = configuration.getProcessingToolPath(context, "wrapinScript").getAbsolutePath();
        String nativeWorkflowScriptWrapper = configuration.getProcessingToolPath(context, "nativeWorkflowScriptWrapper").getAbsolutePath();
        finalCommand = finalCommand.replace(wrapinScript, nativeWorkflowScriptWrapper);
        System.out.println(wrapinScript + " => " + nativeWorkflowScriptWrapper);
        System.out.println(finalCommand);

        ExecutionResult execute = ExecutionService.getInstance().execute(finalCommand);
        //Get the calls file in the temp directory.
        String[] calls = FileSystemAccessProvider.getInstance().loadTextFile(new File(context.getTemporaryDirectory(), "calls"));
        Map<String, GenericJobInfo> callsByID = new LinkedHashMap<>();
        Map<String, String> fakeIDWithRealID = new LinkedHashMap<>();
        for (String call : calls) {
            GenericJobInfo jInfo = targetCommandFactory.parseGenericJobInfo(context, call);
            callsByID.put(jInfo.getID(), jInfo);
            jInfo.getParameters().put(PRM_TOOLS_DIR, configuration.getProcessingToolPath(context, jInfo.getToolID()).getAbsolutePath());
            jInfo.getParameters().put(PRM_TOOL_ID, jInfo.getToolID());
        }
        for (GenericJobInfo jInfo : callsByID.values()) {
            if(jInfo.getParentJobIDs() != null) {
                //Replace all ids!
                List<String> newIDs = new LinkedList<>();
                List<String> parentJobIDs = jInfo.getParentJobIDs();
                for(String id : parentJobIDs) {
                    newIDs.add(fakeIDWithRealID.get(id));
                }
                jInfo.setParentJobIDs(newIDs);
            }

            Command command = CommandFactory.getInstance().createCommand(jInfo);
            ExecutionService.getInstance().execute(command);
            String id = null;
            try {
                id = command.getExecutionID().getShortID();
                fakeIDWithRealID.put(jInfo.getID(), id);
            } catch(Exception ex) {

            }
            System.out.println("\tNative: " + command.getJob().getJobName() + " => " + id);
        }

        return true;
    }
}
