package de.dkfz.roddy.execution.jobs.cluster.sge;

import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.ProcessingCommands;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommandFactory;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSResourceProcessingCommand;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by michael on 20.05.14.
 */
public class SGECommandFactory extends PBSCommandFactory {
    @Override
    public SGECommand createCommand(Job job, ExecutionContext run, String jobName, List<ProcessingCommands> processingCommands, File tool, Map<String, String> parameters, List<String> dependencies, List<String> arraySettings) {
        SGECommand command = new SGECommand(job, run, ExecutionService.getInstance(), jobName, processingCommands, parameters, arraySettings, dependencies, tool.getAbsolutePath());
        addCommandToList(command);
        return command;
    }


    @Override
    public void addSpecificSettingsToConfiguration(Configuration configuration) {
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_JOBID", "${JOB_ID-}"));
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_SCRATCH", "/tmp/roddyScratch/${JOB_ID}"));
        configuration.getConfigurationValues().add(new ConfigurationValue("RODDY_AUTOCLEANUP_SCRATCH", "true"));
    }

    @Override
    public ProcessingCommands parseProcessingCommands(String processingString) {
        return null;
    }

    @Override
    public String getResourceOptionsPrefix() {
        return "SGEResourceOptions_";
    }

    @Override
    public ProcessingCommands convertResourceSet(Configuration configuration, ToolEntry.ResourceSet resourceSet) {
        // "-l mf=4G -l h_vmem=6G -l h_stack=128M -V"
        StringBuilder sb = new StringBuilder();
        sb.append(" -V"); //TODO Think if default SGE options should go somewhere else?
        if (resourceSet.isMemSet()) {
            Float memo = resourceSet.getMem();
            int memoryInMB = (int)(memo * 1024);
            String memfield = configuration.getConfigurationValues().getString("SGEDefaultMemoryResource", "s_data");
            sb.append(" -l ").append(memfield).append("=").append(memoryInMB).append("M");
        }
//        if (resourceSet.isCoresSet() && resourceSet.isNodesSet()) {
//            sb.append(" -l nodes=").append(resourceSet.getNodes()).append(":ppn=").append(resourceSet.getCores());
//        }
//        if(resourceSet.isWalltimeSet()) {
//            sb.append(" -l walltime=").append(resourceSet.getWalltime()).append(":00:00");
//        }
        if (resourceSet.isStorageSet()) {
//            sb.append(" -l mem=").append(resourceSet.getMem()).append("g");
        }
        return new PBSResourceProcessingCommand(sb.toString());
    }

    @Override
    public ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
        return null;
    }

    @Override
    protected String getStringForQueuedJob() {
        return "qw";
    }

    @Override
    protected String getStringForJobOnHold() {
        return "hqw";
    }

    @Override
    protected String getStringForRunningJob() {
        return "r";
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
        if(!commandOutput.startsWith("Your job"))
            return null;
        String id = commandOutput.split(StringConstants.SPLIT_WHITESPACE)[2];
        return id;
    }

    protected List<String> getTestQstat() {
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
