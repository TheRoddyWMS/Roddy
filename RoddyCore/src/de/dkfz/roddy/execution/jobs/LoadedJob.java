/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Stores information about jobs which were read out from Roddys XML file.
 */
public class LoadedJob extends ReadOutJob {

    private List<LoadedFile> loadedFiles = new LinkedList<>();

    public LoadedJob(ExecutionContext context, String jobName, String jobID, String toolID, String toolMD5, Map<String, String> parameters, List<LoadedFile> loadedFiles, List<BEJob> parentJobIDs) {
        super(context, jobName, toolID, jobID, parameters, parentJobIDs);
//        this.toolID = toolID;
        //this.toolMD5 = toolMD5;
        this.loadedFiles = loadedFiles;
    }
}
