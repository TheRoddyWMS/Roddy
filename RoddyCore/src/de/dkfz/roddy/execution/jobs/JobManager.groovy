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
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * (Proxy) Compatibility class for 2.3.x until 2.4
 * Created by heinold on 06.03.17.
 */
@Deprecated()
class JobManager {

    private de.dkfz.eilslabs.batcheuphoria.jobs.JobManager jobManager
    
    static JobManager getInstance() {
        return new JobManager(Roddy.getJobManager())
    }

    JobManager(de.dkfz.eilslabs.batcheuphoria.jobs.JobManager jobManager) {
        this.jobManager = jobManager
    }

    String createJobName(BaseFile baseFile, String toolID, boolean reduceLevel) {
        return RuntimeService._createJobName(baseFile.executionContext, baseFile, toolID, reduceLevel)
    }

    Command createCommand(GenericJobInfo jobInfo) {
        jobManager.createCommand(jobInfo)
    }

    de.dkfz.roddy.execution.jobs.JobResult runJob(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, boolean runDummy) {
        jobManager.runJob(job, runDummy)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.JobDependencyID createJobDependencyID(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String jobResult) {
        jobManager.createJobDependencyID(job, jobResult)
    }

    ProcessingCommands convertResourceSet(ResourceSet resourceSet) {
        jobManager.convertResourceSet(resourceSet)
    }

    ProcessingCommands parseProcessingCommands(String alignmentProcessingOptions) {
        jobManager.parseProcessingCommands(alignmentProcessingOptions)
    }

    ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        jobManager.extractProcessingCommandsFromToolScript(file)
    }

    de.dkfz.eilslabs.batcheuphoria.jobs.Job parseToJob(String commandString) {
        jobManager.parseToJob(commandString)
    }

    GenericJobInfo parseGenericJobInfo(String command) {
        jobManager.parseGenericJobInfo(command)
    }

    de.dkfz.roddy.execution.jobs.JobResult convertToArrayResult(de.dkfz.eilslabs.batcheuphoria.jobs.Job arrayChildJob, de.dkfz.roddy.execution.jobs.JobResult parentJobsResult, int arrayIndex) {
        jobManager.convertToArrayResult(arrayChildJob, parentJobsResult, arrayIndex)
    }

    void storeJobStateInfo(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
//        jobManager.store
        throw new NotImplementedException()
    }

    void updateJobStatus() {
        jobManager.updateJobStatus()
    }

    void addJobStatusChangeListener(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        jobManager.addJobStatusChangeListener(job)
    }

    String getLogFileWildcard(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        jobManager.getLogFileWildcard(job)
    }

    boolean compareJobIDs(String jobID, String id) {
        jobManager.compareJobIDs(jobID, id)
    }

    String getStringForQueuedJob() {
        jobManager.getStringForQueuedJob()
    }

    String getStringForJobOnHold() {
        jobManager.getStringForJobOnHold()
    }

    String getStringForRunningJob() {
        jobManager.getStringForRunningJob()
    }

    String getSpecificJobIDIdentifier() {
        jobManager.getSpecificJobIDIdentifier()
    }

    String getSpecificJobArrayIndexIdentifier() {
        jobManager.getSpecificJobArrayIndexIdentifier()
    }

    String getSpecificJobScratchIdentifier() {
        jobManager.getSpecificJobScratchIdentifier()
    }

    String[] peekLogFile(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
        jobManager.peekLogFile(job)
    }

    String parseJobID(String commandOutput) {
        jobManager.parseJobID(commandOutput)
    }

    String getSubmissionCommand() {
        jobManager.getSubmissionCommand()
    }

    void queryJobAbortion(List executedJobs) {
        jobManager.queryJobAbortion(executedJobs)
    }

    Map<String, JobState> queryJobStatus(List jobIDs) {
        jobManager.queryJobStatus(jobIDs)
    }

    Command createCommand(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String jobName, List processingCommands, File tool, Map parameters, List dependencies, List arraySettings) {
        jobManager.createCommand(job, jobName, processingCommands, tool, parameters, dependencies, arraySettings)
    }

    boolean executesWithoutJobSystem() {
        return jobManager.executesWithoutJobSystem()
    }
}
