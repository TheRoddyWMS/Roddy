package de.dkfz.roddy.execution.jobs.cluster.slurm

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSResourceProcessingCommand

/**
 * Factory for the management of slurm cluster systems.
 *
 * qstat:
 *
 * [pinkert@xgs-master0 ~]$ squeue -S M -l -o "%i %P %u %t %M %c %m %p %.55j %.55R"
 * Wed Jul 15 14:30:00 2015
 * JOBID PARTITION USER ST TIME MIN_CPUS MIN_MEMORY PRIORITY NAME                                        NODELIST(REASON)
 * 938802 xgs pinkert PD 0:00 7 250K 0.99979144753883 elprepSMD_02_H5MHTCCXX_6_AS-83829-LR-10815_Y  (ReqNodeNotAvail(Unavailable:xg
 * 939171 xgs pinkert R 10:20 2 1024 0.99979136162433 mpileup_03_H5MHTCCXX_7_AS-83830-LR-10816_12 xgs-node003
 *
 * qsub
 * sbatch --dependency=afterok:$BCL2FASTQ_2224,$PREP_83360 -w xgs-node0${NODE} -N1-1 --cpus-per-task=2 --mem=9GB --export=SAMPLE="AS-83360",TILE=2224 --job-name=bwa_${NODE}_${FC}_${LANE}_AS-83360_2224  ${TOOLS_PBS}/bwa.slurm
 *
 * qdel
 * scancel JOBID
 * --
 *      https://computing.llnl.gov/tutorials/ibm_sp/man/squeue.txt
 *
 *        %t  Job state, compact form:  PD (pending), R
 (running), CA (cancelled), CG (completing), CD
 (completed), F (failed), TO (timeout), and NF (node
 failure).  See the JOB STATE CODES section below
 for more information.

 %T  Job state, extended form: PENDING, RUNNING,
 SUSPENDED, CANCELLED, COMPLETING, COMPLETED,
 FAILED, TIMEOUT, and NODE_FAIL.  See the JOB STATE
 CODES section below for more information.
 *
 */
class SlurmJobManager extends PBSJobManager {

    @Override
    public SlurmCommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
        SlurmCommand command = new SlurmCommand(job, run, ExecutionService.getInstance(), jobName, processingCommands, parameters, arraySettings, dependencies, tool.getAbsolutePath());
        addCommandToList(command);
        return command;
    }


    @Override
    public void addSpecificSettingsToConfiguration(Configuration configuration) {
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_JOBID", '${SLURM_JOB_ID}'));
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_SCRATCH", '/tmp/roddyScratch/${SLURM_JOB_ID}'));
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_AUTOCLEANUP_SCRATCH", "true"));
    }

    @Override
    public ProcessingCommands parseProcessingCommands(String processingString) {
        return null;
    }

    @Override
    public String getResourceOptionsPrefix() {
        return "SlurmResourceOptions_";
    }

    @Override
    public ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet) {
        StringBuilder sb = new StringBuilder();
        sb.append(" -V"); //TODO Think if default SGE options should go somewhere else?
        if (resourceSet.isMemSet()) {
            Float memo = resourceSet.getMem();
            int memoryInMB = (int)(memo * 1024);
            sb.append(" --mem=").append(memoryInMB).append("MB");
        }
        if (resourceSet.isCoresSet()) {
            sb.append(" --cpus-per-task=").append(resourceSet.getCores());
        }
        if(resourceSet.isNodesSet()) {
            sb.append(" --nodes=").append(resourceSet.getCores()).append("-").append(resourceSet.getCores());
            String enforceSubmissionNodes = configuration.getConfigurationValues().getString(CVALUE_ENFORCE_SUBMISSION_TO_NODES, null);
            if (enforceSubmissionNodes) {
                sb.append(" --nodelist=").append(enforceSubmissionNodes);
            }
        }
        if(resourceSet.isWalltimeSet()) {
            sb.append(" --time=").append(resourceSet.getWalltime()).append(":00:00");
        }
        return new PBSResourceProcessingCommand(sb.toString());
    }

    @Override
    public ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        return null;
    }

    @Override
    protected String getStringForQueuedJob() {
        return "PD";
    }

    @Override
    protected String getStringForJobOnHold() {
        return "hqw";
    }

    @Override
    protected String getStringForRunningJob() {
        return "R";
    }

    @Override
    protected int getPositionOfJobID() {
        return 0;
    }

    @Override
    protected int getPositionOfJobState() {
        return 4;
    }

    @Override
    public String parseJobID(String commandOutput) {

        String id = commandOutput.split(StringConstants.SPLIT_STOP)[0];
        return id;
    }

    @Override
    public String getQueryCommand() {
        return "squeue %i %P %.55j %u %t"
    }

    protected List<String> getTestQstat() {

        //JOBID PARTITION NAME USER ST
        //953540 xgs bwa_04_H2GYVALXX_6_AS-76572-LR-9718_2212 pinkert PD
        //953190 xgs mFastq_03_H75NCCCXX_1_AS-77263-LR-10641_2118 pinkert R
        //squeue %i %P %.55j %u %t
        return Arrays.asList(
                "job - ID prior name user state submit / start at queue slots ja -task - ID",
                "---------------------------------------------------------------------------------------------------------------- -",
                "   1187 0.75000 r140710_09 seqware r 07 / 10 / 2014 09:51:55 main.q @worker3 1",
                "   1188 0.41406 r140710_09 seqware r 07 / 10 / 2014 09:51:40 main.q @worker1 1",
                "   1190 0.25000 r140710_09 seqware r 07 / 10 / 2014 09:51:55 main.q @worker2 1",
                "   1189 0.00000 r140710_09 seqware hqw 07 / 10 / 2014 09:51:27 1",
                "   1191 0.00000 r140710_09 seqware hqw 07 / 10 / 2014 09:51:48 1",
                "   1192 0.00000 r140710_09 seqware hqw 07 / 10 / 2014 09:51:48 1");
    }
}
