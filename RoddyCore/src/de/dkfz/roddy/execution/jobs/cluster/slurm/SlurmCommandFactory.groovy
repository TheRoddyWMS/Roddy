package de.dkfz.roddy.execution.jobs.cluster.slurm

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobDependencyID
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.execution.jobs.cluster.ClusterCommandFactory
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo

/**
 * Factory for the management of slurm cluster systems.
 *
 * qstat:
 *
 * [pinkert@xgs-master0 ~]$ squeue -S M -l -o "%i %P %u %t %M %c %m %p %.55j %.55R"
 * Wed Jul 15 14:30:00 2015
 * JOBID PARTITION USER ST TIME MIN_CPUS MIN_MEMORY PRIORITY NAME                                        NODELIST(REASON)
 * 938802 xgs pinkert PD 0:00 7 250K 0.99979144753883 elprepSMD_02_H5MHTCCXX_6_AS-83829-LR-10815_Y  (ReqNodeNotAvail(Unavailable:xg
 * ...
 * 939171 xgs pinkert R 10:20 2 1024 0.99979136162433 mpileup_03_H5MHTCCXX_7_AS-83830-LR-10816_12 xgs-node003
 *
 * qsub
 * sbatch --dependency=afterok:$BCL2FASTQ_2224,$PREP_83360 -w xgs-node0${NODE} -N1-1 --cpus-per-task=2 --mem=9GB --export=SAMPLE="AS-83360",TILE=2224 --job-name=bwa_${NODE}_${FC}_${LANE}_AS-83360_2224  ${TOOLS_PBS}/bwa.slurm
 *
 * qdel
 * scancel JOBID
 * --
 *
 *
 */
class SlurmCommandFactory extends ClusterCommandFactory<SlurmCommand> {
    @Override
    void createUpdateDaemonThread(int interval) {

    }

    @Override
    SlurmCommand createCommand(GenericJobInfo jobInfo) {
        return null
    }

    @Override
    SlurmCommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
        return null
    }

    @Override
    JobDependencyID createJobDependencyID(Job job, String jobResult) {
        return null
    }

    @Override
    ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet) {
        return null
    }

    @Override
    ProcessingCommands parseProcessingCommands(String alignmentProcessingOptions) {
        return null
    }

    @Override
    ProcessingCommands getProcessingCommandsFromConfiguration(Configuration configuration, String toolID) {
        return null
    }

    @Override
    ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        return null
    }

    @Override
    Job parseToJob(ExecutionContext executionContext, String commandString) {
        return null
    }

    @Override
    GenericJobInfo parseGenericJobInfo(ExecutionContext context, String command) {
        return null
    }

    @Override
    JobResult convertToArrayResult(Job arrayChildJob, JobResult parentJobsResult, int arrayIndex) {
        return null
    }

    @Override
    Map<String, JobState> queryJobStatus(List<String> jobIDs) {
        return null
    }

    @Override
    void queryJobAbortion(List<Job> executedJobs) {

    }

    @Override
    void addJobStatusChangeListener(Job job) {

    }

    @Override
    String getLogFileWildcard(Job job) {
        return null
    }

    @Override
    boolean compareJobIDs(String jobID, String id) {
        return false
    }

    @Override
    String[] peekLogFile(Job job) {
        return new String[0]
    }

    @Override
    String parseJobID(String commandOutput) {
        return null
    }

    @Override
    String getSubmissionCommand() {
        return null
    }
}
