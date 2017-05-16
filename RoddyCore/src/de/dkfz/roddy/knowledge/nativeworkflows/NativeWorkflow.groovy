/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.nativeworkflows

import de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo as BEGenJI
import de.dkfz.eilslabs.batcheuphoria.AvailableClusterSystems
import de.dkfz.eilslabs.batcheuphoria.execution.direct.synchronousexecution.DirectCommand
import de.dkfz.eilslabs.batcheuphoria.execution.direct.synchronousexecution.DirectSynchronousExecutionJobManager
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManager
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManagerCreationParameters
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManagerCreationParametersBuilder
import de.dkfz.eilslabs.batcheuphoria.jobs.JobResult
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.ContextConfiguration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.eilslabs.batcheuphoria.jobs.Command
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic

import java.io.File
import java.lang.reflect.Constructor
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.List
import java.util.Map

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
@CompileStatic
class NativeWorkflow extends Workflow {
    static final LoggerWrapper logger = LoggerWrapper.getLogger(NativeWorkflow.class)

    @Override
    boolean execute(ExecutionContext context) {
        logger.severe("You are using the Native Workflow interface. This might be unstable")

        ExecutionContextLevel level = context.getExecutionContextLevel()

//        if (level != ExecutionContextLevel.QUERY_STATUS && level != ExecutionContextLevel.RUN && level != ExecutionContextLevel.RERUN && level != ExecutionContextLevel.TESTRERUN) {
        if (level != ExecutionContextLevel.RUN && level != ExecutionContextLevel.RERUN) {
            return true
        }
        ContextConfiguration configuration = (ContextConfiguration) context.getConfiguration()
        final AnalysisConfiguration aCfg = configuration.getAnalysisConfiguration()

        //Get the target command factory, initialize it and create an alias for the submission command (i.e. qsub)
        JobManager targetJobManager = null
        try {
            String clz = aCfg.getTargetJobManagerClass()
            ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader()
            Class<?> targetJobManagerClass = classLoader.loadClass(clz)
            Constructor c = targetJobManagerClass.getConstructor(de.dkfz.eilslabs.batcheuphoria.execution.ExecutionService.class, JobManagerCreationParameters.class)
            targetJobManager = (JobManager) c.newInstance(ExecutionService.getInstance(), new JobManagerCreationParametersBuilder().setCreateDaemon(false).build())
        } catch (NullPointerException e) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("No command factory class is set."))
            return false
        } catch (Exception e) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Wrong target command factory class is set."))
            return false
        }

        // In normal cases commands are executed by the default factory. In this case we want the command to be executed directly.
        String toolID = aCfg.getNativeToolID()
        String jobManagerAbbreviation = AvailableClusterSystems.values().find { it.className == targetJobManager.class.name }.name().toUpperCase()
        def nativeScriptID = "nativeWrapperFor${jobManagerAbbreviation}"
        String nativeWorkflowScriptWrapper = configuration.getProcessingToolPath(context, nativeScriptID).absolutePath
        Job wrapperJob = new Job(context, context.getTimestampString() + "_nativeJobWrapper:" + toolID, toolID, null)
//         Change the parameters to run the right tools
//        wrapperJob.getParameters()["WRAPPED_SCRIPT"]

        DirectSynchronousExecutionJobManager dcfac = new DirectSynchronousExecutionJobManager(ExecutionService.getInstance(), new JobManagerCreationParametersBuilder().setCreateDaemon(false).build())
//        DirectCommand wrapperJobCommand = dcfac.createCommand(wrapperJob, aCfg.getProcessingToolPath(context, toolID).absolutePath, [], new LinkedList<>())
        DirectCommand wrapperJobCommand = new DirectCommand(dcfac, wrapperJob, "some_id", [], wrapperJob.parameters, [:], [], [], nativeWorkflowScriptWrapper, new File("/tmp"))
        String submissionCommand = targetJobManager.getSubmissionCommand()
        if (submissionCommand == null) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("There is no submission command for this type of command factory."))
            return false
        }

        // Put in the submission command as an additional parameter
        String finalCommand = "SUBMISSION_COMMAND=" + submissionCommand + " " + wrapperJobCommand.toString()

        // Get the abbrevation for the job system (PBS, SGE, LSF...)

        // Replace the wrapinscript with the nativeWorkflowWrapperScript
        String wrapinScript = configuration.getProcessingToolPath(context, "wrapinScript").absolutePath
        finalCommand = finalCommand.replace(wrapinScript, nativeWorkflowScriptWrapper)
        System.out.println(wrapinScript + " => " + nativeWorkflowScriptWrapper)
        System.out.println(finalCommand)

        ExecutionResult execute = ExecutionService.getInstance().execute(finalCommand)
        logger.rare(execute.resultLines.join("\n"))
        //Get the calls file in the temp directory.
        String[] calls = FileSystemAccessProvider.getInstance().loadTextFile(new File(context.getTemporaryDirectory(), "calls"))
        Map<String, BEGenJI> callsByID = new LinkedHashMap<>()
        Map<String, String> fakeIDWithRealID = new LinkedHashMap<>()
        for (String call : calls) {
            BEGenJI jInfo = targetJobManager.parseGenericJobInfo(call);
            callsByID[jInfo.id] = jInfo;
//            jInfo.getParameters().put(PRM_TOOLS_DIR, configuration.getProcessingToolPath(context, jInfo.getToolID()).getAbsolutePath());
//            jInfo.getParameters().put(PRM_TOOL_ID, jInfo.getToolID());
        }
        for (BEGenJI jInfo : callsByID.values()) {
            if (jInfo.getParentJobIDs() != null) {
                //Replace all ids!
                List<String> newIDs = new LinkedList<>()
                List<String> parentJobIDs = jInfo.getParentJobIDs()
                for (String id : parentJobIDs) {
                    newIDs.add(fakeIDWithRealID.get(id))
                }
                jInfo.setParentJobIDs(newIDs)
            }

            Job job = new GenericJobInfo(context, jInfo).toJob()
            de.dkfz.roddy.execution.jobs.JobResult result = job.run()
            Command command = result.command
            String id = null;
            try {
                id = command.getExecutionID().getShortID();
                fakeIDWithRealID[jInfo.id] = id;
            } catch (Exception ex) {
                println(ex.getMessage())
            }
            System.out.println("\tNative: " + command.getJob().getJobName() + " => " + id);
        }

        return true
    }
}
