package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.LoadedFile;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Stores information about jobs which were read out from Roddys XML file.
 */
public class LoadedJob extends ReadOutJob {

    private List<LoadedFile> loadedFiles = new LinkedList<>();

    public LoadedJob(ExecutionContext run, String jobName, String jobID, String toolID, String toolMD5, Map<String, String> parameters, List<LoadedFile> loadedFiles, List<String> parentJobIDs) {
        super(run, jobName, toolID, jobID, parameters, parentJobIDs);
        this.toolID = toolID;
        this.toolMD5 = toolMD5;
        this.loadedFiles = loadedFiles;
    }
}
