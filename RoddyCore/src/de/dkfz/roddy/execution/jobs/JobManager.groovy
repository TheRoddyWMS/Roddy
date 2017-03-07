/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet
import de.dkfz.eilslabs.batcheuphoria.execution.ExecutionService
import de.dkfz.eilslabs.batcheuphoria.jobs.Command
import de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManagerCreationParameters
import de.dkfz.eilslabs.batcheuphoria.jobs.JobState
import de.dkfz.eilslabs.batcheuphoria.jobs.ProcessingCommands
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.knowledge.files.BaseFile

/**
 * (Proxy) Compatibility class for 2.3.x until 2.4
 * Created by heinold on 06.03.17.
 */
@Deprecated()
class JobManager {

    static JobManager getInstance() {
        return new JobManager(Roddy.getJobManager())
    }

    String createJobName(BaseFile baseFile, String toolID, boolean reduceLevel) {
        return RuntimeService._createJobName(baseFile.executionContext, baseFile, toolID, reduceLevel)
    }

    Command createCommand(GenericJobInfo jobInfo) {
        getInstance().createCommand(jobInfo)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.JobResult runJob(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, boolean runDummy) {
        instance.runJob(job, runDummy)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.JobDependencyID createJobDependencyID(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String jobResult) {
        instance.createJobDependencyID(job, jobResult)
    }

    ProcessingCommands convertResourceSet(ResourceSet resourceSet) {
        instance.convertResourceSet(resourceSet)
    }

    ProcessingCommands parseProcessingCommands(String alignmentProcessingOptions) {
        instance.parseProcessingCommands(alignmentProcessingOptions)
    }

    ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        instance.extractProcessingCommandsFromToolScript(file)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.Job parseToJob(String commandString) {
        instance.parseToJob(commandString)
    }

    GenericJobInfo parseGenericJobInfo(String command) {
        instance.parseGenericJobInfo(command)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.JobResult convertToArrayResult(de.dkfz.eilslabs.batcheuphoria.jobs.Job arrayChildJob, de.dkfz.eilslabs.batcheuphoria.jobs.JobResult parentJobsResult, int arrayIndex) {
        instance.convertToArrayResult(arrayChildJob, parentJobsResult, arrayIndex)
    }

    void updateJobStatus() {
        instance.updateJobStatus()
    }

    void addJobStatusChangeListener(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        instance.addJobStatusChangeListener(job)
    }

    String getLogFileWildcard(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        instance.getLogFileWildcard(job)
    }

    boolean compareJobIDs(String jobID, String id) {
        instance.compareJobIDs(jobID, id)
    }

    String getStringForQueuedJob() {
        instance.getStringForQueuedJob()
    }

    String getStringForJobOnHold() {
        instance.getStringForJobOnHold()
    }

    String getStringForRunningJob() {
        instance.getStringForRunningJob()
    }

    String getSpecificJobIDIdentifier() {
        instance.getSpecificJobIDIdentifier()
    }

    String getSpecificJobArrayIndexIdentifier() {
        instance.getSpecificJobArrayIndexIdentifier()
    }

    String getSpecificJobScratchIdentifier() {
        instance.getSpecificJobScratchIdentifier()
    }

    String[] peekLogFile(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        instance.peekLogFile(job)
    }

    String parseJobID(String commandOutput) {
        instance.parseToJob(commandOutput)
    }

    String getSubmissionCommand() {
        instance.getSubmissionCommand()
    }

    void queryJobAbortion(List executedJobs) {
        instance.queryJobAbortion(executedJobs)
    }

    Map<String, JobState> queryJobStatus(List jobIDs) {
        instance.queryJobStatus(jobIDs)
    }

    Command createCommand(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String jobName, List processingCommands, File tool, Map parameters, List dependencies, List arraySettings) {
        instance.createCommand(job, jobName, processingCommands, tool, parameters, dependencies, arraySettings)
    }

    boolean executesWithoutJobSystem() {
        return instance.executesWithoutJobSystem()
    }
}
