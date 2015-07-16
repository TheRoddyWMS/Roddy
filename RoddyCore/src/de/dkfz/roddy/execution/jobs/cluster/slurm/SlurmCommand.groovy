package de.dkfz.roddy.execution.jobs.cluster.slurm

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommand

/**
 *  * qsub
 * sbatch --dependency=afterok:$BCL2FASTQ_2224,$PREP_83360 -w xgs-node0${NODE} -N1-1 --cpus-per-task=2 --mem=9GB --export=SAMPLE="AS-83360",TILE=2224 --job-name=bwa_${NODE}_${FC}_${LANE}_AS-83360_2224  ${TOOLS_PBS}/bwa.slurm
 *
 */
class SlurmCommand extends PBSCommand {
    /**
     *
     * @param job @param run @param executionService @param id
     * @param processingCommands @param parameters
     * @param arrayIndices
     * @param dependencyIDs @param command
     *
     */
    SlurmCommand(Job job, ExecutionContext run, ExecutionService executionService, String id, List<ProcessingCommands> processingCommands, Map<String, String> parameters, List<String> arrayIndices, List<String> dependencyIDs, String command) {
        super(job, run, executionService, id, processingCommands, parameters, arrayIndices, dependencyIDs, command)
    }


}
