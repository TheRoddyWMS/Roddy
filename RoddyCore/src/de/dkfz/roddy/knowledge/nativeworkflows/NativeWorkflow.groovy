/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.nativeworkflows

import de.dkfz.roddy.AvailableClusterSystems
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.ContextConfiguration
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.execution.BEExecutionService
import de.dkfz.roddy.execution.Executable
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.GenericJobInfo as BEGenJI
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectCommand
import de.dkfz.roddy.execution.jobs.direct.synchronousexecution.DirectSynchronousExecutionJobManager
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic

import java.lang.reflect.Constructor
import java.nio.file.Paths

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

    /**
     * A custom class which allows setting of parent jobs.
     */
    class NativeJob extends Job {
        NativeJob(Job wrappedJob) {
            super(wrappedJob.context,
                  wrappedJob.jobName,
                  wrappedJob.toolCommand,
                  wrappedJob.allRawInputParameters,
                  wrappedJob.parentFiles,
                  wrappedJob.filesToVerify)
        }

        void setNativeParentJobs(List<NativeJob> nativeParentJobs) {
            super.parentJobs.addAll(nativeParentJobs)
        }
    }

    @Override
    boolean execute(ExecutionContext context) {
        logger.severe("You are using the Native Workflow interface. This might be unstable")

        ExecutionContextLevel level = context.executionContextLevel

        if (level != ExecutionContextLevel.RUN && level != ExecutionContextLevel.RERUN) {
            logger.always("The run mode ${level} is not allowed for native workflows.")
            return true
        }

        //Get the target command factory, initialize it and create an alias for the submission command (i.e. qsub)
        BatchEuphoriaJobManager targetJobManager = tryLoadTargetJobManager(context)

        if (!targetJobManager) return false

        if (!callAndInterceptNativeWorkflow(context, targetJobManager)) return false

        Map<String, BEGenJI> callsByID = fetchAndProcessCalls(context, targetJobManager)

        convertJobInfoAndRunJobs(callsByID, context)

        return true
    }

    BatchEuphoriaJobManager tryLoadTargetJobManager(ExecutionContext context) {
        ContextConfiguration configuration = (ContextConfiguration) context.configuration
        AnalysisConfiguration aCfg = configuration.getAnalysisConfiguration()

        try {
            String clz = aCfg.getTargetJobManagerClass()
            ClassLoader classLoader = LibrariesFactory.getGroovyClassLoader()
            Class<?> targetJobManagerClass = classLoader.loadClass(clz)
            Constructor c = targetJobManagerClass.getConstructor(BEExecutionService.class, JobManagerOptions.class)
            return (BatchEuphoriaJobManager) c.newInstance(ExecutionService.getInstance(), JobManagerOptions.create().build())
        } catch (NullPointerException e) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("No command factory class is set."))
            return null
        } catch (Exception e) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Wrong target command factory class is set."))
            return null
        }
    }

    boolean callAndInterceptNativeWorkflow(ExecutionContext context, BatchEuphoriaJobManager targetJobManager) {
        ContextConfiguration configuration = (ContextConfiguration) context.configuration
        AnalysisConfiguration aCfg = configuration.getAnalysisConfiguration()

        // In normal cases commands are executed by the default factory. In this case we want the command to be
        // executed directly.
        String toolID = aCfg.getNativeToolID()
        String jobManagerAbbreviation = AvailableClusterSystems.values().find {
            it.className == targetJobManager.class.name
        }.name().toUpperCase()
        def nativeScriptID = "nativeWrapperFor${jobManagerAbbreviation}"
        String nativeWorkflowScriptWrapper = configuration.getExecutedToolPath(context, nativeScriptID).absolutePath
        Job wrapperJob = new Job(
                context,
                context.getTimestampString() + "_nativeJobWrapper:" + toolID,
                context.getToolCommand(toolID),
                null,
                null,
                null)

        DirectSynchronousExecutionJobManager dcfac = new DirectSynchronousExecutionJobManager(
                ExecutionService.instance, JobManagerOptions.create().build())
        DirectCommand wrapperJobCommand = new DirectCommand(dcfac, wrapperJob, [])
        String submissionCommand = targetJobManager.submissionCommand
        if (submissionCommand == null) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("There is no submission command for this type of command factory."))
            return false
        }

        // Put in the submission command as an additional parameter
        String finalCommand = "SUBMISSION_COMMAND=" + submissionCommand + " " + wrapperJobCommand.toString()

        // Replace the wrapinscript with the nativeWorkflowWrapperScript
        String wrapinScript = configuration.getExecutedToolPath(context, "wrapinScript").absolutePath
        finalCommand = finalCommand.replace(wrapinScript, nativeWorkflowScriptWrapper)
        System.out.println(wrapinScript + " => " + nativeWorkflowScriptWrapper)
        System.out.println(finalCommand)

        ExecutionResult execute = ExecutionService.instance.
                execute(finalCommand, true)
        logger.rare(execute.stdout.join("\n"))
        return true
    }

    private Map<String, BEGenJI> fetchAndProcessCalls(ExecutionContext context, BatchEuphoriaJobManager targetJobManager) {

        ContextConfiguration configuration = (ContextConfiguration) context.configuration
        //Get the calls file in the temp directory.
        String[] calls = FileSystemAccessProvider.instance.
                loadTextFile(new File(context.temporaryDirectory, "calls"))
        if (calls == null)
            calls = new String[0]

        Map<String, BEGenJI> callsByID = new LinkedHashMap<>()

        def inlineScriptsDir = RoddyIOHelperMethods.assembleLocalPath(context.executionDirectory, "analysisTools", "inlineScripts")
        File[] inlineScripts = FileSystemAccessProvider.instance.checkDirectory(inlineScriptsDir, false) ?
                FileSystemAccessProvider.instance.listFilesInDirectory(inlineScriptsDir) :
                null
        if (!inlineScripts) inlineScripts = new File[0]

        Map<String, List<String>> virtualDependencies = [:]

        // Read in all calls and check if there is an inline script (and store that)
        for (String call : calls) {
            BEGenJI jInfo = targetJobManager.parseGenericJobInfo(call);
            callsByID[jInfo.jobID.toString()] = jInfo;
            virtualDependencies[jInfo.jobID.toString()] = jInfo.parentJobIDs

            File iScript = inlineScripts.find { File file -> file.name.endsWith(jInfo.jobID.toString()) }
            if (!iScript) continue

            configuration.tools.add(new ToolEntry(iScript.name.replace(".", "_"), "inlineScripts", iScript.name))
            jInfo.setTool(iScript)
        }
        callsByID
    }

    private void convertJobInfoAndRunJobs(Map<String, de.dkfz.roddy.execution.jobs.GenericJobInfo> callsByID, ExecutionContext context) {
        List<NativeJob> convertedJobs = []
        Map<NativeJob, BEGenJI> jInfoByJob = [:]
        Map<String, NativeJob> jobsByVirtualID = [:]

        for (BEGenJI jInfo : callsByID.values()) {
            def convertedJob = new NativeJob(new GenericJobInfo(context, jInfo).toJob())
            convertedJobs << convertedJob
            jobsByVirtualID[jInfo.jobID.toString()] = convertedJob
            jInfoByJob[convertedJob] = jInfo
        }

        for (NativeJob convertedJob : convertedJobs) {
            List<NativeJob> jobs = jInfoByJob[convertedJob].parentJobIDs.collect {
                jobsByVirtualID[it]
            }
            convertedJob.nativeParentJobs = jobs
        }

        for (Job job in convertedJobs) {
            BEJobResult result = job.run()
            BECommand command = result.beCommand
            String id = null;
            try {
                id = command.jobID.shortID
            } catch (Exception ex) {
                println(ex.message)
            }
            System.out.println("\tNative: " + command.job.jobName + " => " + id);
        }
    }
}
