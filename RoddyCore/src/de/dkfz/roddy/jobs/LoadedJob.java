/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.jobs;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Stores information about jobs which were read out from Roddys XML file.
 */
public class LoadedJob extends ReadOutJob {

    private List<LoadedFile> loadedFiles = new LinkedList<>();

    public LoadedJob(String jobName, String jobID, String toolID, String toolMD5, Map<String, String> parameters, List<LoadedFile> loadedFiles, List<Job> parentJobIDs) {
        super(jobName, jobID, parameters, parentJobIDs);
        this.toolID = toolID;
        //this.toolMD5 = toolMD5;
        this.loadedFiles = loadedFiles;
    }
}
