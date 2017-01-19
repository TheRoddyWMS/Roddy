/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.direct.synchronousexecution;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.*;
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class DirectSynchronousExecutedJobManager extends JobManager<DirectCommand> {


    @Override
    public void createUpdateDaemonThread(int interval) {
        //Not necessary, a command / job knows its state in local execution
    }

    @Override
    public DirectCommand createCommand(GenericJobInfo jobInfo) {
        return null;
    }

    @Override
    public JobDependencyID createJobDependencyID(Job job, String jobResult) {
        return new DirectCommandDependencyID(jobResult, job);
    }

    @Override
    public ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet) {
        return null;
    }

    @Override
    public ProcessingCommands parseProcessingCommands(String pCmd) {
        return new DummyProcessingCommand(pCmd);
    }

    @Override
    public ProcessingCommands getProcessingCommandsFromConfiguration(Configuration configuration, String toolID) {
        return null;
    }

    @Override
    public ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        return null;
    }

    @Override
    public Job parseToJob(ExecutionContext executionContext, String commandString) {
        throw new RuntimeException("Not implemented yet! " + this.getClass().getName() + ".parseToJob()");
    }

    @Override
    public GenericJobInfo parseGenericJobInfo(ExecutionContext context, String command) {
        return null;
    }

    @Override
    public JobResult convertToArrayResult(Job arrayChildJob, JobResult parentJobsResult, int arrayIndex) {
        throw new RuntimeException("Not implemented yet! " + this.getClass().getName() + ".convertToArrayResult()");
    }

    @Override
    public void addJobStatusChangeListener(Job job) {

    }

    @Override
    public String getLogFileWildcard(Job job) {
        return "*";
    }

    @Override
    public boolean compareJobIDs(String jobID, String id) {
        return jobID.equals(id);
    }

    @Override
    public String[] peekLogFile(Job job) {
        return new String[0];
    }

    @Override
    public void queryJobAbortion(List executedJobs) {
        //TODO something with kill
    }

    @Override
    public Map<String, JobState> queryJobStatus(List jobIDs) {
        return new LinkedHashMap<>();
    }

    @Override
    public DirectCommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
        return new DirectCommand(job, run, jobName, processingCommands, parameters, dependencies, arraySettings, tool.getAbsolutePath());
    }

    @Override
    public boolean executesWithoutJobSystem() {
        return true;
    }

    @Override
    public String parseJobID(String commandOutput) {
        return commandOutput;
    }
    //    @Override
//    public DirectCommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
//
//    }

    @Override
    public String getSubmissionCommand() {
        return null;
    }
}
